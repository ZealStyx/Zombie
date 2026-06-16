# Implement Missing Systems, Bug Fixes & Code Review

## Background

LibGDX + Artemis-odb ECS zombie survival game. All 7 original systems exist and work.
This plan covers the **approved 6 bug fixes** plus **4 new systems**: Zed AI, Health/Damage, Combat, and Debug Console.

> [!IMPORTANT]
> **Android support** is planned. All input handling uses abstractions that can be mapped to touch later. No hard keyboard dependencies in new systems — they read from an input abstraction.
>
> **Future shaders**: No HUD for now. Lighting, shadows, screen effects, and projectile VFX will use OpenGL shaders in a future phase. The architecture avoids anything that would conflict with a shader pipeline later.

---

## Phase 1 — Bug Fixes (from approved review)

### 1.1 MainMenuScreen — Screen Resource Leak

#### [MODIFY] [MainMenuScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/MainMenuScreen.java)

- Dispose Stage/Skin in `hide()` since `Game.setScreen()` calls `hide()` but not `dispose()`.

---

### 1.2 CollisionComponent — Scale Proportions by Entity Size

#### [MODIFY] [CollisionComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/CollisionComponent.java)

- Scale feet/body/head Y-offsets and heights by entity `h`, so entities of different sizes get correct boxes.

---

### 1.3 TextureCache — Cache TextureRegions

#### [MODIFY] [TextureCache.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/TextureCache.java)

- Add `regionCache` map to avoid thousands of per-frame allocations.

---

### 1.4 AnimationStateSystem — Use `minDuration` Field

#### [MODIFY] [AnimationStateSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/AnimationStateSystem.java)

- Check `anim.minDuration` before falling back to `DEFAULT_ONESHOT_DURATION`.

---

### 1.5 AnimationStateComponent — Add `playOnce(pose, duration)` Overload

#### [MODIFY] [AnimationStateComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/AnimationStateComponent.java)

- New method `playOnce(String pose, float duration)` that sets `minDuration`.

---

### 1.6 ZedConfig — Fix Static Method on Instance

#### [MODIFY] [ZedConfig.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ZedConfig.java)

- Remove unnecessary `MathUtils` parameter, call static method directly.

---

## Phase 2 — Health & Damage System

### 2.1 HealthComponent

#### [NEW] [HealthComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/HealthComponent.java)

```java
public class HealthComponent extends Component {
    public float hp       = 100f;
    public float maxHp    = 100f;
    public boolean dead   = false;

    // Invulnerability frames after taking damage (prevents stun-lock)
    public float iFrameTimer    = 0f;
    public float iFrameDuration = 0.3f;

    // Pending damage queue (applied by HealthSystem)
    // Each entry: [damage, isHeadshot(0/1)]
    public final Array<float[]> pendingDamage = new Array<>(false, 4);

    public void queueDamage(float amount, boolean headshot) {
        pendingDamage.add(new float[]{ amount, headshot ? 1f : 0f });
    }

    public boolean isInvulnerable() {
        return iFrameTimer > 0f;
    }
}
```

Key design:
- **Pending damage queue** — systems write damage into the queue; `HealthSystem` processes it once per frame. This decouples damage sources from health processing.
- **I-frames** — prevent multiple damage ticks in the same instant.
- Used by both players and zeds.

---

### 2.2 HealthSystem

#### [NEW] [HealthSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/HealthSystem.java)

Processes all entities with `HealthComponent`:

```
Each frame:
  1. Tick down iFrameTimer
  2. Drain pendingDamage queue:
     - Skip if invulnerable
     - Apply headshot multiplier (2×)
     - Subtract from hp
     - Start i-frame timer
  3. If hp <= 0 and !dead:
     - Set dead = true
     - If entity has ZedComponent: trigger death animation
     - If entity has PlayerComponent: trigger death animation
```

Headshot multiplier: `2.0×` when `isHeadshot == true`.

---

