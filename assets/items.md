# Mini DayZ+ 1.8.5 — Reorganized Item Database

Total items: 337 (+ 6 empty/reserved slots documented at the end, excluded from numbering)

This reassigns the game's scattered/sparse original item IDs (1-148, 211-277, 301-569, 700-746) into a single 
sequential ID space (1..337), grouped by item type. **`old_id`** is the ID used in the game's own 
`l_eng_items.xml` / `l_eng_new.xml` localization files (useful for cross-referencing back to the source). 
Sprite files in `item/` (inventory icon) and `on_ground/` (world/dropped sprite) are both named `<new_id>.png`.

## Type overview

| Type | Count | New ID range |
|---|---|---|
| Food & Drink | 39 | 1-39 |
| Medical | 13 | 40-52 |
| Ammunition | 17 | 53-69 |
| Explosives & Throwables | 6 | 70-75 |
| Attachments (scopes, grips, suppressors, mags, launchers) | 29 | 76-104 |
| Crafting Materials | 21 | 105-125 |
| Tools & Misc Usables | 22 | 126-147 |
| Footwear | 4 | 148-151 |
| Accessories | 1 | 152-152 |
| Melee Weapons | 22 | 153-174 |
| Secondary Firearms (pistols) | 17 | 175-191 |
| Primary Firearms | 42 | 192-233 |
| Vests / Body Armor | 22 | 234-255 |
| Helmets / Headwear | 34 | 256-289 |
| Pants | 7 | 290-296 |
| Top-Wear (jackets/shirts) | 18 | 297-314 |
| Backpacks | 12 | 315-326 |
| Structures / Stashes / Misc Deployables | 5 | 327-331 |
| Unidentified Items | 6 | 332-337 |

---

## Food & Drink

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 1 | 1 | Canned Beans | A nutritious and long-lasted can of baked beans that will last two to five years depending on storage conditions. |  |
| 2 | 2 | Canned Tuna | A can of tuna. |  |
| 3 | 3 | Tactical Bacon | A can of bacon. |  |
| 4 | 4 | Rice | A bag of long-grained rice, usually consumed when cooked with water. |  |
| 5 | 5 | Tomato | A tomato. |  |
| 6 | 6 | Apple | An ordinary ripe apple. |  |
| 7 | 7 | Banana | A fresh banana, rich in pottasium. |  |
| 8 | 8 | Pipsi | A comparatively unhealthy, (cola flavoured) beverage, containing carbonated water, sweeteners, flavouring agents, colouring agents, and p... |  |
| 9 | 9 | Spite | A comparatively unhealthy, (lemon-lime flavoured) beverage, containing carbonated water, sweeteners, flavouring agents, colouring agents ... |  |
| 10 | 10 | Kvas | A healthy, (fermented) beverage made from rye bread, tasty and delicious. |  |
| 11 | 25 | Whiskey | "I've drunk whiskey... |  |
| 12 | 27 | Cranberries | Cranberries can be collected from the berry bushes found throughout the forests, they can be spotted and distinguished easily by their re... |  |
| 13 | 30 | Nota-Cola | A comparatively unhealthy, (cola flavoured) beverage, containing carbonated water, sweeteners, flavouring agents, colouring agents and pr... |  |
| 14 | 31 | Zucchini | This zucchini is just ripe enough to be eaten raw. |  |
| 15 | 32 | Bell Pepper | Bell pepper, the green variety. |  |
| 16 | 33 | Orange | A popular citrus fruit, packed with vitamin "C". |  |
| 17 | 60 | Raw Venison Steak | A raw chunk of meat from a deer. |  |
| 18 | 61 | Roasted Venison Steak | Raw venison steak roasted over a campfire to succulent perfection. |  |
| 19 | 62 | Nuka-Cola | A bottle of Nuka-Cola, the flavored softdrink of the post-nuclear world. |  |
| 20 | 63 | Energy Drink | A straightforward option for a bolt of athletic energy, it combines caffeine, vitamins, and a familiar mountain dew taste. |  |
| 21 | 71 | Raw Rabbit | The posterior leg of a rabbit. |  |
| 22 | 72 | Roasted Rabbit | Rabbit leg roasted over a campfire, tender and flavourful. |  |
| 23 | 73 | Cloudberries | Cloud berries can only be found around lakes. |  |
| 24 | 74 | Bilberries | Bilberries can be collected from the berry bushes found throughout the forests. |  |
| 25 | 85 | MRE | Meal Ready-to-Eat (MRE), a self-contained, individual field ration intended for use by the military. |  |
| 26 | 91 | Herring | A small silvery forage fish known for its streamlined bodies and iridescent scales. |  |
| 27 | 92 | Salmon | A diverse group of fish known for its migratory patterns, moving between freshwater and saltwater habitats. |  |
| 28 | 93 | Ruffe | A small freshwater fish, typically around 4-6 inches long, with a slender, elongated body. |  |
| 29 | 94 | Perch | Identified by their two dorsal fins, one spiny and one soft, and are typically carnivorous bottom-dwellers. |  |
| 30 | 95 | Small Cooked Fillet | Small fish filet roasted over a campfire for a tasty, decent sized snack. |  |
| 31 | 96 | Large Cooked Fillet | Prepared fish filet seered over the campfire. |  |
| 32 | 114 | Elderberries | Small, dark purple colored black berries, known for their fragrant creamy flavour and rich nutrients. |  |
| 33 | 121 | Beer | Beer sealed in a can, usually lasts for 6-9 months at room temperature. |  |
| 34 | 718 | Egg | Organic eggs, high in protein, comes from the country hens that are now freely roaming around the wasteland. |  |
| 35 | 719 | Raw Chicken | Uncooked raw poultry, an invaluable source of protein in the wasteland. |  |
| 36 | 720 | Cooked Chincken | Raw chicken breast barbecued over a campfire to tender and smoky goodness. |  |
| 37 | 724 | Fried Egg | A fried egg, sunny-side up. |  |
| 38 | 725 | Pepper Steak | This classic stir-fry combines tender, thinly sliced meat with crisp bell peppers in a savory delish. |  |
| 39 | 726 | Shakshuka | A simple comforting dish that’s popular in the middle eastern home cooking, made by scrambling eggs on a hotpan and adding in tomato purée. |  |

