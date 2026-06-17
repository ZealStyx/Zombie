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
 * Scene2D inventory UI with:
 *   - 6×4 base grid (spec)
 *   - Backpack sub-grid appears immediately when a backpack is equipped
 *   - Sling-bag sub-grid appears when a sling bag is equipped
 *   - Item icons drawn aspect-ratio-correct (Scaling.fit) — never stretched
 *   - Gear slots sized dynamically per-slot to fit the largest compatible item
 *   - Rotation: press R while dragging to rotate item 90° (swaps W↔H)
 *   - dirty flag: rebuildViews() called whenever player equipment changes
 *
 * FIX: catch-all drop target uses a plain Group (not setFillParent) to avoid
 * the libGDX DnD "stage root cannot be a target" crash.
 */
public class InventoryUiSystem extends BaseSystem {

    private ComponentMapper<PlayerComponent>    mPlayer;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<TransformComponent> mTransform;

    private Stage       stage;
    private Skin        skin;
    private DragAndDrop dnd;

    private Table mainPanel;
    private Table gearTable;
    private Table gridsTable;
    private Label descLabel;
    private Group catchAll;

    private boolean open             = false;
    private boolean needsRebuild     = false;
    private int     playerEntityId   = -1;

    /** The item instance currently being dragged (for rotation). */
    private ItemInstance dragging    = null;

    // Per-slot max cell dimensions (computed once from item_grid.json)
    private static final String[] GEAR_SLOTS = {
        "helmet", "top", "vest", "held", "pants", "holstered", "backpack", "footwear", "slingbag"
    };

    @Override
    protected void initialize() {
        stage = new Stage(new ScreenViewport());
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dnd   = new DragAndDrop();
        buildUI();
    }

    private void buildUI() {
        // Catch-all ground-drop target — plain Group, NOT setFillParent
        catchAll = new Group();
        catchAll.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(catchAll);

        mainPanel = new Table(skin);
        mainPanel.setBackground("window");
        mainPanel.getColor().a = 0.92f;
        mainPanel.pad(10f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(FontCache.get().bold(12), Color.CYAN);
        mainPanel.add(new Label("GEAR & INVENTORY", titleStyle)).colspan(2).left().padBottom(8f).row();

        gearTable  = new Table(skin);
        gearTable.padRight(12f);
        gridsTable = new Table(skin);

        mainPanel.add(gearTable).top().left();
        mainPanel.add(gridsTable).top().right().expandX().fillX().row();

        descLabel = new Label("Hover over an item to view description.",
            new Label.LabelStyle(FontCache.get().regular(9), Color.LIGHT_GRAY));
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        mainPanel.add(descLabel).colspan(2).width(460f).height(35f).padTop(6f).expandX().fillX();

        mainPanel.setVisible(false);
        stage.addActor(mainPanel);

        registerCatchAll();
    }

    private void registerCatchAll() {
        dnd.addTarget(new DragAndDrop.Target(catchAll) {
            @Override public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
                descLabel.setText("Release to drop item on the ground.");
                descLabel.getStyle().fontColor = Color.ORANGE;
                return true;
            }
            @Override public void reset(DragAndDrop.Source src, DragAndDrop.Payload payload) {
                descLabel.setText("Hover over an item to view description.");
                descLabel.getStyle().fontColor = Color.LIGHT_GRAY;
            }
            @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                removeInstanceFromSource(dp);
                dropItemOnGround(dp.instance);
                needsRebuild = true;
            }
        });
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

