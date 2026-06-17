# Multiplayer Server, Android Touch Controls, Weapons, and Grid Inventory Plan

This implementation plan outlines the design and integration of multiplayer networking, Android touch controls, gun/weapons mechanics, and a multi-dimensional grid-based inventory system.

---

## Resolved Decisions

- **Backpack/Sling Bag**: These are **containers** with their own separate inventory sub-grids below the player's base grid, separated by visual gaps. Items persist inside them when dropped on the ground or traded.
- **Base Inventory**: Player starts with a **6×4** grid (was 6×3).
- **Backpack grid**: min 6×4, max 6×10 rows.
- **Sling bag grid**: min 3×1, max 6×1 rows.
- **Gun Fire Mode**: Both automatic (hold) and semi-automatic (tap) supported per weapon via a config flag.
- **Android Inventory Button**: Added to the touch control overlay.

---

## Proposed Changes

### 1. Networking Component (Multiplayer Client/Server)

#### [NEW] [Protocol.java](file:///d:/LibGDX/Projects/Zombie/shared/src/main/java/io/github/zom/net/Protocol.java)
- Define network packets as serializable JSON objects over TCP.
- Packet types: `JoinRequest`, `JoinResponse`, `PlayerInput`, `WorldStateUpdate`, `InventorySync`.

#### [NEW] [GameServer.java](file:///d:/LibGDX/Projects/Zombie/server/src/main/java/io/github/zom/net/GameServer.java)
- Runs a server socket on port `7777`.
- Enforces player count limit (configurable, e.g. max 8). Rejects with `JoinResponse(status=FULL)`.
- Maintains headless Artemis ECS world, broadcasts entity state to all clients.

#### [NEW] [GameClient.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/net/GameClient.java)
- Manages connection to server, sends player inputs, receives/applies world state updates.

#### [MODIFY] [ServerLauncher.java](file:///d:/LibGDX/Projects/Zombie/server/src/main/java/io/github/zom/server/ServerLauncher.java)
- Initialize and start `GameServer` on launch.

---

### 2. Android Touch UI Component

#### [NEW] [AndroidControllerSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/AndroidControllerSystem.java)
- Runs only if `Gdx.app.getType() == ApplicationType.Android`.
- Uses `ShapeRenderer` to draw:
  - **Virtual Joystick** (Bottom Left): Outer bounds circle + inner thumb circle. Drag to set move direction.
  - **Attack Button** (Bottom Right): Circle labeled "ATT" to trigger equipped weapon.
  - **Interact Button** (Bottom Right, smaller): Label "F" to pick up items.
  - **Inventory Button** (Top Right): Label "INV" to toggle inventory screen.
- Translates touch coordinates into `MovementSystem` direction and `CombatComponent` request flags.

---

### 3. Main Menu Component

#### [MODIFY] [MainMenuScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/MainMenuScreen.java)
- Add "Join Server" button.
- Clicking opens a Scene2D dialog with a TextField for IP:port (e.g. `127.0.0.1:7777`).
- "Connect" starts `GameScreen` in client network mode. "Cancel" closes the dialog.

---

### 4. Gun & Weapons Mechanics Component

#### [MODIFY] [ItemDef.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ItemDef.java)
- Add weapon stat fields:
  - `damage`: float
  - `range`: float
  - `clipSize`: int
  - `fireCooldown`: float (seconds between shots)
  - `reloadTime`: float
  - `ammoItemId`: int (matching ammo item id)
  - `fireMode`: String (`"semi"`, `"auto"`, `"both"`) — determines fire behavior

#### [MODIFY] [CombatComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/CombatComponent.java)
- Track active weapon state:
  - `currentAmmo`: rounds loaded in clip
  - `reloading`: boolean flag
  - `reloadTimer`: remaining reload duration
  - `isAutoFiring`: boolean (for automatic fire mode — true while fire button held)

#### [MODIFY] [CombatSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/CombatSystem.java)
- Ranged weapon processing:
  - Semi-auto: fire one shot per button press.
  - Auto: fire continuously while button held, respecting `fireCooldown`.
  - `currentAmmo > 0` → deduct 1, play firing anim, hitscan raycast.
  - Reload on `R` key or when clip is empty:
    - Search grid inventory for matching `ammoItemId`.
    - Consume ammo up to `clipSize`.
    - Lock weapon, set `reloading = true` and `reloadTimer`.

---

### 5. Grid-Based Inventory Component

The inventory is split into **three separate sections**, each its own grid, visually stacked vertically with gap separators:

```
┌─────────────────────────┐
│   Player Base Inventory │  6 cols × 4 rows (always present)
│   ┌─┬─┬─┬─┬─┬─┐        │
│   │ │ │ │ │ │ │        │
│   ├─┼─┼─┼─┼─┼─┤        │
│   │ │ │ │ │ │ │        │
│   ├─┼─┼─┼─┼─┼─┤        │
│   │ │ │ │ │ │ │        │
│   ├─┼─┼─┼─┼─┼─┤        │
│   │ │ │ │ │ │ │        │
│   └─┴─┴─┴─┴─┴─┘        │
│  ─ ─ ─ GAP ─ ─ ─ ─ ─  │
│   Backpack Grid         │  6 cols × 4–10 rows (if equipped)
│   ┌─┬─┬─┬─┬─┬─┐        │
│   │ │ │ │ │ │ │        │
│   │ ...              │  │
│   └─┴─┴─┴─┴─┴─┘        │
│  ─ ─ ─ GAP ─ ─ ─ ─ ─  │
│   Sling Bag Grid        │  3–6 cols × 1 row (if equipped)
│   ┌─┬─┬─┐              │
│   │ │ │ │              │
│   └─┴─┴─┘              │
└─────────────────────────┘
```

#### [NEW] [ItemInstance.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/ItemInstance.java)
- Wraps an item with instance-specific data:
  - `itemId`: reference ID
  - `quantity`: stack count
  - `currentAmmo`: loaded ammunition (for guns)
  - `uuid`: unique instance identifier (for drag-drop identity)

#### [NEW] [ItemPlacement.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/ItemPlacement.java)
- Represents an item placed inside the inventory grid:
  - `instance`: reference to `ItemInstance`
  - `r`: grid row index
  - `c`: grid column index

#### [NEW] [ContainerInventory.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/ContainerInventory.java)
- A sub-grid inventory that belongs to a backpack or sling bag item.
- Fields: `rows`, `cols`, `List<ItemPlacement> placements`, `boolean[][] occupied`.
- Same API as the main inventory grid: `canFit()`, `add()`, `remove()`.
- When a backpack is dropped, the `ContainerInventory` stays attached to the `ItemInstance` — items persist.

#### [MODIFY] [Inventory.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/util/Inventory.java)
- Replace 1D slot array with a collection of `ItemPlacement` objects.
- Maintain a `boolean[rows][cols]` occupancy grid.
- Key grid operations:
  - `canFit(ItemDef, r, c)`: checks if item dimensions fit without overlap.
  - `add(ItemInstance)`: searches the 2D grid row-by-row for the first free rectangle. If stackable and size 1×1, merges into existing stacks first.
  - `remove(ItemInstance)`: clears occupied coordinates.
- The player's base grid is fixed at 6×4.

#### [MODIFY] [PlayerComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/PlayerComponent.java)
- Add `slingBagId` equipment slot.
- References to the backpack's `ContainerInventory` and sling bag's `ContainerInventory` are accessed through their `ItemInstance`.

#### [NEW] [InventoryUiSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/InventoryUiSystem.java)
- Render player inventory using Scene2D:
  - **Bottom-right**: Vertical stack of the three grid sections (base → backpack → sling bag) with gap separators.
  - **Bottom-left** (adjacent to grid): Equippable gear slots (Helmet, Chest, Vest, Backpack, Sling Bag, Pants, Footwear, Held, Holstered).
  - The whole inventory panel should NOT fill the entire screen height — snapped to the bottom.
  - `DragAndDrop` handler: drag items between cells within a section and across sections.
- Opens on `Tab` key (desktop) or "INV" button (Android).

---

## Verification Plan

### Automated Tests
- `gradlew.bat lwjgl3:classes` — compile check.

### Manual Verification
- **Multiplayer**: Start server, set `maxPlayers = 2`. Join from 2 clients. Verify 3rd is rejected. Verify movement replication.
- **Android Controls**: Run on emulator. Test joystick, attack, interact, and inventory buttons.
- **Weapon Mechanics**: Equip pistol (semi) and assault rifle (auto). Verify semi fires once per tap, auto fires while held. Verify ammo deduction, reload from grid inventory.
- **Grid Inventory**: Place a rifle (multi-cell) and canned beans (1×1). Verify correct grid space. Equip a backpack → backpack sub-grid appears. Place items in backpack, drop backpack on ground, pick it back up → items still inside. Test sling bag similarly.