## Medical

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 40 | 18 | Bandage | A roll of sterile gauze bandage, can be used to directly stop the bleeding. |  |
| 41 | 19 | IV Blood Bag | A bag of whole blood (O−) with an IV set ready for transfusion. |  |
| 42 | 26 | Rags | An assortment of torn rags, still can be used to stop bleeding. |  |
| 43 | 28 | IV Saline Bag | A bag of saline solution with an IV set ready for transfusion. |  |
| 44 | 29 | Vitamins | Multi-Vitamin supplements helps in boosting blood to recover health twice as fast. |  |
| 45 | 45 | Adrenaline | Adrenaline, medically know as epinephrine, prepares the body for "fight or flight" situations by increasing heart rate, blood flow, and a... |  |
| 46 | 46 | Tetracycline | Broad spectrum antibiotics. |  |
| 47 | 97 | Psilocybin | 🍄Bad trip guaranteed🍄 "ᴛᴇᴄʜɴᴏʙʟᴀᴅᴇ ɴᴇᴠᴇʀ ᴅɪᴇs" y҉̃̀̋̑ = ḿ̬̏ͤͅx̛̘̠̹͋ + b̬͖̏́͢                           "𝓞𝓱𝓲𝓸 𝓼𝓴𝓲𝓫𝓲𝓭𝓲 𝓰𝔂𝓪𝓽𝓽 𝓼𝓲𝓰𝓶𝓪 𝓻𝓲𝔃𝔃" Ｍ... |  |
| 48 | 109 | IV Kit | IV set along with an sterile blood bag, can be used to draw your own blood for a single use in future. |  |
| 49 | 118 | Adhesive Plaster | A type of medical tape with adhesive coating containing zinc oxide, can be used to directly stop the bleeding in an instant. |  |
| 50 | 143 | Charcoal Tablets | Activated charcoal tablets that can filter out toxins. |  |
| 51 | 708 | AI-2 Medkit | The original AI-2 cold war-era medical kit designed to protect Soviet citizens and military personnel from chemical, biological, radiolog... |  |
| 52 | 710 | Stimulants | Psychostimulant shots used for enhancing attention, cognition and physical performance. |  |

## Ammunition

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 53 | 11 | 5.56x45 Ammo | 5.56x45mm NATO intermediate cartridge. |  |
| 54 | 12 | .45 ACP Ammo | .45 ACP pistol cartridge. |  |
| 55 | 13 | 7.62x54R Ammo | 7.62x54mm Rimmed full power cartridge. |  |
| 56 | 14 | 7.62x39 Ammo | 7.62x39mm intermediate cartridge. |  |
| 57 | 15 | 5.45x39 Ammo | 5.45x39mm intermediate cartridge. |  |
| 58 | 16 | .357 M Ammo | .357 Magnum cartridge. |  |
| 59 | 17 | 12 Cal Ammo | 12 Gauge buck shell with 5 pellets. |  |
| 60 | 51 | Crafted Arrow | Wooden sticks carved using knifes into finely matched arrows, but they are flimsy. |  |
| 61 | 52 | Composite Bolt | Modern aluminium core composite bolts with high impact. |  |
| 62 | 58 | .22 LR Ammo | .22 Long Rifle rimfire cartridge. |  |
| 63 | 69 | 9x19 P Ammo | 9x19mm parabellum pistol cartridge. |  |
| 64 | 100 | 9x18 M Ammo | 9x18mm Makarov pistol cartridge. |  |
| 65 | 101 | 9x39 Ammo | 9x39mm intermediate subsonic cartridge. |  |
| 66 | 104 | 50 BMG Ammo Can | This specific type of .50 caliber ammunition and its availability in belt-fed ammo can configurations can be very based for the applicati... |  |
| 67 | 105 | 40mm Grenade | Grenade round charges for the M203 under-barrel grenade launcher. |  |
| 68 | 106 | VOG-25 Grenade | Grenade round charges for the GP-25 under-barrel grenade launcher. |  |
| 69 | 138 | .308 Win Ammo | .308 Winchester full power cartridge. |  |

## Explosives & Throwables

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 70 | 21 | F1 Grenade | Fragmentation grenade type used by the Russian forces, can cause serious injuries at medium ranges but is lethal in small radius. |  |
| 71 | 56 | Landmine | Anti-personnel mine, provides a nasty surprise for anybody unlucky enough to step on it. |  |
| 72 | 99 | Claymore | A directional anti-personnel mine developed by the U.S. |  |
| 73 | 102 | Molotov Cocktail | An incendiary molotov cocktail, improvised by taping a flare to a bottle of good whiskey, It surprisingly does the job. |  |
| 74 | 119 | M18 Smoke Grenade | Smoke grenade used by western forces. |  |
| 75 | 729 | VOG-25 Khatabka | A modified hand grenade version of the VOG-25 grenade round...Its a homemade russian ingenuity. |  |

