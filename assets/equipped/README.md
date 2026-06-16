# equipped/ — worn/held appearance per item

131 of the 331 items are "equippable" (visible on the player's character when held or worn) — every weapon (primary/secondary/melee) and every clothing item that resolved to a real game object in `on_ground_mapping.md` (i.e. everything that wasn't a `FALLBACK`). The 200 consumables/ammo/materials/etc. have no character-overlay graphic in the source, so they're not included here.

Each item gets a folder named `<new_id>/` (matching `item/` and `on_ground/`), containing one subfolder per pose the object renders in:

- `idle/`, `run/` — the four-direction standing/moving poses, always present
- `axe/`, `pistol/`, `twohand/` — how this item looks while the player is actively using a melee weapon / sidearm / two-handed weapon (present when the source defines that pose)
- `using/` — the "consuming/using" animation frames
- `h_axe/`, `h_run/`, `h_pistol/`, `h_twohand/` — the holstered/secondary-slot variants of the above poses, for items that can be carried passively while something else is equipped

Frame filenames keep their original animation name + frame index (e.g. `idle_down_0.png`, `h_run_left_2.png`), so direction and frame order are preserved. `on_ground`/`Default` frames are intentionally excluded here since those are already covered by the `on_ground/` and `item/` folders.

Coverage isn't perfectly uniform — e.g. id 175 (FNX 45) only has `idle/run/pistol/h_axe/h_run` (no `axe`/`twohand`/`using`, since the source object for this weapon doesn't define those poses), while id 274 (Gorka Helmet) has `idle/run/axe/pistol/using` but no `twohand`. This reflects what actually exists in the game data per item — nothing was padded or invented.

131 folders, 6,085 files total.
