# Master Implementation Plan (Zombie Game)

This plan integrates and resolves the remaining tasks from the networking, touch controls, weapons, and inventory plans, while addressing specific bugs identified in player animations, item rotation, item placement guides, and firing mechanics. Finally, it schedules the Procedural Map Generation component as the last to-do.

---

## User Review Required

> [!IMPORTANT]
> - **Item Instance Persistence:** We are modifying `PlayerComponent` to keep `ItemInstance` references for `held` and `holstered` slots (in addition to containers). This ensures that custom weapon state (e.g. loaded ammo count `currentAmmo`) is preserved when weapons are equipped, holstered, or unequipped.
> - **Custom Drawing for Inventory Items:** To fix the long-standing bug where item rotation squishes texture aspect ratios in tables, we are introducing a custom Scene2D `ItemIcon` actor. It overrides the `draw` method to compute correct rotation/scaling bounds internally, resolving the libGDX table layout limitations with child rotation.
> - **Map Generation Schedule:** As requested, the procedural map generation system is scheduled as the final task in the implementation queue.

---

## Proposed Changes

### Component 1: Weapons & Combat

#### [MODIFY] [PlayerComponent.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/component/PlayerComponent.java)
- Add `public ItemInstance equippedHeld;` and `public ItemInstance equippedHolstered;` fields to reference the weapon item instances.
- Modify `equip(String slot, ItemInstance instance)` and `unequip(String slot)` to update these references when changing the `"held"` or `"holstered"` slots, preserving the instance stats (such as loaded ammo).

#### [MODIFY] [CombatSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/CombatSystem.java)
- Check `player.dirty` inside `process(entityId)` to call `syncHeldWeapon(player, combat)`. Keep `player.dirty` true so `GameRenderSystem` still processes it.
- Implement `syncHeldWeapon(player, combat)` to:
  - If a gun is equipped: set `combat.hasRanged = true`, read and copy stats (`damage`, `range`, `clipSize`, `fireCooldown`, `reloadTime`, `ammoItemId`, `fireMode`), and load `combat.currentAmmo` from `player.equippedHeld.currentAmmo`.
  - If a melee weapon is equipped: set `combat.hasRanged = false`, and sync melee stats (`damage`, `range`, `cooldown`).
  - If unarmed: set defaults for fists.
- At the end of `process()`, write `combat.currentAmmo` back to `player.equippedHeld.currentAmmo` (if present) to keep the ammo count persistent.
- In `doRanged()`, resolve the correct one-shot weapon pose animation (`"twohand"` for primary, `"pistol"` for secondary/melee) instead of hardcoding `"pistol"`.

---

### Component 2: Animations & Sprite Rendering

#### [MODIFY] [MovementSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/MovementSystem.java)
- In `process()`, when the player is idle (not running) and not locked in an animation:
  - Determine the correct stance pose (`"twohand"`, `"pistol"`, or `"axe"`) by looking up the type of weapon equipped in `PlayerComponent.heldItemId` using the item database.
  - Set the player's pose to this weapon-specific stance instead of always reverting to `"idle"`. This fixes the bug where hands appear at the side while holding a weapon.

#### [MODIFY] [PlayerRenderer.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/PlayerRenderer.java)
- In `draw()`, when calling `drawLayer` for `holsteredAnims`:
  - Pass `0f` stateTime if the player's current pose is not `"run"` (i.e. if they are in `"idle"`, `"axe"`, `"pistol"`, or `"twohand"` stances).
  - This keeps the holstered weapon sprite static at frame 0 (sitting passively on the back/hip) and prevents it from playing the bobbing `"h_run"` animation when the player stands still.

---

### Component 3: Inventory UI

#### [MODIFY] [InventoryUiSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/system/InventoryUiSystem.java)
- Add a custom `ItemIcon` actor as a static nested class. It will override `draw(Batch batch, float parentAlpha)`:
  - When `rotated == false`: Draw the texture region centered within the actor's bounds, scaled using fit logic.
  - When `rotated == true`: Swap the source dimensions (`srcW = region.getRegionHeight()`, `srcH = region.getRegionWidth()`), calculate the scaling factor, and draw the region rotated 90 degrees around its center (`batch.draw(region, px, py, originX, originY, unrotatedW, unrotatedH, 1f, 1f, 90f)`) so it is perfectly centered and fits the grid footprint.