## Attachments (scopes, grips, suppressors, mags, launchers)

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 76 | 76 | RDS | A standardized rail-mounted, non-magnifying optic with a red dot projected onto the lens. |  |
| 77 | 77 | PU Scope | 3.5x scope of Soviet manufacture from WW2 era. |  |
| 78 | 78 | PSO-1 Scope | A Russian telescopic sight with fixed 4x magnification, manufactured in Novosibirsk. |  |
| 79 | 79 | ACOG Scope | Advanced Combat Optical Gunsight with 4x magnification, attachable to any weapon with a standardized rail system. |  |
| 80 | 80 | 5.45 Suppressor | A muzzle attachment that suppresses the amount of noise generated by firing. |  |
| 81 | 81 | 5.56 Suppressor | A muzzle attachment that suppresses the amount of noise generated by firing. |  |
| 82 | 83 | Bandolier | An ammo belt attached on the rifle stock, its designed to keep ammunition readily available during hunting or combat. |  |
| 83 | 89 | Magpul | A durable rubber accessory attached at the end of a magazine, it makes easy work to pick and swap magazines from pouches, pockets or bags. |  |
| 84 | 90 | Choke | This attachment chokes or constricts the muzzle end of the barrel to alter the shot into a concentrated pattern and thus reducing the spr... |  |
| 85 | 98 | Long Range Scope | A mounted 4x-8x-12x variable magnification scope. |  |
| 86 | 107 | M203 Grenade Launcher | The M203 is a single-shot, muzzle loaded, under-barrel grenade launcher produced by the US military, designed to be attached on western-s... |  |
| 87 | 108 | GP-25 Grenade Launcher | The GP-25 is a single shot, muzzle loaded, under-barrel grenade launcer that belongs to the family of Russian grenade launchers designed ... |  |
| 88 | 111 | AK Grip | Universal tactical forend grip for AK series weapons, greatly reduces maximum spread and recoil when firing. |  |
| 89 | 112 | Rail Grip | Foregrip for firearms with picatinny rails, greatly reduces maximum spread and recoil when firing. |  |
| 90 | 124 | Laser Sight | Projects a red laser beam onto a target, aiding in aim speed and recoil recovery. |  |
| 91 | 127 | Improvised Suppressor | A homemade DIY suppressor that can reduce some firearm noise... |  |
| 92 | 128 | AN/PVS-4 Scope | A standardized rail-mounted, 3.5x magnifying night vision scope, known as the "starlight scope" among western forces. |  |
| 93 | 129 | 1PN51 Scope | A side-mounted, 3.5x magnifying night vision scope, suitable for Warsaw Pact firearms. |  |
| 94 | 130 | C-MAG 5.56 Drum | A compact twin-drum century magazine that holds 100 rounds of 5.56x45 NATO ammo. |  |
| 95 | 131 | 7.62x39 Drum Mag | The Molot Arms, 75-round metal drum magazine that holds 7.62x39mm ammo for AK and other compatible firearms. |  |
| 96 | 142 | Gun Sling | A leather strap harness that aids in carrying an extra primary firearm over the shoulders. |  |
| 97 | 146 | Underbarrel Flashlight | Mounted underbarrel flashlight for primary weapons, provides illumination at night. |  |
| 98 | 147 | Laser AK Grip | Universal front grip and side-mounted laser sight for AK series firearms. |  |
| 99 | 148 | Laser Rail Grip | Foregrip and laser sight combined for firearms with picatinny rails. |  |
| 100 | 704 | 12 Gauge Drum | A ProMag Saiga 12-Gauge, 20-round drum magazine. |  |
| 101 | 715 | 5.45x39 Drum Mag | KCI AK double drum magazine, that holds 95 rounds of 5.45x39mm ammo. |  |
| 102 | 721 | Mini Sight | A mounted non-magnifying optics with an illuminated reticle, intended for use with compatible pistols. |  |
| 103 | 722 | Pistol Suppressor | A muzzle suppressor for select pistols and submachine guns. |  |
| 104 | 723 | Pistol Flashlight | A tactical flashlight used in conjunction with a pistol to illuminate target. | "(stowed)" variant - pistol-mounted flashlight |

## Crafting Materials

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 105 | 22 | 9V Battery | A common 9V battery, used to power various electronic devices. |  |
| 106 | 37 | Ashwood Stick | A firm but flexible long wooden branch of the native ashwood trees. |  |
| 107 | 38 | Wooden Sticks | Multipurpose pieces of wood, ideal for making arrows, kindling, and barricades. |  |
| 108 | 39 | Duct Tape | A strong, versatile, and flexible wide adhesive tape. |  |
| 109 | 40 | Burlap Sack | Made from plant fibres its ideal for shipping and storing stuff. |  |
| 110 | 41 | Rope | Strong rope, used to tie stuff together, be it things or people. |  |
| 111 | 47 | Tomato Seeds Pack | The text on the pack says: "A useful variety for outdoor cultivation in even the wet summers! |  |
| 112 | 48 | Bell Pepper Seeds Pack | The text on the pack says: "A large bell pepper and a great variety for classic bell pepper taste. |  |
| 113 | 49 | Newspapers | Everyday newspapers found in homes before the apocalypse, still they contain useful crafts, guides, and articles about survival and can b... |  |
| 114 | 50 | Woodpiles | Its wood, the basic survival material, can be obtained by chopping up a tree using axes or bladed weapons. |  |
| 115 | 53 | Gasoline Can | A metal jerrycan, that can be filled with large volumes of gasoline, can be refueled again at gas stations. |  |
| 116 | 68 | Zucchini Seeds Pack | The text on the pack says: "This is our most popular zucchini. |  |
| 117 | 70 | Barbed Wire | A roll of gnarly barbed wire made for fencing off walls and barriers. |  |
| 118 | 88 | Protective Case | A lost yellow protective case sealed with loot, belongs to the group of neutral survivors. |  |
| 119 | 113 | Map Notes | The lost property of an exprienced survivor, Alive or Dead, who knows... |  |
| 120 | 116 | Scout Handbook | The handbook provides a comprehensive guide on scouting activities, covering areas like camping, survival skills, first aid, leadership, ... |  |
| 121 | 117 | Canvas | Burlap sacks rolled into a roll for padding and tarp. |  |
| 122 | 122 | Epoxy Glue | A strong, two-part adhesive made from epoxy resin and a hardener, creates a powerful bond on a variety of surfaces, even metal. |  |
| 123 | 123 | Fertilizer | Commercial fertilizers composed of plant matter, animal wastes, and other rich essential nutrients that plants love. |  |
| 124 | 705 | Dye Bucket | A small round container containing a vivid variety of complex colors in all the spectrums of a rainbow...oOh and comes with a dye removin... |  |
| 125 | 714 | Deerskin | A supple, durable and lightweight luxury leather, its exceptionally soft and breathable. |  |