### 2.3 EntityFactory Updates

#### [MODIFY] [EntityFactory.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/EntityFactory.java)

- Add `HealthComponent` to player (hp=100) and zed entities.
- Zed HP varies by type: normal=60, fast=40, army=80, tank=200, etc.

---

## Phase 3 — Zed AI System

### Why Not A*?

For a large procedurally generated world, A* has significant drawbacks:
- Requires a navigation grid/mesh — expensive to build and rebuild as terrain changes
- Memory scales with map area (O(width × height))
- Pathfinding cost spikes with many zeds pathing simultaneously

### Recommended: Steering Behaviors + Line-of-Sight

Simple, scalable, and realistic for zombies:

| Behavior | Description |
|---|---|
| **Detection** | Circle range check — if player within `detectionRange`, switch to CHASE |
| **Chase** | Steer toward player position. Normalize direction, move at zed speed. |
| **Attack** | When within `attackRange`, stop, play attack anim, deal damage to player body/head box |
| **Wander** | Random direction changes on a timer when no target detected |
| **Obstacle avoidance** | Sample 2-3 points ahead of movement direction, deflect away from world obstacles |
| **De-aggro** | If player moves beyond `deaggroRange`, return to WANDER |

This uses **zero memory per map tile**, scales to thousands of zeds, and works on any map shape.

### 3.1 ZedAIComponent

#### [NEW] [ZedAIComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/ZedAIComponent.java)

```java
public class ZedAIComponent extends Component {
    public enum State { IDLE, WANDER, CHASE, ATTACK }

    public State   state           = State.WANDER;
    public float   speed           = 1.5f;    // world units/sec

    // Detection
    public float   detectionRange  = 8f;      // aggro radius
    public float   deaggroRange    = 14f;     // lose interest radius
    public float   attackRange     = 0.8f;    // melee strike distance

    // Attack
    public float   attackDamage    = 15f;
    public float   attackCooldown  = 1.2f;    // seconds between attacks
    public float   attackTimer     = 0f;

    // Wander
    public float   wanderDirX     = 0f;
    public float   wanderDirY     = -1f;
    public float   wanderTimer    = 0f;
    public float   wanderInterval = 3f;       // change direction every N seconds
}
```

Per-type defaults set in EntityFactory:

| Type | Speed | Detection | Attack Dmg | Attack CD | HP |
|---|---|---|---|---|---|
| normal | 1.5 | 8 | 15 | 1.2 | 60 |
| fast | 3.0 | 10 | 10 | 0.8 | 40 |
| army | 2.0 | 10 | 20 | 1.0 | 80 |
| tank | 0.8 | 6 | 35 | 2.0 | 200 |
| screamer | 1.0 | 12 | 5 | 1.5 | 50 |
| shooter | 1.2 | 14 | 12 | 2.0 | 60 |
| jumper | 2.5 | 8 | 20 | 1.5 | 50 |
| buried | 0.0 | 4 | 15 | 1.2 | 60 |

---

### 3.2 ZedAISystem

#### [NEW] [ZedAISystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/ZedAISystem.java)

Iterates all entities with `ZedComponent + ZedAIComponent + TransformComponent + AnimationStateComponent + HealthComponent`:

```
State machine per zed:

WANDER:
  - Move in wanderDir at reduced speed (speed × 0.4)
  - Every wanderInterval seconds, pick a new random direction
  - Set pose to "walk" (or "idle" if paused)
  - Check: distance to player < detectionRange → CHASE
  - Obstacle avoidance: if next step overlaps a WorldCollision rect, pick perpendicular direction

CHASE:
  - Calculate direction to player center
  - Move toward player at full speed
  - Set facing direction based on movement
  - Set pose to "run"
  - Obstacle avoidance: deflect off world obstacles
  - Check: distance to player < attackRange → ATTACK
  - Check: distance to player > deaggroRange → WANDER

ATTACK:
  - Stop moving
  - Play "attack" animation (one-shot)
  - On cooldown expiry:
    - Check if zed's feetBox center is within attackRange of player
    - If yes: queue damage to player's HealthComponent (body hit)
    - Reset cooldown timer
  - Check: distance > attackRange → CHASE
  - Check: distance > deaggroRange → WANDER

IDLE:
  - Used for buried zeds before emergence
  - No movement, just wait for trigger
```

