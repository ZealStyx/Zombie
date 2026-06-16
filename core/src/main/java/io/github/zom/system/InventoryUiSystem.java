package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.rendering.FontCache;
import io.github.zom.rendering.TextureCache;
import io.github.zom.util.EntityFactory;
import io.github.zom.util.Inventory;
import io.github.zom.util.ItemInstance;
import io.github.zom.util.ItemPlacement;

/**
 * Handles Stage-based 2D grid inventory UI and drag-and-drop mechanics.
 * Snaps to the bottom right of the screen.
 */
public class InventoryUiSystem extends BaseSystem {

    private ComponentMapper<PlayerComponent>    mPlayer;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<TransformComponent> mTransform;

    private Stage stage;
    private Skin skin;
    private DragAndDrop dnd;

    private Table mainPanel;
    private Table gearTable;
    private Table gridsTable;
    private Label descLabel;

    private boolean open = false;
    private int playerEntityId = -1;

    public InventoryUiSystem() {
        // Run on players
    }

    @Override
    protected void initialize() {
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dnd = new DragAndDrop();

        buildUI();
    }

    private void buildUI() {
        // Transparent dark styling for glassmorphism look
        mainPanel = new Table(skin);
        mainPanel.setBackground("window");
        mainPanel.getColor().a = 0.9f;
        mainPanel.pad(10f);

        // Header Title
        Label.LabelStyle titleStyle = new Label.LabelStyle(FontCache.get().bold(12), Color.CYAN);
        Label title = new Label("GEAR & INVENTORY", titleStyle);
        mainPanel.add(title).colspan(2).left().padBottom(8f).row();

        // Left: Gear slots
        gearTable = new Table(skin);
        gearTable.padRight(12f);

        // Right: Grid panels
        gridsTable = new Table(skin);

        mainPanel.add(gearTable).top().left();
        mainPanel.add(gridsTable).top().right().expandX().fillX().row();

        // Bottom Description Panel
        descLabel = new Label("Hover over an item to view description.", new Label.LabelStyle(FontCache.get().regular(9), Color.LIGHT_GRAY));
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        mainPanel.add(descLabel).colspan(2).width(440f).height(35f).padTop(6f).expandX().fillX();

        mainPanel.setVisible(false);
        stage.addActor(mainPanel);

        // Target for dropping item outside the UI -> drops on the ground
        dnd.addTarget(new DragAndDrop.Target(stage.getRoot()) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                // Highlighting when dragging outside to drop
                descLabel.setText("Drop here to discard item onto the ground.");
                descLabel.getStyle().fontColor = Color.ORANGE;
                return true;
            }

            @Override
            public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
                descLabel.setText("Hover over an item to view description.");
                descLabel.getStyle().fontColor = Color.LIGHT_GRAY;
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                DragPayload dp = (DragPayload) payload.getObject();
                dropItemOnGround(dp.instance);
                removeInstanceFromSource(dp);
                rebuildViews();
            }
        });
    }

    private void positionPanel() {
        mainPanel.pack();
        // Snap to bottom right
        mainPanel.setPosition(
            Gdx.graphics.getWidth() - mainPanel.getWidth() - 10f,
            10f
        );
    }

    @Override
    protected void processSystem() {
        // Find player entity
        if (playerEntityId < 0) {
            IntBag players = world.getAspectSubscriptionManager()
                .get(Aspect.all(PlayerComponent.class, InventoryComponent.class, TransformComponent.class))
                .getEntities();
            if (players.size() > 0) {
                playerEntityId = players.get(0);
            }
        }

        if (playerEntityId < 0) return;

        boolean togglePressed = Gdx.input.isKeyJustPressed(Input.Keys.TAB)
            || (Gdx.app.getType() == Application.ApplicationType.Android && AndroidControllerSystem.inventoryPressed);

        if (togglePressed) {
            open = !open;
            mainPanel.setVisible(open);
            if (open) {
                rebuildViews();
            }
        }

        if (open) {
            stage.act(world.getDelta());
        }
    }

    public void drawStage() {
        if (open) {
            stage.draw();
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        if (open) {
            positionPanel();
        }
    }

    // ── UI Rebuilding ─────────────────────────────────────────────────────────

    public void rebuildViews() {
        if (playerEntityId < 0) return;

        PlayerComponent player = mPlayer.get(playerEntityId);
        InventoryComponent invComp = mInventory.get(playerEntityId);

        gearTable.clearChildren();
        gridsTable.clearChildren();
        dnd.clear();

        // ── 1. Rebuild Gear Slots (Left) ──
        // Gear Slots are 42x42 size
        float slotSize = 42f;

        gearTable.add(new Label("EQUIPPED", new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY))).colspan(3).padBottom(4f).row();

        // Row 1: Helmet | Top | Vest
        gearTable.add(createGearSlot("helmet", player.helmetId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("top", player.topId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("vest", player.vestId, slotSize)).pad(2f).row();

        // Row 2: Held | Pants | Holstered
        gearTable.add(createGearSlot("held", player.heldItemId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("pants", player.pantsId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("holstered", player.holsteredItemId, slotSize)).pad(2f).row();

        // Row 3: Backpack | Footwear | Sling Bag
        gearTable.add(createGearSlot("backpack", player.backpackId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("footwear", player.footwearId, slotSize)).pad(2f);
        gearTable.add(createGearSlot("slingbag", player.slingBagId, slotSize)).pad(2f).row();

        // ── 2. Rebuild Grids (Right) ──
        float cellSize = 30f;

        // Base Inventory (6x4)
        Table baseGrid = createGridTable(invComp.inventory, "base", cellSize);
        gridsTable.add(new Label("BASE INVENTORY", new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY))).left().padBottom(2f).row();
        gridsTable.add(baseGrid).padBottom(8f).row();

        // Backpack Inventory (6xR)
        if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
            Table bpGrid = createGridTable(player.equippedBackpack.container, "backpack", cellSize);
            gridsTable.add(new Label("BACKPACK", new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY))).left().padBottom(2f).row();
            gridsTable.add(bpGrid).padBottom(8f).row();
        }

        // Sling Bag Inventory (6xR or 3xR)
        if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
            Table sbGrid = createGridTable(player.equippedSlingBag.container, "slingbag", cellSize);
            gridsTable.add(new Label("SLING BAG", new Label.LabelStyle(FontCache.get().bold(9), Color.GRAY))).left().padBottom(2f).row();
            gridsTable.add(sbGrid).row();
        }

        positionPanel();
    }

    private Table createGridTable(Inventory inv, String sourceName, float cellSize) {
        Table table = new Table(skin);
        table.setBackground("dialog-dim"); // Subtle background for the grid container

        inv.rebuildOccupiedGrid();

        // Draw slots grid
        for (int r = 0; r < inv.rows; r++) {
            for (int c = 0; c < inv.cols; c++) {
                Table slot = new Table(skin);
                slot.setBackground("textfield"); // Slot background frame
                slot.getColor().a = 0.5f;

                // Make the cell hoverable to highlight
                slot.addListener(new ClickListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        slot.getColor().a = 0.8f;
                    }
                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        slot.getColor().a = 0.5f;
                    }
                });

                table.add(slot).size(cellSize).pad(1f);

                // Register drop target for this specific cell
                final int cellR = r;
                final int cellC = c;
                dnd.addTarget(new DragAndDrop.Target(slot) {
                    @Override
                    public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                        DragPayload dp = (DragPayload) payload.getObject();
                        ItemDef def = ConfigLoader.getItemDatabase().get(dp.instance.itemId);
                        // Check if item fits in this inventory at cell (cellR, cellC)
                        boolean fits = inv.canFit(def, cellR, cellC, dp.instance);
                        slot.getColor().set(fits ? Color.GREEN : Color.RED);
                        return fits;
                    }

                    @Override
                    public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
                        slot.getColor().set(Color.WHITE);
                        slot.getColor().a = 0.5f;
                    }

                    @Override
                    public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                        DragPayload dp = (DragPayload) payload.getObject();
                        removeInstanceFromSource(dp);
                        inv.addAt(dp.instance, cellR, cellC);
                        rebuildViews();
                    }
                });
            }
            table.row();
        }

        // Overlay actual item widgets on top of the grid
        for (ItemPlacement p : inv.placements) {
            ItemDef def = ConfigLoader.getItemDatabase().get(p.instance.itemId);
            if (def == null) continue;

            Table itemWidget = createItemWidget(p.instance, def, p.r, p.c, cellSize);
            
            // Calculate absolute position on grid table
            float w = def.gridW * cellSize + (def.gridW - 1) * 2f;
            float h = def.gridH * cellSize + (def.gridH - 1) * 2f;
            float x = p.c * (cellSize + 2f) + 1f;
            // Rows go top-to-bottom in Scene2D table layout but Y goes bottom-to-top, so invert Y
            float gridHeight = inv.rows * (cellSize + 2f);
            float y = gridHeight - (p.r + def.gridH) * (cellSize + 2f) + 1f;

            table.addActor(itemWidget);
            itemWidget.setBounds(x, y, w, h);

            // Register drag source for this item
            dnd.addSource(new DragAndDrop.Source(itemWidget) {
                @Override
                public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    DragPayload dp = new DragPayload();
                    dp.instance = p.instance;
                    dp.source = "grid:" + sourceName;
                    dp.placement = p;
                    payload.setObject(dp);

                    Table dragActor = createItemWidget(p.instance, def, 0, 0, cellSize);
                    dragActor.setSize(w, h);
                    payload.setDragActor(dragActor);
                    return payload;
                }
            });
        }

        return table;
    }

    private Table createGearSlot(String slotName, int itemId, float size) {
        Table slot = new Table(skin);
        slot.setBackground("textfield");
        slot.getColor().a = 0.6f;

        // Label/Overlay helper
        Label nameLabel = new Label(slotName.toUpperCase().substring(0, Math.min(slotName.length(), 6)), new Label.LabelStyle(FontCache.get().regular(7), Color.DARK_GRAY));
        slot.stack(nameLabel).expand().fill().align(Align.topLeft).pad(2f);

        if (itemId > 0) {
            ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
            if (def != null) {
                PlayerComponent player = mPlayer.get(playerEntityId);
                // Retrieve the actual ItemInstance from PlayerComponent for backpack/slingbag
                ItemInstance instance = null;
                if ("backpack".equals(slotName)) instance = player.equippedBackpack;
                else if ("slingbag".equals(slotName)) instance = player.equippedSlingBag;

                if (instance == null) {
                    instance = ItemInstance.create(itemId, 1);
                }

                Table itemWidget = createItemWidget(instance, def, 0, 0, size - 4f);
                slot.stack(itemWidget).expand().fill();

                // Make gear draggable
                final ItemInstance finalInst = instance;
                dnd.addSource(new DragAndDrop.Source(itemWidget) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        DragPayload dp = new DragPayload();
                        dp.instance = finalInst;
                        dp.source = "slot:" + slotName;
                        payload.setObject(dp);

                        Table dragActor = createItemWidget(finalInst, def, 0, 0, size - 4f);
                        dragActor.setSize(size, size);
                        payload.setDragActor(dragActor);
                        return payload;
                    }
                });
            }
        }

        // Register drop target for gear slot
        dnd.addTarget(new DragAndDrop.Target(slot) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                DragPayload dp = (DragPayload) payload.getObject();
                ItemDef def = ConfigLoader.getItemDatabase().get(dp.instance.itemId);
                boolean compatible = isCompatible(slotName, def);
                slot.getColor().set(compatible ? Color.GREEN : Color.RED);
                return compatible;
            }

            @Override
            public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
                slot.getColor().set(Color.WHITE);
                slot.getColor().a = 0.6f;
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                DragPayload dp = (DragPayload) payload.getObject();
                removeInstanceFromSource(dp);
                
                // Equip new item
                PlayerComponent player = mPlayer.get(playerEntityId);
                player.equip(slotName, dp.instance);
                rebuildViews();
            }
        });

        return slot;
    }

    private Table createItemWidget(ItemInstance inst, ItemDef def, int r, int c, float cellSize) {
        Table table = new Table(skin);
        table.setBackground("dialog-dim");
        table.getColor().a = 0.95f;

        // Icon
        if (def.sprite != null && def.sprite.icon != null) {
            Image icon = new Image(TextureCache.get().region(def.sprite.icon));
            table.add(icon).expand().fill().pad(2f);
        }

        // Quantity count (if stackable)
        boolean stackable = Inventory.isStackable(inst.itemId);
        if (stackable && inst.quantity > 1) {
            Label qty = new Label(String.valueOf(inst.quantity), new Label.LabelStyle(FontCache.get().bold(8), Color.WHITE));
            table.addActor(qty);
            qty.pack();
            // Position bottom right of widget
            table.addListener(event -> {
                qty.setPosition(table.getWidth() - qty.getWidth() - 2f, 2f);
                return false;
            });
        }

        // Loaded clip ammo indicator for guns
        if (def.isGun()) {
            Label ammo = new Label(inst.currentAmmo + "/" + def.clipSize, new Label.LabelStyle(FontCache.get().bold(7), Color.YELLOW));
            table.addActor(ammo);
            ammo.pack();
            table.addListener(event -> {
                ammo.setPosition(2f, 2f);
                return false;
            });
        }

        // Tooltip description hover listener
        table.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                descLabel.setText(def.name + ": " + def.description);
                descLabel.getStyle().fontColor = Color.CYAN;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                descLabel.setText("Hover over an item to view description.");
                descLabel.getStyle().fontColor = Color.LIGHT_GRAY;
            }
        });

        return table;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isCompatible(String slotName, ItemDef def) {
        if (def == null) return false;
        switch (slotName) {
            case "helmet":    return "helmet".equals(def.type);
            case "top":       return "top".equals(def.type);
            case "vest":      return "vest".equals(def.type);
            case "pants":     return "pants".equals(def.type);
            case "footwear":  return "footwear".equals(def.type);
            case "backpack":  return "backpack".equals(def.type) && def.containerRows > 0;
            case "slingbag":  return ("backpack".equals(def.type) || "slingbag".equals(def.type)) && def.containerRows > 0 && def.containerRows <= 2;
            case "held":
            case "holstered": return "melee".equals(def.type) || "primary".equals(def.type) || "secondary".equals(def.type);
            default:          return false;
        }
    }

    private void removeInstanceFromSource(DragPayload dp) {
        PlayerComponent player = mPlayer.get(playerEntityId);
        InventoryComponent inv = mInventory.get(playerEntityId);

        if (dp.source.startsWith("grid:")) {
            String gridName = dp.source.substring(5);
            if ("base".equals(gridName)) {
                inv.inventory.remove(dp.instance);
            } else if ("backpack".equals(gridName)) {
                if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
                    player.equippedBackpack.container.remove(dp.instance);
                }
            } else if ("slingbag".equals(gridName)) {
                if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
                    player.equippedSlingBag.container.remove(dp.instance);
                }
            }
        } else if (dp.source.startsWith("slot:")) {
            String slotName = dp.source.substring(5);
            player.unequip(slotName);
        }
    }

    private void dropItemOnGround(ItemInstance inst) {
        TransformComponent transform = mTransform.get(playerEntityId);
        // Spawn item dropping in the world near player feet
        int droppedEntityId = EntityFactory.createWorldItem(
            world,
            transform.x + transform.w * 0.5f,
            transform.y,
            inst.itemId,
            inst.quantity
        );
        // Set the concrete ItemInstance to preserve sub-grid items inside dropped containers!
        WorldItemComponent droppedComp = world.getMapper(WorldItemComponent.class).get(droppedEntityId);
        droppedComp.itemInstance = inst;
    }

    // Drag-Drop inner payload helper
    private static class DragPayload {
        public ItemInstance instance;
        public String source;
        public ItemPlacement placement;
    }
}