## Tools & Misc Usables

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 126 | 20 | Flare | Provides a small radius of bright red light for a brief time. |  |
| 127 | 23 | Campfire Kit | After deploying, can be lit into a campfire providing a large radius of light and heat for various purposes, such as cooking food, buildi... |  |
| 128 | 24 | Matches | A box of long kitchen safety matches used to light a flame on demand. |  |
| 129 | 34 | Heatpack | Small disposable heat source to build up thermal comfort for a limited time when put into a jacket, pants, or vest. |  |
| 130 | 35 | Sewing Kit | A pocket-sized universal sewing kit, comes handy for patching various clothes and some headwear. |  |
| 131 | 36 | Cleaning Kit | A wooden box containing a complete and universal gun-care system. |  |
| 132 | 54 | Water Bottle | A commom reusable plastic bottle. |  |
| 133 | 55 | Canteen | A reusable military grade plastic canteen used by the army and campers, comes in its own pouch. |  |
| 134 | 57 | Bear Trap | Old-fashioned but functional, this marvel of mechanical engineering can almost snap your leg in half. |  |
| 135 | 59 | Car Tool Kit | A tool kit containing various mechanical tools that can be used for reparing and restoring damaged vehicles. |  |
| 136 | 75 | Radio | A device that transmits or receives radio signals. |  |
| 137 | 82 | Hacksaw | A fine-toothed hand saw with a blade held under tension in a frame, used for cutting materials such as metal or plastics. |  |
| 138 | 86 | Spinning Fishing Rod | An extendable fishing rod with a spinnig reel. |  |
| 139 | 87 | Simple Fishing Rod | A piece of rope tied to a stick. |  |
| 140 | 103 | Flare Gun | A single use flare gun loaded with a flare cartridge. |  |
| 141 | 110 | Whetstone | A whetstone of high grit, can be used to sharpen the edges of various bladed weapons and tools. |  |
| 142 | 115 | Binoculars |  |  |
| 143 | 120 | Cigarettes | Vintage Soviet cigarettes. |  |
| 144 | 125 | Lighter | A vintage mechanical lighter, creates a flame using a mechanical ignition system, powered by a flint and wheel mechanism. |  |
| 145 | 126 | Officer's Keycard | An officer's keycard. |  |
| 146 | 132 | Frying Pan | A cast iron frying pan ideal for searing, frying, and baking. | name clash with id 564 "Pan" (melee slot) - different item (frying pan vs cast-iron pan weapon) |
| 147 | 144 | Blowtorch | A metal working blowtorch fed by gas canisters, primarily used to cut and weld metal. |  |

## Footwear

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 148 | 139 | Sneakers | A pair running shoes with flexible rubber soles, designed for general comfort and casual style. |  |
| 149 | 140 | Woodland Boots | Highly durable, water resistant boots, designed to protect the feet and ankles during all types of outdoor activities in wet environments. |  |
| 150 | 141 | Assault Boots | Combat boots designed for soldiers. |  |
| 151 | 700 | NBC Boots | A part of the "Nuclear, Biological, Chemical" protective suits, these boots are very important for successfully traversing contaminated a... |  |

## Accessories

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 152 | 701 | NBC Gloves | A part of the "Nuclear, Biological, Chemical" protective suits, these gloves are very important for successfully traversing contaminated ... |  |

## Melee Weapons

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 153 | 42 | Hunting Knife | A specialized knife designed with a slightly curved tip, primarily for field dressing game. | dup of id 558 (melee slot) |
| 154 | 43 | Army Knife | Sharp combat knife with a sturdy blade and a handle. | dup of id 559 (melee slot) |
| 155 | 44 | Butcher Knife | A big, heavy, rusted chopping knife. | dup of id 557 (melee slot) |
| 156 | 551 | Hatchet | A Single-handed bladed axe with a hammer head on one side. |  |
| 157 | 552 | Shovel | An ordinary shovel with a strong, steel blade that easily splits both soil and skulls. |  |
| 158 | 553 | Pipe Wrench | Adjustable pipe wrench. |  |
| 159 | 554 | Baseball Bat | A wooden baseball bat. |  |
| 160 | 555 | Fire-Axe | A firefighter axe is the answer, If the question is a town full of zombies. |  |
| 161 | 556 | Crowbar | A durable, multi-purpose melee weapon and tool for prying open doors and crates. |  |
| 162 | 557 | Butcher Knife | A big, heavy, rusted chopping knife. | also referenced at id 44 (1-148 range) |
| 163 | 558 | Hunting Knife | A specialized knife designed with a slightly curved tip, primarily for field dressing game. | also referenced at id 42 (1-148 range) |
| 164 | 559 | Army Knife | Sharp combat knife with a sturdy blade and a handle. | also referenced at id 43 (1-148 range) |
| 165 | 560 | Pickaxe | An iron pick with a wooden handle used by miners to pick away rocks or coal, but in this day and age it will be used to peneterate skulls. |  |
| 166 | 561 | Pitchfork | An agricultural tool with thick prongs at the end to pitch hay, quite capable of piercing through flesh. |  |
| 167 | 562 | Sledgehammer | A sledgehammer that can do devastating damage. |  |
| 168 | 563 | Crusader Sword | In legends, a sword such as this is destined for a great knight. |  |
| 169 | 564 | Pan | A cast-iron pan purposely built for cooking up hate for your enemies. | name clash with id 132 "Frying Pan" (1-148 range) - different item |
| 170 | 565 | Katana | A single-edged, Japanese blade historically used by the samurai class in feudal Japan. |  |
| 171 | 566 | Barbed Bat | A regular baseball bat wrapped in gnarly barbed wire. |  |
| 172 | 567 | Chainsaw | Texas, our Texas! |  |
| 173 | 568 | Generator | A portable generator. |  |
| 174 | 569 | Metal Sheet | Metal sheets scrapped from wrecks, great if you need to do some serious reinforcing and barricading, otherwise not that useful. |  |

