package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.config.ItemGridConfig;
import io.github.zom.config.ItemGridDef;
import io.github.zom.rendering.FontCache;
import io.github.zom.rendering.TextureCache;
import io.github.zom.util.EntityFactory;
import io.github.zom.util.Inventory;
import io.github.zom.util.ItemInstance;
import io.github.zom.util.ItemPlacement;

/**
 * Scene2D inventory UI.
 *
 * FIX (5.2 / Bug A & D): Removed full-screen catchAll DragAndDrop.Target.
 *   Ground-drop is now handled in Source.dragStop() when target==null AND
 *   the release point is outside mainPanel. Releasing over dead panel space
 *   (gaps, padding) snaps back — no accidental ground-drops.
 *
 * FIX (5.3 / Bug B): createItemWidget() now visually rotates the icon Image
 *   90° when inst.rotated==true, using setOrigin(center)+setRotation(90).
 *
 * FIX (5.4 / Bug C): slotMaxSize() for held/holstered now treats guns as
 *   horizontal (swaps W↔H when computing max). createGearSlot() force-rotates
 *   the icon for portrait guns in held/holstered slots.
 *
 * NEW  (5.5): Nearby Loot panel — shows WorldItemComponent entities within
 *   LOOT_RANGE while inventory is open. Items are draggable into grids/slots.
 *   Dragging inventory items onto the Nearby Loot panel drops them on ground.
 *   Refreshed every frame (cheap distance check, not just on needsRebuild).
 */
public class InventoryUiSystem extends BaseSystem {

    // ── Loot range (mirrors old ItemPickupSystem.PICKUP_RANGE) ────────────────
    public static final float LOOT_RANGE = 80f;

    private ComponentMapper<PlayerComponent>    mPlayer;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<TransformComponent> mTransform;
    private ComponentMapper<WorldItemComponent> mWorldItem;

    private Stage       stage;
    private Skin        skin;
    private DragAndDrop dnd;

    private Table mainPanel;
    private Table gearTable;
    private Table gridsTable;
    /** Nearby Loot section — always visible inside the panel when open. */
    private Table lootTable;
    private Label descLabel;
    /** Hint label shown when dragging outside the panel. */
    private Label groundHintLabel;

    private boolean open           = false;
    private boolean needsRebuild   = false;
    private int     playerEntityId = -1;

    /** Subscription for world items (for nearby-loot scan). */
    private com.artemis.EntitySubscription worldItemSub;

    /** The item instance currently being dragged (for rotation). */
    private ItemInstance dragging = null;

    private static final String[] GEAR_SLOTS = {
        "helmet", "top", "vest", "held", "pants", "holstered", "backpack", "footwear", "slingbag"
    };

    @Override
    protected void initialize() {
        stage = new Stage(new ScreenViewport());
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dnd   = new DragAndDrop();

        worldItemSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(WorldItemComponent.class, TransformComponent.class));