        if (open) stage.act(world.getDelta());
    }

    public void drawStage() { if (open) stage.draw(); }
    public Stage getStage() { return stage; }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        catchAll.setBounds(0, 0, w, h);
        if (open) positionPanel();
    }

    /** Call this from outside (e.g. debug console) to force a UI refresh. */
    public void markDirty() { needsRebuild = true; }

    // ── UI rebuild ────────────────────────────────────────────────────────────

    public void rebuildViews() {
        if (playerEntityId < 0) return;

        PlayerComponent    player  = mPlayer.get(playerEntityId);
        InventoryComponent invComp = mInventory.get(playerEntityId);

        gearTable.clearChildren();
        gridsTable.clearChildren();
        dnd.clear();
        registerCatchAll(); // re-add after dnd.clear()

        float cellSize = (float) ConfigLoader.getItemGridConfig().cellSize;
        ItemGridConfig igc = ConfigLoader.getItemGridConfig();

        // ── Left: gear slots ──────────────────────────────────────────────────
        gearTable.add(new Label("EQUIPPED",
                new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY)))
            .colspan(3).padBottom(4f).row();

        // Row 1: Helmet | Top | Vest
        gearTable.add(createGearSlot("helmet",    player.helmetId,        cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("top",       player.topId,           cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("vest",      player.vestId,          cellSize, igc)).pad(2f).row();
        // Row 2: Held | Pants | Holstered
        gearTable.add(createGearSlot("held",      player.heldItemId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("pants",     player.pantsId,         cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("holstered", player.holsteredItemId, cellSize, igc)).pad(2f).row();
        // Row 3: Backpack | Footwear | Sling Bag
        gearTable.add(createGearSlot("backpack",  player.backpackId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("footwear",  player.footwearId,      cellSize, igc)).pad(2f);
        gearTable.add(createGearSlot("slingbag",  player.slingBagId,      cellSize, igc)).pad(2f).row();

        // ── Right: grid sections ──────────────────────────────────────────────
        Label.LabelStyle sectionStyle = new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY);

        gridsTable.add(new Label("BASE INVENTORY", sectionStyle)).left().padBottom(2f).row();
        gridsTable.add(createGridTable(invComp.inventory, "base", cellSize)).padBottom(8f).row();

        // Backpack sub-grid — shown as soon as equippedBackpack is non-null
        if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
            gridsTable.add(new Label("BACKPACK", sectionStyle)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedBackpack.container, "backpack", cellSize)).padBottom(8f).row();
        }

        // Sling bag sub-grid
        if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
            gridsTable.add(new Label("SLING BAG", sectionStyle)).left().padBottom(2f).row();
            gridsTable.add(createGridTable(player.equippedSlingBag.container, "slingbag", cellSize)).row();
        }

        positionPanel();
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
                    public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
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
                    @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
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

            float pw = iw * cellSize + (iw - 1) * gap;
            float ph = ih * cellSize + (ih - 1) * gap;
            float px = p.c * (cellSize + gap) + gap * 0.5f;
            float gridH = inv.rows * (cellSize + gap);
            float py = gridH - (p.r + ih) * (cellSize + gap) + gap * 0.5f;

            Table widget = createItemWidget(p.instance, def, gd, cellSize);
            table.addActor(widget);
            widget.setBounds(px, py, pw, ph);

            dnd.addSource(new DragAndDrop.Source(widget) {
                @Override
                public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    dragging = p.instance;
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    DragPayload dp = new DragPayload();
                    dp.instance  = p.instance;
                    dp.source    = "grid:" + sourceName;
                    dp.placement = p;
                    payload.setObject(dp);
                    Table ghost = createItemWidget(p.instance, def, gd, cellSize);
                    ghost.setSize(pw, ph);
                    payload.setDragActor(ghost);
                    return payload;
                }
                @Override public void dragStop(InputEvent event, float x, float y, int pointer,
                                               DragAndDrop.Payload payload, DragAndDrop.Target target) {
                    if (target == null) dragging = null;
                }
            });
        }

        return table;
    }

    // ── Gear slot ─────────────────────────────────────────────────────────────

    /**
     * Creates a gear slot widget sized to the largest item compatible with slotName.
     * The equipped item icon is drawn aspect-ratio-correct (never stretched).
     */
    private Table createGearSlot(String slotName, int itemId, float cellSize, ItemGridConfig igc) {
        // Compute slot pixel size from largest compatible item type
        int[] maxSize = igc.slotMaxSize(slotName);  // [maxW, maxH] in grid cells
        float slotW = maxSize[0] * cellSize + (maxSize[0] - 1) * 2f;
        float slotH = maxSize[1] * cellSize + (maxSize[1] - 1) * 2f;

        Table slot = new Table(skin);
        slot.setBackground("textfield");
        slot.getColor().a = 0.55f;

        // Slot label (top-left corner, tiny)
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

                // Icon drawn aspect-ratio correct inside the slot
                if (def.sprite != null && def.sprite.icon != null) {
                    TextureRegion region = TextureCache.get().region(def.sprite.icon);
                    Image icon = new Image(new TextureRegionDrawable(region), Scaling.fit);
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
                    @Override public void dragStop(InputEvent event, float x, float y, int pointer,
                                                   DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        if (target == null) dragging = null;
                    }
                });
            }
        }

        dnd.addTarget(new DragAndDrop.Target(slot) {
            @Override
            public boolean drag(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
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
            @Override public void drop(DragAndDrop.Source src, DragAndDrop.Payload payload, float x, float y, int ptr) {
                DragPayload dp = (DragPayload) payload.getObject();
                removeInstanceFromSource(dp);
                PlayerComponent player = mPlayer.get(playerEntityId);
                player.equip(slotName, dp.instance);
                dragging = null;
                needsRebuild = true;
            }
        });

        // Fix size so slot is always exactly slotW × slotH regardless of content
        Table wrapper = new Table();
        wrapper.add(slot).size(slotW, slotH);
        return wrapper;
    }

    // ── Item widget ───────────────────────────────────────────────────────────

    /**
     * Creates a visual widget for an item.
     * Icon is drawn with Scaling.fit so it fills the cell without stretching.
     * Quantity label is bottom-right; ammo indicator is bottom-left.
     */
    private Table createItemWidget(ItemInstance inst, ItemDef def, ItemGridDef gd, float cellSize) {
        Table table = new Table();
        table.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 0.9f)));

        // Icon — Scaling.fit preserves aspect ratio, no stretch
        if (def.sprite != null && def.sprite.icon != null) {
            TextureRegion region = TextureCache.get().region(def.sprite.icon);
            Image icon = new Image(new TextureRegionDrawable(region), Scaling.fit);
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
            table.addListener(e -> { rot.setPosition(table.getWidth() - rot.getWidth() - 2f, table.getHeight() - rot.getHeight() - 2f); return false; });
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
            } else if ("backpack".equals(gn) && player.equippedBackpack != null && player.equippedBackpack.container != null) {
                player.equippedBackpack.container.remove(dp.instance);
            } else if ("slingbag".equals(gn) && player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
                player.equippedSlingBag.container.remove(dp.instance);
            }
        } else if (dp.source.startsWith("slot:")) {
            player.unequip(dp.source.substring(5));
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