## Secondary Firearms (pistols)

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 175 | 211 | FNX 45 | A double action pistol with highly enhanced ergonomics, manufactured by FN America. | dup icon also at id 730-746 (disregarded per request) |
| 176 | 212 | Colt 1911 | "God made man, Sam Colt made 'em equal"  ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶  Damage: 18-30    ... | dup icon also at id 730-746 (disregarded per request) |
| 177 | 213 | Magnum Revolver | Six shot double-action revolver, it can deliver exceptional damage. | dup icon also at id 730-746 (disregarded per request) |
| 178 | 214 | Glock 17 | Affordable, reliable, and compact. | dup icon also at id 730-746 (disregarded per request) |
| 179 | 215 | Amphibia S | The AWC TM-Amphibian S. | dup icon also at id 730-746 (disregarded per request) |
| 180 | 216 | Sawn-Off IZh-43 | Sawed-off double-barreled break-action shotgun. | dup icon also at id 730-746 (disregarded per request) |
| 181 | 217 | Sawn-Off Mosin | Sawed-off five shot bolt-action rifle, fed from internal magazine. | dup icon also at id 730-746 (disregarded per request) |
| 182 | 218 | Engraved Colt | A meticulously engraved Colt 1911 with extended magazine. | dup icon also at id 730-746 (disregarded per request) |
| 183 | 219 | MAC-10 | The Military Armament Corporation model 10 or MAC-10. | dup icon also at id 730-746 (disregarded per request) |
| 184 | 220 | Mare's Leg | "This is wild country out here, ma'am." -Josh Randall  ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶  Dam... | dup icon also at id 730-746 (disregarded per request) |
| 185 | 221 | PM | The Makarov pistol or PM. | dup icon also at id 730-746 (disregarded per request) |
| 186 | 222 | PB | A Soviet, integrally suppressed, semi-automatic pistol used by the KGB and Spetsnaz. | dup icon also at id 730-746 (disregarded per request) |
| 187 | 223 | Desert Eagle | An iconic handgun, known for its large and distinctive appearance and high stopping power. | dup icon also at id 730-746 (disregarded per request) |
| 188 | 224 | M79 | Break-action single shot grenade launcher made in 1960s. | dup icon also at id 730-746 (disregarded per request) |
| 189 | 225 | Sawn-Off Blaze | Sawed-off double-barreled break-action rifle. | dup icon also at id 730-746 (disregarded per request) |
| 190 | 226 | Longhorn | Single shot break action hunting pistol. | dup icon also at id 730-746 (disregarded per request) |
| 191 | 227 | Mini-UZI | A smaller, more compact version of the original UZI submachine gun. | dup icon also at id 730-746 (disregarded per request) |

## Primary Firearms

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 192 | 232 | Mosin-Nagant | A Russian-designed, five-shot, bolt-action rifle that was widely used in world war II. |  |
| 193 | 233 | M4A1 | A carbine developed from the M16 rifle, it serves as the standard-issue infantry weapon for the U.S. |  |
| 194 | 234 | IZh-43 | A Russian-made, side-by-side break-action shotgun used for bear hunting. |  |
| 195 | 235 | AK-74 | A succesor of the AKM, designed by Mikhail Kalashnikov to compete with western rifles like the M16. |  |
| 196 | 236 | AKM | Modernized version of the classic AK-47 with lot of attachment capabilities, designed by Mikhail Kalashnikov. |  |
| 197 | 237 | AKs-74u | A carbine variant of the AK-74 assault rifle, with shortened barrel and foldable stock. |  |
| 198 | 238 | SKS | A popular war souvenir, this rugged, semi-automatic carbine was used extensively in the vietnam war. |  |
| 199 | 240 | L85A2 | A British bullpup assault rifle.The "A2" refers to a major overhaul by HK to transform it into a reliable and accurate firearm. |  |
| 200 | 241 | Remington Model 870 | The classic American pump-action shotgun, renowned for its distinct "pump" sound to threaten enemies. |  |
| 201 | 243 | Improvised Bow | A functional bow made from a branch and a rope. |  |
| 202 | 244 | Hunting Crossbow | A camo patterned hunting crossbow. |  |
| 203 | 246 | Ruger 10/22 Sporter | An elegant, integrally suppressed rifle featuring a hardwood stock and fine styling, great for plinking. |  |
| 204 | 247 | Rossi R92 | A lever-action repeating rifle, modern version of the Winchester Model 92 designed by Amadeo Rossi. |  |
| 205 | 248 | SVD Dragunov | An iconic Soviet, semi-automatic, gas-operated DMR/ Sniper rifle, designed by Yevgeny Dragunov. |  |
| 206 | 249 | MP5K | A compact submachine gun from the HK MP5 family. |  |
| 207 | 251 | UMP-45 | The Universale Maschinen Pistole, A lighter successor to the popular MP5 submachine gun. |  |
| 208 | 252 | RPK | A Soviet LMG, functionally a heavier version of the AKM, designed by Mikhail Kalashnikov. |  |
| 209 | 253 | FN-FAL | A battle rifle with the title "The right arm of the free world", developed in Belgium in 1953, adpoted by NATO. |  |
| 210 | 254 | Saiga-12K | A Russian-made, full automatic, 12 gauge shotgun known for its AK-style design. |  |
| 211 | 255 | AUG A1 | The original and iconic version of the Steyr AUG, is a bullpup assault rifle known for its integrated optic. |  |
| 212 | 256 | PP-19 Bizon | A Russian submachine gun recognizable by its distinctive helical-feed magazine, holds the rounds parallel to the bore. |  |
| 213 | 257 | OTs-14 Groza | A combined assault rifle and grenade launcher in a modular platform, developed in the mid-90s. |  |
| 214 | 258 | VSS Vintorez | The legendary VSS "Special Sniper Rifle", designed for silent engagement with targets. |  |
| 215 | 259 | SV-98 | Bolt-action sniper rifle with a free-floating barrel of the highest precision on top of a laminated chassis. |  |
| 216 | 260 | Madsen | The world's first true LMG, adopted by the Royal Danish Army in 1902. |  |
| 217 | 261 | AN-94 | A complex Russian-made assault rifle with a distinctive "hyper burst" feature. |  |
| 218 | 262 | Silenced Remington | "If the rule you followed brought you to this, of what use was the rule?" - Anton Chigurh  ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ... |  |
| 219 | 263 | M16A2 | The standard-issue service weapon for the U.S. |  |
| 220 | 264 | KRISS Vector | A SMG notable for its unique recoil mitigation system and futuristic appearance with insane firerates. |  |
| 221 | 265 | M4A1 w/Drum | A carbine developed from the M16 rifle, it serves as the standard-issue infantry weapon for the U.S. | w/Drum variant - separate item from base weapon |
| 222 | 266 | AKM w/Drum | Modernized version of the classic AK-47 with lot of attachment capabilities, designed by Mikhail Kalashnikov. | w/Drum variant - separate item from base weapon |
| 223 | 267 | M70 Tundra | The Winchester Model 70, a bolt-action target rifle held in high regard by shooters as "The Rifleman's Rifle". |  |
| 224 | 268 | Saiga-12K w/Drum | A Russian-made, full automatic, 12 gauge shotgun known for its AK-style design. | w/Drum variant - separate item from base weapon |
| 225 | 269 | RPK w/Drum | A Soviet LMG, functionally a heavier version of the AKM, designed by Mikhail Kalashnikov. | w/Drum variant - separate item from base weapon |
| 226 | 270 | M16A2 w/Drum | The standard-issue service weapon for the U.S. | w/Drum variant - separate item from base weapon |
| 227 | 271 | AUG A1 w/Drum | The original and iconic version of the Steyr AUG, is a bullpup assault rifle known for its integrated optic. | w/Drum variant - separate item from base weapon |
| 228 | 272 | Milkor MGL | A lightweight, 40 mm, six-shot, revolver grenade launcher developed in South Africa by Milkor. |  |
| 229 | 273 | Blaze | A break-action hunting rifle, favored for its unique ability to fire two rounds almost instantly. |  |
| 230 | 274 | FAMAS F1 | A French bullpup assault rifle nicknamed "Le Clairon" (The Bugle) for its shape, known for high rate of fire. |  |
| 231 | 275 | Mini-14 | The Ruger Mini-14, a lightweight, semi-automatic DMR, valued for its classic design and versatility. |  |
| 232 | 276 | AK-74 w/Drum | A succesor of the AKM, designed by Mikhail Kalashnikov to compete with western rifles like the M16. | w/Drum variant - separate item from base weapon |
| 233 | 277 | AKs-74U w/Drum | A carbine variant of the AK-74 assault rifle, with shortened barrel and foldable stock. | w/Drum variant - separate item from base weapon |

