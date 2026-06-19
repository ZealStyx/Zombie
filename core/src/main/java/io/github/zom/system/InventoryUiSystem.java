package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDataDef;
import io.github.zom.config.ItemDef;
import io.github.zom.config.ItemGridConfig;
import io.github.zom.config.ItemGridDef;
import io.github.zom.rendering.FontCache;
import io.github.zom.rendering.TextureCache;
import io.github.zom.util.EntityFactory;
import io.github.zom.util.Inventory;
import io.github.zom.util.ItemInstance;
import io.github.zom.util.ItemPlacement;

public class InventoryUiSystem extends BaseSystem {

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
    private Table lootTable;
    private Label descLabel;
    private Label groundHintLabel;

    private boolean open           = false;
    private boolean needsRebuild   = false;
    private int     playerEntityId = -1;

    private com.artemis.EntitySubscription worldItemSub;
    private ItemInstance dragging = null;

    /** Snapshot of nearby entity IDs — change triggers rebuild. */
    private final IntArray lastNearbyIds = new IntArray();

    @Override
    protected void initialize() {
        stage = new Stage(new ScreenViewport());
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dnd   = new DragAndDrop();
        dnd.setDragTime(0);

        worldItemSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(WorldItemComponent.class, TransformComponent.class));

        buildUI();
    }

    // ── UI skeleton ───────────────────────────────────────────────────────────

    private void buildUI() {
        mainPanel = new Table(skin);
        mainPanel.setBackground("window");
        mainPanel.getColor().a = 0.92f;
        mainPanel.pad(10f);

        Label.LabelStyle title = new Label.LabelStyle(FontCache.get().bold(12), Color.CYAN);
        mainPanel.add(new Label("GEAR & INVENTORY", title)).colspan(3).left().padBottom(8f).row();

        gearTable  = new Table(skin); gearTable.padRight(12f);
        gridsTable = new Table(skin);
        lootTable  = new Table(skin); lootTable.padLeft(12f);

        mainPanel.add(gearTable).top().left();
        mainPanel.add(gridsTable).top().left().expandX().fillX();
        mainPanel.add(lootTable).top().left().row();

        descLabel = new Label("Hover over an item to view description.",
            new Label.LabelStyle(FontCache.get().regular(9), Color.LIGHT_GRAY));
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        mainPanel.add(descLabel).colspan(3).width(560f).height(35f).padTop(6f).expandX().fillX();

        groundHintLabel = new Label("[ Release outside panel to drop on ground ]",
            new Label.LabelStyle(FontCache.get().regular(9), Color.ORANGE));
        groundHintLabel.setVisible(false);
        groundHintLabel.pack();
        stage.addActor(groundHintLabel);

        mainPanel.setVisible(false);
        stage.addActor(mainPanel);
    }

    // ── Per-frame ─────────────────────────────────────────────────────────────

    @Override
    protected void processSystem() {
        if (playerEntityId < 0) {
            IntBag bag = world.getAspectSubscriptionManager()
                .get(Aspect.all(PlayerComponent.class, InventoryComponent.class, TransformComponent.class))
                .getEntities();
            if (bag.size() > 0) playerEntityId = bag.get(0);
        }
        if (playerEntityId < 0) return;

        boolean toggle = Gdx.input.isKeyJustPressed(Input.Keys.TAB)
            || (Gdx.app.getType() == Application.ApplicationType.Android
            && AndroidControllerSystem.inventoryPressed);
        if (toggle) {
            open = !open;
            mainPanel.setVisible(open);
            if (open) { needsRebuild = true; lastNearbyIds.clear(); }
        }

        if (dragging != null && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            dragging.rotate();
            needsRebuild = true;
        }

        if (open) {
            if (!needsRebuild) {
                IntArray cur = computeNearbyIds();
                if (!intArrayEquals(cur, lastNearbyIds)) needsRebuild = true;
            }
            if (needsRebuild) { rebuildViews(); needsRebuild = false; }
            stage.act(world.getDelta());
            updateGroundHint();
        }
    }

    public void drawStage()           { if (open) stage.draw(); }
    public Stage getStage()           { return stage; }
    public void resize(int w, int h)  { stage.getViewport().update(w, h, true); if (open) positionPanel(); }
    public void markDirty()           { needsRebuild = true; }

    // ── Nearby ID helpers ────────────────────────────────────────────────────

    private IntArray computeNearbyIds() {
        IntArray out = new IntArray();
        if (playerEntityId < 0) return out;
        TransformComponent ptf = mTransform.get(playerEntityId);
        float pcx = ptf.x + ptf.w * 0.5f, pcy = ptf.y + ptf.h * 0.5f;
        IntBag bag  = worldItemSub.getEntities();
        int[]  data = bag.getData();
        for (int i = 0, n = bag.size(); i < n; i++) {
            int eid = data[i];
            if (!mTransform.has(eid)) continue;
            TransformComponent tf = mTransform.get(eid);
            float dx = (tf.x + tf.w * 0.5f) - pcx;
            float dy = (tf.y + tf.h * 0.5f) - pcy;
            if (dx*dx + dy*dy <= LOOT_RANGE * LOOT_RANGE) out.add(eid);
        }
        out.sort();
        return out;
    }

    private static boolean intArrayEquals(IntArray a, IntArray b) {
        if (a.size != b.size) return false;
        for (int i = 0; i < a.size; i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    // ── Ground-drop hint ──────────────────────────────────────────────────────

    private void updateGroundHint() {
        if (dragging == null) { groundHintLabel.setVisible(false); return; }
        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean over = isPanelBounds(mx, my);
        groundHintLabel.setVisible(!over);
        if (!over) groundHintLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - groundHintLabel.getWidth() / 2f, 24f);
    }

    private boolean isPanelBounds(float sx, float sy) {
        return mainPanel.getX() <= sx && sx <= mainPanel.getX() + mainPanel.getWidth()
            && mainPanel.getY() <= sy && sy <= mainPanel.getY() + mainPanel.getHeight();
    }

    // ── Full rebuild ──────────────────────────────────────────────────────────

    private void rebuildViews() {
        if (playerEntityId < 0) return;
        PlayerComponent    player  = mPlayer.get(playerEntityId);
        InventoryComponent invComp = mInventory.get(playerEntityId);

        gearTable.clearChildren();
        gridsTable.clearChildren();
        lootTable.clearChildren();
        dnd.clear();

        float cellSize = ConfigLoader.getItemGridConfig().cellSize;
        ItemGridConfig igc = ConfigLoader.getItemGridConfig();

        Label.LabelStyle sec = new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY);

        // ── Left: gear slots ──────────────────────────────────────────────────
        gearTable.add(new Label("EQUIPPED", sec)).colspan(3).padBottom(4f).row();
        gearTable.add(createGearSlot("helmet",    player.helmetId,        cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("top",       player.topId,           cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("vest",      player.vestId,          cellSize, igc)).pad(2f).row();
        gearTable.add(createGearSlot("held",      player.heldItemId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("pants",     player.pantsId,         cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("holstered", player.holsteredItemId, cellSize, igc)).pad(2f).row();
        gearTable.add(createGearSlot("backpack",  player.backpackId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("footwear",  player.footwearId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("slingbag",  player.slingBagId,      cellSize, igc)).pad(2f).row();

        // ── Centre: inventory grids ───────────────────────────────────────────
        gridsTable.add(new Label("BASE INVENTORY", sec)).left().padBottom(2f).row();
        gridsTable.add(createGridTable(invComp.inventory, "base", cellSize)).padBottom(8f).row();

        if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
            gridsTable.add(new Label("BACKPACK", sec)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedBackpack.container, "backpack", cellSize)).padBottom(8f).row();
        }
        if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
            gridsTable.add(new Label("SLING BAG", sec)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedSlingBag.container, "slingbag", cellSize)).row();
        }

        // ── Right: nearby loot ────────────────────────────────────────────────
        buildLootPanel(cellSize);
        positionPanel();
    }

    // ── Nearby Loot panel ────────────────────────────────────────────────────

    private void buildLootPanel(float cellSize) {
        Label.LabelStyle sec = new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY);
        lootTable.add(new Label("NEARBY LOOT", sec)).left().padBottom(4f).row();

        IntArray nearbyIds = computeNearbyIds();
        lastNearbyIds.clear();
        lastNearbyIds.addAll(nearbyIds);

        if (nearbyIds.size == 0) {
            lootTable.add(new Label("Nothing nearby",
                new Label.LabelStyle(FontCache.get().regular(8), Color.DARK_GRAY))).pad(8f).row();
        } else {
            Table flow = new Table();
            for (int i = 0; i < nearbyIds.size; i++) {
                int eid = nearbyIds.get(i);
                WorldItemComponent wc  = mWorldItem.get(eid);
                ItemDef            def = ConfigLoader.getItemDatabase().get(wc.itemId);
                ItemGridDef        gd  = ConfigLoader.getItemGridConfig().get(wc.itemId);
                if (def == null) continue;

                ItemInstance inst = wc.getItemInstance();
                int iw = Math.min(inst.effectiveW(), 2);
                int ih = Math.min(inst.effectiveH(), 2);
                float gap = 2f;
                float pw = iw * cellSize + (iw-1)*gap;
                float ph = ih * cellSize + (ih-1)*gap;

                Table widget = createItemWidget(inst, def, cellSize);

                final int   capturedEid  = eid;
                final float cpw = pw, cph = ph;
                final ItemInstance capturedInst = inst;
                final ItemDef capturedDef = def;

                dnd.addSource(new DragAndDrop.Source(widget) {
                    @Override public DragAndDrop.Payload dragStart(InputEvent e, float x, float y, int ptr) {
                        dragging = capturedInst;
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        DragPayload dp = new DragPayload();
                        dp.instance = capturedInst;
                        dp.source   = "world:" + capturedEid;
                        payload.setObject(dp);
                        payload.setDragActor(createItemWidget(capturedInst, capturedDef, cellSize));
                        payload.getDragActor().setSize(cpw, cph);
                        return payload;
                    }
                    @Override public void dragStop(InputEvent e, float x, float y, int ptr,
                                                   DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        dragging = null; // loot stays on ground if target==null
                    }
                });

                Table cell = new Table();
                cell.add(widget).size(pw, ph);
                flow.add(cell).pad(2f);
                if ((i+1) % 3 == 0) flow.row();
            }
            lootTable.add(flow).row();
        }

        // Loot panel as drop target = ground drop for inventory items
        dnd.addTarget(new DragAndDrop.Target(lootTable) {
            @Override public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                          float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                if (dp.source.startsWith("world:")) return false;
                descLabel.setText("Release to drop on the ground.");
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

    // ── Grid table ────────────────────────────────────────────────────────────

    private Table createGridTable(Inventory inv, String sourceName, float cellSize) {
        Table table = new Table(skin);
        inv.rebuildOccupiedGrid();

        // Build 2D array of cell slot actors for multi-cell drag highlighting
        final Table[][] cellSlots = new Table[inv.rows][inv.cols];

        for (int r = 0; r < inv.rows; r++) {
            for (int c = 0; c < inv.cols; c++) {
                Table slot = new Table(skin);
                slot.setBackground("textfield");
                slot.getColor().a = 0.45f;
                table.add(slot).size(cellSize).pad(1f);
                cellSlots[r][c] = slot;

                final int cr = r, cc = c;
                dnd.addTarget(new DragAndDrop.Target(slot) {
                    @Override
                    public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
                                        float x, float y, int ptr) {
                        DragPayload dp = (DragPayload) payload.getObject();
                        int iw = dp.instance.effectiveW();
                        int ih = dp.instance.effectiveH();
                        boolean fits = inv.canFit(cr, cc, dp.instance, dp.instance);

                        // Highlight the full footprint — green if fits, red if not
                        Color highlight = fits ? Color.GREEN : Color.RED;
                        for (int dr = 0; dr < ih; dr++) {
                            for (int dc = 0; dc < iw; dc++) {
                                int nr = cr + dr, nc = cc + dc;
                                if (nr < inv.rows && nc < inv.cols) {
                                    cellSlots[nr][nc].getColor().set(highlight);
                                    cellSlots[nr][nc].getColor().a = 0.6f;
                                }
                            }
                        }
                        return fits;
                    }
                    @Override
                    public void reset(DragAndDrop.Source src, DragAndDrop.Payload payload) {
                        // Reset all cells in the grid
                        for (int rr = 0; rr < inv.rows; rr++)
                            for (int cc2 = 0; cc2 < inv.cols; cc2++) {
                                cellSlots[rr][cc2].getColor().set(Color.WHITE);
                                cellSlots[rr][cc2].getColor().a = 0.45f;
                            }
                    }
                    @Override
                    public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload,
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

        // Item overlay widgets
        float gap = 2f;
        for (ItemPlacement p : inv.placements) {
            ItemDef def = ConfigLoader.getItemDatabase().get(p.instance.itemId);
            if (def == null) continue;

            int   iw  = p.instance.effectiveW();
            int   ih  = p.instance.effectiveH();
            float pw  = iw * cellSize + (iw-1)*gap;
            float ph  = ih * cellSize + (ih-1)*gap;
            float px  = p.c * (cellSize+gap) + gap*0.5f;
            float gridH = inv.rows * (cellSize+gap);
            float py  = gridH - (p.r+ih) * (cellSize+gap) + gap*0.5f;

            Table widget = createItemWidget(p.instance, def, cellSize);
            table.addActor(widget);
            widget.setBounds(px, py, pw, ph);

            final ItemPlacement capturedP   = p;
            final ItemDef       capturedDef = def;
            final float         cpw = pw, cph = ph;

            dnd.addSource(new DragAndDrop.Source(widget) {
                @Override public DragAndDrop.Payload dragStart(InputEvent e, float x, float y, int ptr) {
                    dragging = capturedP.instance;
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    DragPayload dp = new DragPayload();
                    dp.instance  = capturedP.instance;
                    dp.source    = "grid:" + sourceName;
                    dp.placement = capturedP;
                    payload.setObject(dp);
                    Table ghost = createItemWidget(capturedP.instance, capturedDef, cellSize);
                    ghost.setSize(cpw, cph);
                    payload.setDragActor(ghost);
                    return payload;
                }
                @Override public void dragStop(InputEvent e, float x, float y, int ptr,
                                               DragAndDrop.Payload payload, DragAndDrop.Target target) {
                    if (target == null) {
                        float sx = e.getStageX(), sy = e.getStageY();
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

        return table;
    }

    // ── Gear slot ─────────────────────────────────────────────────────────────

    private Table createGearSlot(String slotName, int itemId, float cellSize, ItemGridConfig igc) {
        int[] maxSize = slotMaxSizeFixed(slotName, igc);
        float slotW = maxSize[0] * cellSize + (maxSize[0]-1)*2f;
        float slotH = maxSize[1] * cellSize + (maxSize[1]-1)*2f;

        Table slot = new Table(skin);
        slot.setBackground("textfield");
        slot.getColor().a = 0.55f;

        Label lbl = new Label(slotName.toUpperCase().substring(0, Math.min(slotName.length(), 4)),
            new Label.LabelStyle(FontCache.get().regular(6), new Color(0.7f, 0.7f, 0.7f, 1f)));
        lbl.setAlignment(Align.topLeft);
        slot.add(lbl).expand().top().left().pad(2f).row();

        if (itemId > 0) {
            ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
            ItemGridDef gd = igc.get(itemId);
            if (def != null) {
                PlayerComponent player = mPlayer.get(playerEntityId);
                ItemInstance instance = null;
                if ("held".equals(slotName))      instance = player.equippedHeld;
                else if ("holstered".equals(slotName)) instance = player.equippedHolstered;
                else if ("backpack".equals(slotName))  instance = player.equippedBackpack;
                else if ("slingbag".equals(slotName))  instance = player.equippedSlingBag;
                if (instance == null) instance = ItemInstance.create(itemId, 1);

                boolean weaponSlot  = "held".equals(slotName) || "holstered".equals(slotName);
                boolean displayFlat = weaponSlot && gd != null && gd.isPortrait();

                if (def.sprite != null && def.sprite.icon != null) {
                    TextureRegion region = TextureCache.get().region(def.sprite.icon);
                    // For gear slots pass 0×0 grid dims (ignored when forceFlat=true)
                    // displayFlat is already correct: true for held/holstered portrait weapons
                    ItemIcon icon = new ItemIcon(region, 1, 1, false, displayFlat);
                    slot.add(icon).expand().fill().pad(4f);
                }

                final ItemInstance finalInst = instance;
                dnd.addSource(new DragAndDrop.Source(slot) {
                    @Override public DragAndDrop.Payload dragStart(InputEvent e, float x, float y, int ptr) {
                        dragging = finalInst;
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        DragPayload dp = new DragPayload();
                        dp.instance = finalInst;
                        dp.source   = "slot:" + slotName;
                        payload.setObject(dp);
                        Table ghost = createItemWidget(finalInst, def, cellSize);
                        ghost.setSize(slotW, slotH);
                        payload.setDragActor(ghost);
                        return payload;
                    }
                    @Override public void dragStop(InputEvent e, float x, float y, int ptr,
                                                   DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        if (target == null) {
                            float sx = e.getStageX(), sy = e.getStageY();
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
            @Override public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload,
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
                mPlayer.get(playerEntityId).equip(slotName, dp.instance);
                dragging = null;
                needsRebuild = true;
            }
        });

        Table wrapper = new Table();
        wrapper.add(slot).size(slotW, slotH);
        return wrapper;
    }

    private int[] slotMaxSizeFixed(String slotName, ItemGridConfig igc) {
        boolean weaponSlot = "held".equals(slotName) || "holstered".equals(slotName);
        if (!weaponSlot) return igc.slotMaxSize(slotName);
        int maxW = 1, maxH = 1;
        com.badlogic.gdx.utils.Array<ItemGridDef> items = igc.items;
        if (items == null) return new int[]{maxW, maxH};
        for (ItemGridDef d : items) {
            String t = d.type;
            if (!"melee".equals(t) && !"primary".equals(t) && !"secondary".equals(t)) continue;
            int w = Math.max(d.gridW, d.gridH);
            int h = Math.min(d.gridW, d.gridH);
            if (w > maxW) maxW = w;
            if (h > maxH) maxH = h;
        }
        return new int[]{maxW, maxH};
    }

    // ── Item widget ───────────────────────────────────────────────────────────

    /**
     * Builds the visual widget for one item using the custom ItemIcon actor.
     * No more Image+setRotation — ItemIcon handles all rotation/fit maths in draw().
     */
    private Table createItemWidget(ItemInstance inst, ItemDef def, float cellSize) {
        Table table = new Table();
        table.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 0.9f)));

        if (def.sprite != null && def.sprite.icon != null) {
            TextureRegion region = TextureCache.get().region(def.sprite.icon);
            ItemIcon icon = new ItemIcon(region,
                inst.effectiveW(), inst.effectiveH(),
                inst.rotated, false);
            table.add(icon).expand().fill().pad(2f);
        }

        // Ammo badge — read from ItemDataConfig
        ItemDataDef dd = ConfigLoader.getItemDataConfig().get(inst.itemId);
        if (dd != null && dd.isGun()) {
            Label ammo = new Label(inst.currentAmmo + "/" + dd.clipSize,
                new Label.LabelStyle(FontCache.get().bold(6), Color.YELLOW));
            table.addActor(ammo);
            ammo.pack();
            table.addListener(e -> { ammo.setPosition(2f, 2f); return false; });
        }

        // Stack count badge
        if (Inventory.isStackable(inst.itemId) && inst.quantity > 1) {
            Label qty = new Label(String.valueOf(inst.quantity),
                new Label.LabelStyle(FontCache.get().bold(7), Color.WHITE));
            table.addActor(qty);
            qty.pack();
            table.addListener(e -> { qty.setPosition(table.getWidth()-qty.getWidth()-2f, 2f); return false; });
        }

        // Rotation indicator
        if (inst.rotated) {
            Label rot = new Label("↻", new Label.LabelStyle(FontCache.get().regular(7), Color.CYAN));
            table.addActor(rot);
            rot.pack();
            table.addListener(e -> {
                rot.setPosition(table.getWidth()-rot.getWidth()-2f, table.getHeight()-rot.getHeight()-2f);
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

    // ── ItemIcon — custom actor that draws a TextureRegion with correct rotation/fit ──

    /**
     * Custom actor that draws a TextureRegion correctly fitted inside its bounds,
     * rotating the sprite 90° when the sprite's natural aspect ratio doesn't match
     * the grid cell's aspect ratio.
     *
     * ROOT CAUSE FIX: All gun sprites are LANDSCAPE (wider than tall), but their
     * grid cells are PORTRAIT (gridH > gridW). Simply cramming a landscape texture
     * into a portrait Table cell squishes it. The sprite must be rotated 90° to
     * visually fill the portrait cell correctly.
     *
     * Decision logic:
     *   naturalRotation = (sprite is landscape) AND (cell is portrait)
     *                     OR (sprite is portrait) AND (cell is landscape)
     *                   = (spriteW > spriteH) == (gridH > gridW)
     *   finalRotation   = naturalRotation XOR inst.rotated
     *                     (user R-key toggle flips the natural state)
     *
     * For the held/holstered gear slot icon, forceFlat=true overrides everything
     * and always rotates landscape sprites to lie flat in the wide-and-short slot.
     */
    private static class ItemIcon extends Actor {

        private final TextureRegion region;
        private final boolean       shouldRotate;  // pre-computed at construction

        /**
         * @param region      the sprite texture
         * @param gridW       item's effective grid width  (after inst.rotated applied)
         * @param gridH       item's effective grid height (after inst.rotated applied)
         * @param userRotated inst.rotated — true when user pressed R
         * @param forceFlat   true for held/holstered gear slot: always show lying flat
         */
        ItemIcon(TextureRegion region, int gridW, int gridH,
                 boolean userRotated, boolean forceFlat) {
            this.region = region;

            if (region == null) {
                this.shouldRotate = false;
                return;
            }

            int spriteW = region.getRegionWidth();
            int spriteH = region.getRegionHeight();

            if (forceFlat) {
                // Gear slot: always lay the gun flat → rotate if sprite is portrait
                // (which would be unusual, but handles it correctly)
                this.shouldRotate = spriteH > spriteW; // portrait sprite → rotate to landscape
            } else {
                // Inventory grid cell: rotate when sprite and cell aspects disagree
                boolean spriteIsLandscape = spriteW > spriteH;
                boolean cellIsPortrait    = gridH > gridW;
                boolean naturalRotation   = (spriteIsLandscape == cellIsPortrait);
                this.shouldRotate = naturalRotation ^ userRotated;
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (region == null) return;

            float aw = getWidth(), ah = getHeight();
            float ax = getX(),     ay = getY();
            int   sw = region.getRegionWidth();
            int   sh = region.getRegionHeight();

            batch.setColor(getColor().r, getColor().g, getColor().b,
                getColor().a * parentAlpha);

            if (shouldRotate) {
                // Draw the sprite rotated 90° centred inside aw×ah.
                // batch.draw rotates around (originX,originY) in pre-rotation space.
                // We swap the "logical" w/h so the rotated image fits the actor cell.
                float scaleW = ah / (float) sw;  // sprite natural-width → actor height
                float scaleH = aw / (float) sh;  // sprite natural-height → actor width
                float scale  = Math.min(scaleW, scaleH);

                float dw = sw * scale;  // pre-rotation draw width  (= post-rot visual height)
                float dh = sh * scale;  // pre-rotation draw height (= post-rot visual width)

                // Centre the pre-rotation rect so post-rotation it sits centred in aw×ah
                float dx = ax + (aw - dh) * 0.5f;
                float dy = ay + (ah - dw) * 0.5f;

                batch.draw(region,
                    dx, dy,           // position
                    dh * 0.5f,        // originX (centre of pre-rotation rect)
                    dw * 0.5f,        // originY
                    dh, dw,           // width, height (swapped so 90° lands right)
                    1f, 1f,
                    90f);
            } else {
                // Normal fit-centre draw
                float scaleW = aw / (float) sw;
                float scaleH = ah / (float) sh;
                float scale  = Math.min(scaleW, scaleH);

                float dw = sw * scale;
                float dh = sh * scale;
                float dx = ax + (aw - dw) * 0.5f;
                float dy = ay + (ah - dh) * 0.5f;

                batch.draw(region, dx, dy, dw, dh);
            }

            batch.setColor(1f, 1f, 1f, 1f); // restore
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void positionPanel() {
        mainPanel.pack();
        mainPanel.setPosition(Gdx.graphics.getWidth() - mainPanel.getWidth() - 10f, 10f);
    }

    private boolean isCompatible(String slotName, ItemDef def) {
        if (def == null) return false;
        switch (slotName) {
            case "helmet":   return "helmet".equals(def.type);
            case "top":      return "top".equals(def.type);
            case "vest":     return "vest".equals(def.type);
            case "pants":    return "pants".equals(def.type);
            case "footwear": return "footwear".equals(def.type);
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
            case "holstered": return "melee".equals(def.type)
                || "primary".equals(def.type) || "secondary".equals(def.type);
            default: return false;
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