**Obstacle avoidance** (simple, no grid):
```
Given movement direction (dx, dy) and step size:
  1. Compute candidate position = current + (dx, dy) * speed * delta
  2. Build a test rect at candidate position (using feet box proportions)
  3. If test rect overlaps any WorldCollision obstacle:
     a. Try (perpendicular direction): rotate 90° clockwise
     b. If that also overlaps: try 90° counter-clockwise
     c. If both blocked: stop (don't move this frame)
```

**Player finding**: Uses `EntitySubscription` to find the player entity (same pattern as `ItemPickupSystem`).

---

### 3.3 System Execution Order Update

#### [MODIFY] [GameScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/GameScreen.java)

Updated system order:
```
1. AnimationStateSystem   — advance timers
2. MovementSystem         — player WASD input
3. ZedAISystem            — zed steering + state machine
4. CollisionSystem        — resolve feet vs world for ALL entities
5. CombatSystem           — player attacks → damage to zeds
6. HealthSystem           — process damage queues, trigger death
7. ItemPickupSystem       — F key pickup
8. DebugConsoleSystem     — process debug commands
9. WorldItemRenderSystem  — draw items
10. ZedRenderSystem       — draw zeds
11. PlayerRenderSystem    — draw player on top
```

---

## Phase 4 — Combat System

### 4.1 CombatComponent

#### [NEW] [CombatComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/CombatComponent.java)

```java
public class CombatComponent extends Component {
    // Current weapon stats (set from equipped item)
    public float meleeDamage    = 20f;
    public float meleeRange     = 1.2f;    // world units in front of player
    public float meleeCooldown  = 0.5f;
    public float meleeTimer     = 0f;

    // Ranged
    public boolean hasRanged    = false;
    public float rangedDamage   = 0f;
    public float rangedRange    = 15f;     // hitscan max distance
    public float rangedCooldown = 0.3f;
    public float rangedTimer    = 0f;

    // State
    public boolean meleeRequested  = false;
    public boolean rangedRequested = false;
}
```

---

### 4.2 CombatSystem

#### [NEW] [CombatSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/CombatSystem.java)

Processes player entities with `CombatComponent + TransformComponent + CollisionComponent + AnimationStateComponent`:

**Melee attack** (mouse left click / touch):
```
1. Check meleeRequested flag and cooldown
2. Play attack animation ("axe" for melee weapons)
3. Create attack hitbox:
   - Rectangle in front of player, based on facing direction
   - Size: meleeRange × 0.8 (width) positioned at player center + direction offset
4. Check overlap against all zed bodyBox and headBox:
   - headBox hit → queue damage with headshot=true
   - bodyBox hit → queue damage with headshot=false
   - If both overlap, prefer headBox (headshot)
5. Reset cooldown timer
```

**Ranged attack** (mouse right click / touch):
```
1. Check rangedRequested flag, cooldown, and hasRanged
2. Play shooting animation ("pistol" or "twohand" based on weapon type)
3. Hitscan ray from player center in facing direction:
   - Step along ray in small increments (0.2 world units)
   - Check each step against zed headBox first, then bodyBox
   - First hit: queue damage to that zed
   - Stop at rangedRange or world obstacle
4. Reset cooldown timer
```

> [!NOTE]
> **Android input**: Melee/ranged will later be mapped to on-screen buttons. For now, the system reads `meleeRequested` / `rangedRequested` flags that `MovementSystem` sets from mouse clicks. This is easy to swap to touch buttons later.