## Vests / Body Armor

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 234 | 145 | Ghillie Suit | Camouflage suit used to completely conceal the wearer in a natural environment. | dup of id 408 (vest slot) |
| 235 | 401 | Bulletproof Vest | A fabric shell covered over a ballistic armor plate. |  |
| 236 | 402 | Assault Vest | A tactical vest designed for combat, which prioritizes carrying gear and ammo with comfort, offers limited protection from soft padding. |  |
| 237 | 403 | Press Vest | A ballistic press vest able to absorb various types of impact damage. |  |
| 238 | 404 | High Capacity Vest | A tactical vest designed to hold as much gear as possible with comfort, offers limited protection from soft padding. |  |
| 239 | 405 | Kevlar Vest | Hard body armor made from strong, synthetic kevlar fibres, effective at stopping rounds while still offering great carry capacity. |  |
| 240 | 406 | Soviet Vest | A dedicated tactical vest that offers greater protection to the lower abdomen due to an elongated chest section. |  |
| 241 | 407 | Apron | A versatile garment worn over clothes to protect the wearer's body and clothing from substances like dirt, guts, grease, and chemicals. |  |
| 242 | 408 | Ghillie Suit | Camouflage suit used to completely conceal the wearer in a forest environment. | also referenced at id 145 (1-148 range) |
| 243 | 409 | M Heavy Armor | Top grade military armor coated with a layer of experimental compound called "Complex M", stops rounds like its nothing. |  |
| 244 | 410 | Chem Suit | A NBC suit upgraded into a level "A" chem suit through military research to protect against hazardous chemical contaminants. |  |
| 245 | 411 | M Kevlar CBRN Suit | A kevlar vest layered on top of a CBRN suit, combining both their properties. |  |
| 246 | 412 | M Heavy CBRN Suit | A M heavy armor layered on top of a CBRN suit, combining both their properties. |  |
| 247 | 413 | M Ghillie CBRN Suit | A ghillie suit layered on top of a CBRN suit, combining both their properties. |  |
| 248 | 414 | Hazmat Suit | An impermeable, non-conductive, full body suit designed to protect from hazardous chemical and biological threats. |  |
| 249 | 415 | NBC Suit | A multi-layered "Nuclear, Biological and Chemical" suit designed to offer complete protection against these threats. |  |
| 250 | 416 | CRBN Suit | A state of the art version of the chem suit, designed very lightly so that it can layer select vests on top of it. |  |
| 251 | 702 | Chem Suit (stowed) | A NBC suit upgraded into a level "A" chem suit through military research to protect against hazardous chemical contaminants. | "(stowed)" variant - see id 410 Chem Suit |
| 252 | 703 | Hazmat Suit (stowed) | An impermeable, non-conductive, full body suit designed to protect from hazardous chemical and biological threats. | "(stowed)" variant - see id 414 Hazmat Suit |
| 253 | 711 | CBRN Suit | A state of the art version of the chem suit, designed very lightly so that it can layer select vests on top of it. | similar to id 415/416 NBC/CRBN Suit |
| 254 | 712 | NBC Suit |  | marked ❓ in source - possibly unused duplicate of id 713 |
| 255 | 713 | NBC Suit | A multi-layered "Nuclear, Biological and Chemical" suit designed to offer complete protection against these threats. | similar to id 415 NBC Suit / id 712 |