        buildUI();
    }

    private void buildUI() {
        mainPanel = new Table(skin);
        mainPanel.setBackground("window");
        mainPanel.getColor().a = 0.92f;
        mainPanel.pad(10f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(FontCache.get().bold(12), Color.CYAN);
        mainPanel.add(new Label("GEAR & INVENTORY", titleStyle)).colspan(3).left().padBottom(8f).row();

        gearTable  = new Table(skin);
        gearTable.padRight(12f);
        gridsTable = new Table(skin);
        lootTable  = new Table(skin);
        lootTable.padLeft(12f);

        mainPanel.add(gearTable).top().left();
        mainPanel.add(gridsTable).top().left().expandX().fillX();
        mainPanel.add(lootTable).top().left().row();

        descLabel = new Label("Hover over an item to view description.",
            new Label.LabelStyle(FontCache.get().regular(9), Color.LIGHT_GRAY));
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        mainPanel.add(descLabel).colspan(3).width(560f).height(35f).padTop(6f).expandX().fillX();

        // Ground-drop hint — shown only while dragging outside the panel
        groundHintLabel = new Label("[ Drop to throw on ground ]",
            new Label.LabelStyle(FontCache.get().regular(9), Color.ORANGE));
        groundHintLabel.setVisible(false);
        groundHintLabel.pack();
        stage.addActor(groundHintLabel);

        mainPanel.setVisible(false);
        stage.addActor(mainPanel);
    }

    @Override
    protected void processSystem() {
        // Find player
        if (playerEntityId < 0) {
            IntBag players = world.getAspectSubscriptionManager()
                .get(Aspect.all(PlayerComponent.class, InventoryComponent.class, TransformComponent.class))
                .getEntities();
            if (players.size() > 0) playerEntityId = players.get(0);
        }
        if (playerEntityId < 0) return;

        // Toggle
        boolean toggle = Gdx.input.isKeyJustPressed(Input.Keys.TAB)
            || (Gdx.app.getType() == Application.ApplicationType.Android
                && AndroidControllerSystem.inventoryPressed);
        if (toggle) {
            open = !open;
            mainPanel.setVisible(open);
            if (open) needsRebuild = true;
        }

        // Rotation key (R) during drag
        if (dragging != null && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            dragging.rotate();
            needsRebuild = true;
        }

        // Rebuild when equipment changed or inventory toggled open
        if (open && needsRebuild) {
            rebuildViews();
            needsRebuild = false;
        }

        if (open) {
            stage.act(world.getDelta());
            // Refresh nearby loot every frame (player may have walked)
            refreshLootPanel();
            // Update ground-drop hint visibility while dragging
            updateGroundHint();
        }
    }

    public void drawStage() { if (open) stage.draw(); }
    public Stage getStage() { return stage; }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        if (open) positionPanel();
    }

    public void markDirty() { needsRebuild = true; }

    // ── Ground-drop hint ──────────────────────────────────────────────────────

    private void updateGroundHint() {
        if (dragging == null) {
            groundHintLabel.setVisible(false);
            return;
        }
        // Show hint at bottom-centre of screen when drag pointer is outside panel
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean overPanel = isPanelBounds(mx, my);
        groundHintLabel.setVisible(!overPanel);
        if (!overPanel) {
            groundHintLabel.setPosition(
                Gdx.graphics.getWidth() / 2f - groundHintLabel.getWidth() / 2f,
                20f);
        }
    }

    private boolean isPanelBounds(float sx, float sy) {
        return mainPanel.getX() <= sx && sx <= mainPanel.getX() + mainPanel.getWidth()
            && mainPanel.getY() <= sy && sy <= mainPanel.getY() + mainPanel.getHeight();
    }

    // ── UI rebuild ────────────────────────────────────────────────────────────

    public void rebuildViews() {
        if (playerEntityId < 0) return;

        PlayerComponent    player  = mPlayer.get(playerEntityId);
        InventoryComponent invComp = mInventory.get(playerEntityId);

        gearTable.clearChildren();
        gridsTable.clearChildren();
        lootTable.clearChildren();
        dnd.clear();
        // Note: NO registerCatchAll() — that was the bug root cause.

        float cellSize = ConfigLoader.getItemGridConfig().cellSize;
        ItemGridConfig igc = ConfigLoader.getItemGridConfig();

        // ── Left: gear slots ──────────────────────────────────────────────────
        gearTable.add(new Label("EQUIPPED",
                new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY)))
            .colspan(3).padBottom(4f).row();

        gearTable.add(createGearSlot("helmet",    player.helmetId,        cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("top",       player.topId,           cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("vest",      player.vestId,          cellSize, igc)).pad(2f).row();
        gearTable.add(createGearSlot("held",      player.heldItemId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("pants",     player.pantsId,         cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("holstered", player.holsteredItemId, cellSize, igc)).pad(2f).row();
        gearTable.add(createGearSlot("backpack",  player.backpackId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("footwear",  player.footwearId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("slingbag",  player.slingBagId,      cellSize, igc)).pad(2f).row();

        // ── Centre: grid sections ─────────────────────────────────────────────
        Label.LabelStyle sectionStyle = new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY);

        gridsTable.add(new Label("BASE INVENTORY", sectionStyle)).left().padBottom(2f).row();
        gridsTable.add(createGridTable(invComp.inventory, "base", cellSize)).padBottom(8f).row();

        if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
            gridsTable.add(new Label("BACKPACK", sectionStyle)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedBackpack.container, "backpack", cellSize)).padBottom(8f).row();
        }

        if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
            gridsTable.add(new Label("SLING BAG", sectionStyle)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedSlingBag.container, "slingbag", cellSize)).row();
        }

        // ── Right: Nearby Loot panel (populated by refreshLootPanel) ──────────
        buildLootPanelShell(cellSize);

        positionPanel();
    }

    // ── Nearby Loot shell (built once on rebuild; items refreshed each frame) ──

    private void buildLootPanelShell(float cellSize) {
        Label.LabelStyle sectionStyle = new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY);
        lootTable.add(new Label("NEARBY LOOT", sectionStyle)).left().padBottom(4f).row();
        // Actual loot widgets are added/removed by refreshLootPanel() each frame.
        // We add a placeholder child table that refreshLootPanel replaces.
        Table placeholder = new Table();
        lootTable.add(placeholder).row();

        // The Nearby Loot area is also a drop target — dropping here = ground drop.
        dnd.addTarget(new DragAndDrop.Target(lootTable) {
            @Override
            public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                if (dp.source.startsWith("world:")) return false; // can't drop loot onto loot panel
                descLabel.setText("Release to drop item on the ground.");
                descLabel.getStyle().fontColor = Color.ORANGE;
                return true;
            }
            @Override public void reset(DragAndDrop.Source src, DragAndDrop.Payload payload) {
                descLabel.setText("Hover over an item to view description.");
                descLabel.getStyle().fontColor = Color.LIGHT_GRAY;
            }
            @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                       float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                if (dp.source.startsWith("world:")) return;
                removeInstanceFromSource(dp);
                dropItemOnGround(dp.instance);
                dragging = null;
                needsRebuild = true;
            }
        });
    }

    /**
     * Called every frame while inventory is open.
     * Rebuilds only the loot item widgets inside lootTable (row index 1 onward).
     */
    private void refreshLootPanel() {
        if (playerEntityId < 0) return;

        TransformComponent playerTf = mTransform.get(playerEntityId);
        float pcx = playerTf.x + playerTf.w * 0.5f;
        float pcy = playerTf.y + playerTf.h * 0.5f;

        float cellSize = ConfigLoader.getItemGridConfig().cellSize;

        // Collect nearby entities (this is cheap — small subscription)
        IntBag itemBag = worldItemSub.getEntities();
        int[]  data    = itemBag.getData();

        // Remove all rows after the header row (index 0) before re-adding
        // We achieve this by rebuilding cells from row index 1 onward.
        // Scene2D Table: clearChildren would clear the header too. Instead we
        // track a dedicated inner container refreshed here.
        // Implementation: use a scrollable sub-table as the loot item area.

        // For simplicity (avoids per-frame layout thrash), only rebuild when
        // the set has changed. We snapshot entity ids.
        // However, per the plan refresh is desired every frame—we keep it simple
        // and just rebuild the item container.

        // Find the second cell (index 1) which is our item area container
        if (lootTable.getCells().size < 2) return;
        com.badlogic.gdx.scenes.scene2d.ui.Cell<?> itemCell = lootTable.getCells().get(1);
        Table itemArea = new Table();

        int nearbyCount = 0;
        for (int i = 0, n = itemBag.size(); i < n; i++) {
            int eid = data[i];
            if (!mTransform.has(eid)) continue;
            TransformComponent tf = mTransform.get(eid);
            float dx = (tf.x + tf.w * 0.5f) - pcx;
            float dy = (tf.y + tf.h * 0.5f) - pcy;
            if (dx * dx + dy * dy > LOOT_RANGE * LOOT_RANGE) continue;

            WorldItemComponent wc = mWorldItem.get(eid);
            ItemDef     def = ConfigLoader.getItemDatabase().get(wc.itemId);
            ItemGridDef gd  = ConfigLoader.getItemGridConfig().get(wc.itemId);
            if (def == null) continue;

            ItemInstance inst = wc.getItemInstance();

            // Size: show loot item as its natural footprint capped to 2×2 cells
            // for compactness in the panel
            int iw = Math.min(inst.effectiveW(), 2);
            int ih = Math.min(inst.effectiveH(), 2);
            float gap = 2f;
            float pw = iw * cellSize + (iw - 1) * gap;
            float ph = ih * cellSize + (ih - 1) * gap;

            Table widget = createItemWidget(inst, def, gd, cellSize);
            widget.setSize(pw, ph);

            final int capturedEid = eid;
            final DragPayload dp = new DragPayload();
            dp.instance = inst;
            dp.source   = "world:" + capturedEid;

            dnd.addSource(new DragAndDrop.Source(widget) {
                @Override
                public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    dragging = inst;
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    payload.setObject(dp);
                    Table ghost = createItemWidget(inst, def, gd, cellSize);
                    ghost.setSize(pw, ph);
                    payload.setDragActor(ghost);
                    return payload;
                }
                @Override
                public void dragStop(InputEvent event, float x, float y, int pointer,
                                     DragAndDrop.Payload payload, DragAndDrop.Target target) {
                    // If dropped on no target → snap back (loot stays on ground)
                    if (target == null) dragging = null;
                }
            });

            itemArea.add(widget).size(pw, ph).pad(2f);
            nearbyCount++;
            // Stack 4 items per row in the loot panel
            if (nearbyCount % 4 == 0) itemArea.row();
        }

        if (nearbyCount == 0) {
            itemArea.add(new Label("Nothing nearby",
                new Label.LabelStyle(FontCache.get().regular(8), Color.DARK_GRAY))).pad(4f);
        }

        // Replace the old item area
        itemCell.setActor(itemArea);
        lootTable.invalidate();
    }

    // ── Grid table ────────────────────────────────────────────────────────────

    private Table createGridTable(Inventory inv, String sourceName, float cellSize) {
        Table table = new Table(skin);
        inv.rebuildOccupiedGrid();

        // Empty cell backgrounds + drop targets
        for (int r = 0; r < inv.rows; r++) {
            for (int c = 0; c < inv.cols; c++) {
                Table slot = new Table(skin);
                slot.setBackground("textfield");
                slot.getColor().a = 0.45f;
                slot.addListener(new ClickListener() {
                    @Override public void enter(InputEvent e, float x, float y, int ptr, Actor from) { slot.getColor().a = 0.75f; }
                    @Override public void exit(InputEvent e, float x, float y, int ptr, Actor to)   { slot.getColor().a = 0.45f; }
                });
                table.add(slot).size(cellSize).pad(1f);

                final int cr = r, cc = c;
                dnd.addTarget(new DragAndDrop.Target(slot) {
                    @Override
                    public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                        float x, float y, int ptr) {
                        DragPayload dp = (DragPayload) payload.getObject();
                        boolean fits = inv.canFit(cr, cc, dp.instance, dp.instance);
                        slot.getColor().set(fits ? Color.GREEN : Color.RED);
                        slot.getColor().a = 0.6f;
                        return fits;
                    }
                    @Override public void reset(DragAndDrop.Source src, DragAndDrop.Payload payload) {
                        slot.getColor().set(Color.WHITE);
                        slot.getColor().a = 0.45f;
                    }
                    @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                               float x, float y, int ptr) {
                        DragPayload dp = (DragPayload) payload.getObject();
                        removeInstanceFromSource(dp);
                        inv.addAt(dp.instance, cr, cc);
                        dragging = null;
                        needsRebuild = true;
                    }
                });
            }
            table.row();
        }

        // Overlay item widgets
        float gap = 2f;
        for (ItemPlacement p : inv.placements) {
            ItemDef     def = ConfigLoader.getItemDatabase().get(p.instance.itemId);
            ItemGridDef gd  = ConfigLoader.getItemGridConfig().get(p.instance.itemId);
            if (def == null) continue;

            int iw = p.instance.effectiveW();
            int ih = p.instance.effectiveH();

            float pw  = iw * cellSize + (iw - 1) * gap;
            float ph  = ih * cellSize + (ih - 1) * gap;
            float px  = p.c * (cellSize + gap) + gap * 0.5f;
            float gridH = inv.rows * (cellSize + gap);
            float py  = gridH - (p.r + ih) * (cellSize + gap) + gap * 0.5f;

            Table widget = createItemWidget(p.instance, def, gd, cellSize);
            table.addActor(widget);
            widget.setBounds(px, py, pw, ph);

            // Capture for closures
            final DragPayload dp = new DragPayload();
            dp.instance  = p.instance;
            dp.source    = "grid:" + sourceName;
            dp.placement = p;

            dnd.addSource(new DragAndDrop.Source(widget) {
                @Override
                public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    dragging = dp.instance;
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    payload.setObject(dp);
                    Table ghost = createItemWidget(dp.instance, def, gd, cellSize);
                    ghost.setSize(pw, ph);
                    payload.setDragActor(ghost);
                    return payload;
                }
                @Override
                public void dragStop(InputEvent event, float x, float y, int pointer,
                                     DragAndDrop.Payload payload, DragAndDrop.Target target) {
                    // FIX (Bug A/D): no catchAll anymore.
                    // target==null + outside panel = ground drop; inside panel = snap back.
                    if (target == null) {
                        float sx = event.getStageX(), sy = event.getStageY();
                        if (!isPanelBounds(sx, sy)) {
                            removeInstanceFromSource(dp);
                            dropItemOnGround(dp.instance);
                            needsRebuild = true;
                        }
                        // else: released on dead panel padding — snap back, no-op
                        dragging = null;
                    }
                }
            });
        }

        return table;
    }

    // ── Gear slot ─────────────────────────────────────────────────────────────

    private Table createGearSlot(String slotName, int itemId, float cellSize, ItemGridConfig igc) {
        // FIX (Bug C / 5.4): for held/holstered, compute slot size with guns lying flat.
        int[] maxSize = slotMaxSizeFixed(slotName, igc);
        float slotW = maxSize[0] * cellSize + (maxSize[0] - 1) * 2f;
        float slotH = maxSize[1] * cellSize + (maxSize[1] - 1) * 2f;

        Table slot = new Table(skin);
        slot.setBackground("textfield");
        slot.getColor().a = 0.55f;

        Label nameLabel = new Label(
            slotName.toUpperCase().substring(0, Math.min(slotName.length(), 4)),
            new Label.LabelStyle(FontCache.get().regular(6), new Color(0.7f, 0.7f, 0.7f, 1f)));
        nameLabel.setAlignment(Align.topLeft);
        slot.add(nameLabel).expand().top().left().pad(2f).row();

        if (itemId > 0) {
            ItemDef     def = ConfigLoader.getItemDatabase().get(itemId);
            ItemGridDef gd  = igc.get(itemId);
            if (def != null) {
                PlayerComponent player = mPlayer.get(playerEntityId);
                ItemInstance instance = null;
                if ("backpack".equals(slotName))  instance = player.equippedBackpack;
                else if ("slingbag".equals(slotName)) instance = player.equippedSlingBag;
                if (instance == null) instance = ItemInstance.create(itemId, 1);

                // FIX (Bug C / 5.4): force-rotate portrait guns in held/holstered slots
                boolean isHeldOrHolstered = "held".equals(slotName) || "holstered".equals(slotName);
                boolean displayRotated = isHeldOrHolstered && gd != null && gd.gridH > gd.gridW;

                if (def.sprite != null && def.sprite.icon != null) {
                    TextureRegion region = TextureCache.get().region(def.sprite.icon);
                    Image icon = new Image(new TextureRegionDrawable(region), Scaling.fit);
                    if (displayRotated) {
                        icon.setOrigin(Align.center);
                        icon.setRotation(90f);
                    }
                    slot.add(icon).expand().fill().pad(4f);
                }

                final ItemInstance finalInst = instance;
                dnd.addSource(new DragAndDrop.Source(slot) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                        dragging = finalInst;
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        DragPayload dp = new DragPayload();
                        dp.instance = finalInst;
                        dp.source   = "slot:" + slotName;
                        payload.setObject(dp);
                        Table ghost = createItemWidget(finalInst, def, gd, cellSize);
                        ghost.setSize(slotW, slotH);
                        payload.setDragActor(ghost);
                        return payload;
                    }
                    @Override
                    public void dragStop(InputEvent event, float x, float y, int pointer,
                                         DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        if (target == null) {
                            float sx = event.getStageX(), sy = event.getStageY();
                            if (!isPanelBounds(sx, sy)) {
                                DragPayload dp = (DragPayload) payload.getObject();
                                removeInstanceFromSource(dp);
                                dropItemOnGround(dp.instance);
                                needsRebuild = true;
                            }
                            dragging = null;
                        }
                    }
                });
            }
        }

        dnd.addTarget(new DragAndDrop.Target(slot) {
            @Override
            public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                ItemDef def = ConfigLoader.getItemDatabase().get(dp.instance.itemId);
                boolean ok = isCompatible(slotName, def);
                slot.getColor().set(ok ? Color.GREEN : Color.RED);
                slot.getColor().a = 0.7f;
                return ok;
            }
            @Override public void reset(DragAndDrop.Source src, DragAndDrop.Payload payload) {
                slot.getColor().set(Color.WHITE); slot.getColor().a = 0.55f;
            }
            @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                       float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                removeInstanceFromSource(dp);
                PlayerComponent player = mPlayer.get(playerEntityId);
                player.equip(slotName, dp.instance);
                dragging = null;
                needsRebuild = true;
            }
        });

        Table wrapper = new Table();
        wrapper.add(slot).size(slotW, slotH);
        return wrapper;
    }

    /**
     * FIX (Bug C / 5.4): slotMaxSize for held/holstered treats guns as horizontal
     * (swaps gridW↔gridH before taking max) so the slot is wide, not tall.
     */
    private int[] slotMaxSizeFixed(String slotName, ItemGridConfig igc) {
        boolean isWeaponSlot = "held".equals(slotName) || "holstered".equals(slotName);
        if (!isWeaponSlot) {
            return igc.slotMaxSize(slotName); // unchanged for all other slots
        }
        // Weapon slots: compute max with guns lying flat
        int maxW = 1, maxH = 1;
        com.badlogic.gdx.utils.Array<ItemGridDef> items = igc.items;
        if (items == null) return new int[]{maxW, maxH};
        for (ItemGridDef d : items) {
            String t = d.type;
            if (t == null) continue;
            if ("melee".equals(t) || "primary".equals(t) || "secondary".equals(t)) {
                // Lay flat: long axis → W, short axis → H
                int w = Math.max(d.gridH, d.gridW);
                int h = Math.min(d.gridH, d.gridW);
                if (w > maxW) maxW = w;
                if (h > maxH) maxH = h;
            }
        }
        return new int[]{maxW, maxH};
    }

    // ── Item widget ───────────────────────────────────────────────────────────

    /**
     * Creates a visual widget for an item.
     *
     * FIX (Bug B / 5.3): when inst.rotated is true the icon Image is actually
     *   rotated 90° around its centre so the texture matches the rotated footprint.
     */
    private Table createItemWidget(ItemInstance inst, ItemDef def, ItemGridDef gd, float cellSize) {
        Table table = new Table();
        table.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 0.9f)));

        if (def.sprite != null && def.sprite.icon != null) {
            TextureRegion region = TextureCache.get().region(def.sprite.icon);
            Image icon = new Image(new TextureRegionDrawable(region), Scaling.fit);

            // FIX (Bug B / 5.3): visually rotate the icon when the item is rotated.
            // For a rotated item the cell is now wider than tall, so the icon's
            // "natural" axis must be swapped before rotating 90° around centre.
            if (inst.rotated) {
                icon.setOrigin(Align.center);
                icon.setRotation(90f);
                // expand().fill() still works because libGDX measures the actor's
                // *pre-rotation* bounding box; we rely on the table cell being already
                // sized to the rotated footprint by the calling code (setBounds with
                // swapped pw/ph). This is the standard libGDX pattern.
            }
            table.add(icon).expand().fill().pad(2f);
        }

        // Overlay labels
        if (Inventory.isStackable(inst.itemId) && inst.quantity > 1) {
            Label qty = new Label(String.valueOf(inst.quantity),
                new Label.LabelStyle(FontCache.get().bold(7), Color.WHITE));
            table.addActor(qty);
            qty.pack();
            table.addListener(e -> { qty.setPosition(table.getWidth() - qty.getWidth() - 2f, 2f); return false; });
        }
        if (gd != null && gd.isGun()) {
            Label ammo = new Label(inst.currentAmmo + "/" + gd.clipSize,
                new Label.LabelStyle(FontCache.get().bold(6), Color.YELLOW));
            table.addActor(ammo);
            ammo.pack();
            table.addListener(e -> { ammo.setPosition(2f, 2f); return false; });
        }
        if (inst.rotated) {
            Label rot = new Label("↻", new Label.LabelStyle(FontCache.get().regular(7), Color.CYAN));
            table.addActor(rot);
            rot.pack();
            table.addListener(e -> {
                rot.setPosition(table.getWidth() - rot.getWidth() - 2f,
                                table.getHeight() - rot.getHeight() - 2f);
                return false;
            });
        }

        // Tooltip
        table.addListener(new ClickListener() {
            @Override public void enter(InputEvent e, float x, float y, int ptr, Actor from) {
                descLabel.setText(def.name + ": " + def.description);
                descLabel.getStyle().fontColor = Color.CYAN;
            }
            @Override public void exit(InputEvent e, float x, float y, int ptr, Actor to) {
                descLabel.setText("Hover over an item to view description.");
                descLabel.getStyle().fontColor = Color.LIGHT_GRAY;
            }
        });

        return table;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void positionPanel() {
        mainPanel.pack();
        mainPanel.setPosition(
            Gdx.graphics.getWidth()  - mainPanel.getWidth()  - 10f,
            10f
        );
    }

    private boolean isCompatible(String slotName, ItemDef def) {
        if (def == null) return false;
        switch (slotName) {
            case "helmet":    return "helmet".equals(def.type);
            case "top":       return "top".equals(def.type);
            case "vest":      return "vest".equals(def.type);
            case "pants":     return "pants".equals(def.type);
            case "footwear":  return "footwear".equals(def.type);
            case "backpack": {
                ItemGridDef gd = ConfigLoader.getItemGridConfig().get(def.id);
                return "backpack".equals(def.type) && gd != null && gd.isContainer();
            }
            case "slingbag": {
                ItemGridDef gd = ConfigLoader.getItemGridConfig().get(def.id);
                return ("backpack".equals(def.type) || "slingbag".equals(def.type))
                    && gd != null && gd.isContainer() && gd.containerRows <= 4;
            }
            case "held":
            case "holstered": return "melee".equals(def.type) || "primary".equals(def.type) || "secondary".equals(def.type);
            default:          return false;
        }
    }

    private void removeInstanceFromSource(DragPayload dp) {
        PlayerComponent    player = mPlayer.get(playerEntityId);
        InventoryComponent inv    = mInventory.get(playerEntityId);

        if (dp.source.startsWith("grid:")) {
            String gn = dp.source.substring(5);
            if ("base".equals(gn)) {
                inv.inventory.remove(dp.instance);
            } else if ("backpack".equals(gn) && player.equippedBackpack != null
                        && player.equippedBackpack.container != null) {
                player.equippedBackpack.container.remove(dp.instance);
            } else if ("slingbag".equals(gn) && player.equippedSlingBag != null
                        && player.equippedSlingBag.container != null) {
                player.equippedSlingBag.container.remove(dp.instance);
            }
        } else if (dp.source.startsWith("slot:")) {
            player.unequip(dp.source.substring(5));
        } else if (dp.source.startsWith("world:")) {
            // Loot item dragged into inventory: delete the world entity
            try {
                int eid = Integer.parseInt(dp.source.substring(6));
                world.delete(eid);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void dropItemOnGround(ItemInstance inst) {
        TransformComponent tf = mTransform.get(playerEntityId);
        int eid = EntityFactory.createWorldItem(world,
            tf.x + tf.w * 0.5f, tf.y, inst.itemId, inst.quantity);
        WorldItemComponent wc = world.getMapper(WorldItemComponent.class).get(eid);
        wc.itemInstance = inst;
    }

    private static class DragPayload {
        ItemInstance  instance;
        String        source;
        ItemPlacement placement;
    }

    @Override
    protected void dispose() {
        if (stage != null) stage.dispose();
        if (skin  != null) skin.dispose();
    }
}