**Input handling** (added to `MovementSystem`):
```java
// Mouse/touch combat input
if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
    combat.meleeRequested = true;
}
if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && combat.hasRanged) {
    combat.rangedRequested = true;
}
```

---

## Phase 5 — Debug Console

### 5.1 DebugConsoleSystem

#### [NEW] [DebugConsoleSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/DebugConsoleSystem.java)

A system that listens for a toggle key (backtick `` ` `` or `F3`) and renders a Scene2D `Dialog` with a `TextField` for command input.

**Architecture**:
- The system holds a `Stage` with a `Dialog` (using the existing `ui/uiskin.json` skin)
- When active, it captures input via `InputMultiplexer`
- Commands are parsed and executed immediately
- Output is logged to a scrollable label in the dialog

**Supported commands**:

| Command | Description | Example |
|---|---|---|
| `equip <slot> <itemId>` | Equip item to player slot | `equip held 175` (FNX 45) |
| `unequip <slot>` | Remove item from slot | `unequip held` |
| `give <itemId> [qty]` | Add item to inventory | `give 1 5` (5× Canned Beans) |
| `spawn zed <type> [x] [y]` | Spawn a zed at position | `spawn zed fast 15 10` |
| `spawn item <id> [x] [y]` | Drop item in world | `spawn item 192 10 10` |
| `heal [amount]` | Restore player HP | `heal` or `heal 50` |
| `god` | Toggle invulnerability | `god` |
| `kill_all` | Kill all zeds | `kill_all` |
| `tp <x> <y>` | Teleport player | `tp 20 12` |
| `hp` | Print current HP | `hp` |
| `inv` | Print inventory contents | `inv` |
| `clear` | Clear console output | `clear` |
| `help` | List all commands | `help` |

**Slots for equip**: `held`, `holstered`, `vest`, `helmet`, `pants`, `top`, `backpack`, `footwear`

---

### 5.2 GameScreen InputMultiplexer

#### [MODIFY] [GameScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/GameScreen.java)

- Set `InputMultiplexer` as input processor so debug console Stage and game input coexist.

---

## New File Summary

| File | Package | Purpose |
|---|---|---|
| [HealthComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/HealthComponent.java) | `component` | HP, death flag, damage queue, i-frames |
| [ZedAIComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/ZedAIComponent.java) | `component` | AI state machine, speeds, ranges |
| [CombatComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/CombatComponent.java) | `component` | Weapon stats, attack requests |
| [ZedAISystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/ZedAISystem.java) | `system` | Steering behaviors, state machine |
| [HealthSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/HealthSystem.java) | `system` | Damage processing, death triggers |
| [CombatSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/CombatSystem.java) | `system` | Melee hitbox, hitscan ranged |
| [DebugConsoleSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/DebugConsoleSystem.java) | `system` | In-game command console |

## Modified File Summary

| File | Changes |
|---|---|
| [MainMenuScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/MainMenuScreen.java) | Dispose on hide |
| [CollisionComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/CollisionComponent.java) | Scale by entity h |
| [TextureCache.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/TextureCache.java) | Region cache |
| [AnimationStateSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/AnimationStateSystem.java) | Use minDuration |
| [AnimationStateComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/AnimationStateComponent.java) | playOnce overload |
| [ZedConfig.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ZedConfig.java) | Fix static method |
| [EntityFactory.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/EntityFactory.java) | Add Health, ZedAI, Combat components |
| [GameScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/GameScreen.java) | New system order, InputMultiplexer |
| [MovementSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/MovementSystem.java) | Combat input flags |

---

## Verification Plan

### Automated Tests
- `gradlew.bat lwjgl3:classes` — compile check

### Manual Verification
- Main menu → Play → no resource leak
- Walk near items, press F → pickup works
- Zeds wander, detect player, chase, attack
- Player melee (left click) kills zeds, headshots do 2× damage
- Debug console (F3): `equip held 175`, `spawn zed fast`, `heal`, `god`
- Death: player/zed death animations trigger correctly