## Helmets / Headwear

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 256 | 133 | Gas Mask | A standard issue army gasmask, with basic filter and circular lenses, provides immunity against toxic gas contaminated zones. | dup of id 454 (helmet slot) |
| 257 | 134 | Balaclava | A close-fitting head warmer that protects the head, face and neck, leaving only the eyes and mouth exposed. | dup of id 461 (helmet slot) |
| 258 | 135 | GP-5 Gas Mask | Full face Soviet gas mask with frontal mount for filter cartridges, provides immunity against toxic gas contaminated zones. | dup of id 473 (helmet slot) |
| 259 | 136 | Clown Mask | A goofy or sinister clown mask. | dup of id 474 (helmet slot) |
| 260 | 451 | Motorcycle Helmet | Protective helmet with visor for motorcycle riders. |  |
| 261 | 452 | Army Helmet | Army infantry helmet, a piece of ballistic headgear designed for protection against shrapnel, blunt impact, and some small firearms. |  |
| 262 | 453 | Hard Hat | Construction safety helmet used to protect the head from impact of falling tools or debris. |  |
| 263 | 454 | Gas Mask | A standard issue army gasmask, with basic filter and circular lenses, provides immunity against toxic gas contaminated zones. | also referenced at id 133 (1-148 range) |
| 264 | 455 | Motorcycke Helmet 2 | Protective helmet with visor for motorcycle riders. |  |
| 265 | 456 | Motorcycle Helmet 3 | Protective helmet with visor for motorcycle riders. |  |
| 266 | 457 | Cap | Soft cap with a curved bill and rounded crown. |  |
| 267 | 458 | Warmhat | A soft, close-fitting brimless beanie, known for its warmth, often associated with drug dealers interested in the crystal blue meth. |  |
| 268 | 459 | Ushanka | A traditional Russian fur hat made with real fur like sheepskin, rabbit, or mink, with ear flaps that can be tied in different positions ... |  |
| 269 | 460 | Beret | A beret with an insignia stitched on one side. |  |
| 270 | 461 | Balaclava | A close-fitting head warmer that protects the head, face and neck, leaving only the eyes and mouth exposed. | also referenced at id 134 (1-148 range) |
| 271 | 462 | Headlamp | A hands-free lighting device worn on the head with an adjustable strap. | also referenced at id 706 (1-148/700 range, "stowed" variant) |
| 272 | 463 | Cowboy Hat | High-crowned, wide-brimmed cowboy hat, often associated with ranch workers, cowboys and men with no names. |  |
| 273 | 464 | Welding Mask | Used by welders to protect the eyes, face and neck from arc lightning, sparks and heat while welding, although not the most durable of he... |  |
| 274 | 465 | Gorka Helmet | A military-grade Russian combat helmet. |  |
| 275 | 466 | Police Cap | A peaked police cap, its worn as part of a police uniform. |  |
| 276 | 467 | NVG | Battery-powered Night Vision Goggles for intensifying images in low light environment, comes with a headstrap. | also referenced at id 707 (700 range, "stowed" variant) |
| 277 | 468 | Bandana | A piece of cloth worn around the head, typically made of cotton or silk and can be styled in many ways. |  |
| 278 | 469 | Crusader Helm | A full-face crusader helm made of cold steel, forged to offer superior head protection for knights during the brutal melee of medieval wa... |  |
| 279 | 470 | Pilot Helmet | A pilot's helmet with a visor designed to provide protection against sunlight and supersonic wind blasts, along with a noise attenuation ... |  |
| 280 | 471 | Altyn helmet | A 4mm thick layer of stamped titanium shell helmet equipped with a face shield, developed in the Soviet Union and adopted by the KGB. |  |
| 281 | 472 | Rys-T Helmet | An improved and lightweight version of the famous "Altyn" helmet. |  |
| 282 | 473 | GP-5 Gas Mask | Full face Soviet gas mask with frontal mount for filter cartridges, provides immunity against toxic gas contaminated zones. | also referenced at id 135 (1-148 range) |
| 283 | 474 | Clown Mask | A goofy or sinister clown mask. | also referenced at id 136 (1-148 range) |
| 284 | 475 | Assault Helmet | Ballistic helmet with rails for accessories and NVG shroud, designed to meet combat applications in the dark while protecting the head. |  |
| 285 | 476 | Assault Helmet | Ballistic helmet with rails for accessories and NVG shroud, designed to meet combat applications in the dark while protecting the head. |  |
| 286 | 477 | Assault Helmet | Ballistic helmet with rails for accessories and NVG shroud, designed to meet combat applications in the dark while protecting the head. |  |
| 287 | 478 | Assault Helmet | Ballistic helmet with rails for accessories and NVG shroud, designed to meet combat applications in the dark while protecting the head. |  |
| 288 | 706 | Headlamp (stowed) | A hands-free lighting device worn on the head with an adjustable strap. | "(stowed)" variant - see id 462 Headlamp |
| 289 | 707 | NVG (stowed) | Battery-powered Night Vision Goggles for intensifying images in low light environment, comes with a headstrap. | "(stowed)" variant - see id 467 NVG |

## Pants

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 290 | 351 | Jeans | Denim trousers, also known as jeans. |  |
| 291 | 352 | Worker Pants | Loosely cut pants suitable for tough work in the outdoors, distinguishable by pockets on legs. |  |
| 292 | 353 | Tracksuit Pants | Casual pants, designed for comfort and athletic purposes. |  |
| 293 | 354 | Gorka Pants | Tactical pants used by the units stationed at Gorka, Chernarus. |  |
| 294 | 355 | Paramedic Pants | Standard issue paramedic pants, not that different from any other work apparel, except for the bright, reflective stripes. |  |
| 295 | 356 | Orel Pants | Uniform pants of the OREL special police force unit, provides decent warmth and comfort with little protection. |  |
| 296 | 357 | Hunter Pants | A set of durable pants with forest camo pattern, designed for prolonged periods of time spent hunting in the wilderness. |  |