- In `createItemWidget` and `createGearSlot`, replace `Image` with the new `ItemIcon` actor, eliminating scaling/rotation issues inside Scene2D tables.
- In `createGridTable`:
  - Store slot Table actors in a 2D array: `Table[][] cellSlots`.
  - In `DragAndDrop.Target.drag`: Calculate the item footprint columns (`iw`) and rows (`ih`). Loop through all cells in the footprint starting from the hovered `(cr, cc)` and set their background color to green (if fits) or red (if doesn't fit).
  - In `DragAndDrop.Target.reset`: Reset all cells in the grid back to their default color and alpha.

---

### Component 4: JSON Config Split

#### [NEW] [ItemDataDef.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ItemDataDef.java)
- Mirror weapon stats (`damage`, `range`, `clipSize`, etc.) and container stats (`containerRows`, `containerCols`), as well as gameplay attributes for food, medical, and ammo types.

#### [NEW] [ItemDataConfig.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ItemDataConfig.java)
- Parse and index `assets/config/item_data.json` by item ID.

#### [MODIFY] [ItemDef.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ItemDef.java)
- Remove weapon, container, and grid footprint details. It remains a pure description of identity, types, and texture paths.

#### [MODIFY] [ItemGridDef.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ItemGridDef.java)
- Keep only `id`, `gridW`, `gridH` to serve the layout size.

#### [MODIFY] [ConfigLoader.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/config/ConfigLoader.java)
- Load `config/item_textures.json` (renamed from `items.json`) -> `ItemDatabase`.
- Load `config/item_data.json` [NEW] -> `ItemDataConfig`.
- Expose getter for `ItemDataConfig`.
- Repoint weapon/container stats lookups in `InventoryUiSystem`, `CombatSystem`, and `ItemInstance` to query `ConfigLoader.getItemDataConfig()`.

---

### Component 5: Procedural Map Generation

> [!NOTE]
> This component is scheduled as the final task in the implementation plan.

#### [NEW] [OpenSimplex2.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/world/noise/OpenSimplex2.java)
- Add 2D OpenSimplex2 noise implementation for procedural landmass generation.

#### [NEW] [TileType.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/world/TileType.java)
- Add `GRASS` / `DIRT` enum.

#### [NEW] [TileDesc.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/world/TileDesc.java)
- Struct containing sheet index, sheet column, and sheet row.

#### [NEW] [TileAutotiler.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/world/TileAutotiler.java)
- 4-bit cardinal autotile bitmask resolver with 8-bit diagonal concave corner fallback logic.

#### [NEW] [ProceduralMapGenerator.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/world/ProceduralMapGenerator.java)
- Pipeline: Noise grid generation -> cellular automaton smoothing (2 passes) -> autotile mapping -> 2D `TileDesc` grid array.

#### [NEW] [TileShader.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/TileShader.java)
- Shader loader, setting custom tint color uniforms (`u_grassColor`, `u_dirtColor`) and textures.

#### [NEW] [MapRenderSystem.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/MapRenderSystem.java)
- Artemis `BaseSystem` registered before `GameRenderSystem`. Performs camera frustum culling and groups SpriteBatch draw calls by sheet texture.

#### [MODIFY] [TextureCache.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/rendering/TextureCache.java)
- Expose `storeRegionByKey()` and `cachedRegionByKey()` to store custom sub-textures.

#### [MODIFY] [GameScreen.java](file:///d:/LibGDX/Projects/Zombie/core/src/main/java/io/github/zom/GameScreen.java)
- Set map dimensions (e.g. 64x64 tiles), run `ProceduralMapGenerator`, instantiate and register `MapRenderSystem` in the ECS world, and expand boundary collision bounds.

---

## Verification Plan

### Automated Tests
- `gradlew.bat lwjgl3:classes` to verify that all code compiles correctly.

### Manual Verification
- **Firing & Ammo Persistence:**
  - Drag an FNX 45 pistol from inventory into the `held` slot. Check that the player's stance changes to holding the weapon.
  - Fire the pistol (right-click). Verify that ammo is decremented from the gun.
  - Holster the pistol (drag to holstered slot). Verify that the gun remains visual, static on the hip/back, and does not play a running bobbing animation while the player is idle.
  - Drag the pistol back to inventory. Verify that the current ammo matches the loaded count.
- **Visual Rotation & Placement Guides:**
  - Drag a vertical weapon (e.g., 1x4 rifle) inside the inventory.
  - Hover over cells. Check that the highlighted cells show a 1x4 green footprint (if fits) or red footprint (if overlaps/out-of-bounds).
  - Press `R` to rotate. Check that the highlighted footprint is now 4x1.
  - Drop the weapon. Verify that the weapon's texture region rotates 90 degrees and fits inside the 4x1 bounds without aspect ratio squishing or letterboxing.
- **Config JSON Split:**
  - Run the game and verify that textures, layout footprint, and stats load correctly from the three config files without startup errors.