## Top-Wear (jackets/shirts)

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 297 | 501 | Hoodie | Hooded sweatshirt with vertical zipper and two large pocket, provides decent protection from the elements. |  |
| 298 | 502 | Shirt | A common long-sleeved, button up, cloth garment for the upper body made from polyester and cotton. |  |
| 299 | 503 | (Unnamed Top-Wear) |  | sprite frame exists but id has no name/description in source |
| 300 | 504 | Hoodie | Hooded sweatshirt with vertical zipper and two large pocket, provides decent protection from the elements. | name duplicated with id 501 |
| 301 | 505 | Raincoat | A jacket made from water resistant fabric with extended collar and hood, keeps one dry from rain. |  |
| 302 | 506 | Raincoat | A jacket made from water resistant fabric with extended collar and hood, keeps one dry from rain. | name duplicated with id 505 |
| 303 | 507 | Jacket | Classic motorcycle leather jacket. |  |
| 304 | 509 | Trench Coat | A long, loose fur coat with a belt, similar in style to a military coat or a classic detective's duds. |  |
| 305 | 510 | Gorka Jacket | Tactical jacket used by the units stationed at Gorka, Chernarus. |  |
| 306 | 511 | T-Shirt | Short sleeve T-Shirt with round neck line and a regular fit. |  |
| 307 | 512 | Paramedic Jacket | Standard issue jacket used by paramedics, not that different from any other work apparel, except for the bright, reflective stripes. |  |
| 308 | 513 | Orel Jacket | Uniform jacket of the OREL special police force unit, offers decent warmth, comfort, and light armor. |  |
| 309 | 514 | Tracksuit Jacket | Casual jacket, designed for comfort and athletic purposes. |  |
| 310 | 515 | Dress | Comfortable women’s dress, will keep you looking pretty in any situation. |  |
| 311 | 516 | Down Jacket | Highly compressible puffer jacket filled with soft and warm under feathers from duck or geese, feels cozy. |  |
| 312 | 517 | Hunter Jacket | A durable jacket with forest camo pattern, designed for prolonged periods of time spent hunting in the wilderness. |  |
| 313 | 518 | Sweater | Thick long-sleeved wool sweater, very warm, very absorbent, and itchy as hell. |  |
| 314 | 519 | Awesome Hoodie | The legendary hoodie belonging to the man, the myth, the legend... |  |

## Backpacks

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 315 | 301 | Taloon Backpack | A lightweight backpack used for all day trips, comfortably supports a small load that fits in. |  |
| 316 | 302 | Mountain Backpack | Tall mountaineering backpack made from hi-synthetic materials, spacious with many pouches but its also very noticeable. |  |
| 317 | 303 | Hunting Backpack | A Standard hunting backpack, very durable and comfortable, if a little old-fashioned. |  |
| 318 | 304 | School Backpack | A child's backpack... |  |
| 319 | 305 | Improvised Sling Bag | A small, burlap bag suspended by one strap over the shoulder. |  |
| 320 | 306 | Improvised Backpack | A Low profile burlap backpack with a wooden frame, not too rigid, but very practical for carrying a decent load. |  |
| 321 | 307 | Camping Backpack | A civilian camping tent with backpacking storage capabilities. |  |
| 322 | 308 | Tortilla Backpack | A combat backpack for prolonged carrying, hits the sweet spot in carrying a lot more gear while still being minimal in size. |  |
| 323 | 309 | Satchel | Handy fanny pack used to carry small items strapped over the chest. |  |
| 324 | 310 | Army Satchel | A small military grade shoulder bag used to store small EDC survival items and tools strapped over the chest for quick use. |  |
| 325 | 311 | Medical Satchel | An medical grade IFAK shoulder bag, designed to store medicine in a protected manner against contaminants. |  |
| 326 | 312 | Supplies Stache | A touch of Devs magic to give your items the ability to dimesion jump! |  |

## Structures / Stashes / Misc Deployables

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 327 | 137 | Mini-Game Console | "Welcome to MiniDayZ+"  ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶ ̶  A Debug console for activating dif... |  |
| 328 | 709 | Doggo Kennel | A weathered pet kennel with chew marks on the door, tucked inside, a brown, short haired dog naps peacefully, curled up on a bed of hay. |  |
| 329 | 716 | Supplies Stash | A touch of Devs magic to give your items the ability to dimesion jump! | possible duplicate of id 312 Supplies Stash |
| 330 | 717 | Secret Stash | A touch of Devs magic to store all your exclusive secret location equipment! |  |
| 331 | 727 | Searchlight Set | A searchlight tripod stand and the head lights that go on top, can illuminate large areas when powered. |  |

## Unidentified Items

| new_id | old_id | name | description | notes |
|---|---|---|---|---|
| 332 | 64 | Unknown Item #64 |  |  |
| 333 | 65 | Unknown Item #65 |  |  |
| 334 | 66 | Unknown Item #66 |  |  |
| 335 | 67 | Unknown Item #67 |  |  |
| 336 | 84 | Unknown Item #84 |  |  |
| 337 | 728 | Unknown Item #728 |  | no name/description found in source |

## Empty / reserved slots (excluded from numbering, no sprite or name)

| old_id | category | reason |
|---|---|---|
| 231 | Primary Firearms | no name/description in source - unused primary-slot frame |
| 239 | Primary Firearms | no name/description in source - unused primary-slot frame |
| 242 | Primary Firearms | no name/description in source - unused primary-slot frame |
| 245 | Primary Firearms | no name/description in source - unused primary-slot frame |
| 250 | Primary Firearms | no name/description in source - unused primary-slot frame |
| 508 | Top-Wear (jackets/shirts) | no sprite frame exists for this id |