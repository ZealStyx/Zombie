package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.GameScreen;
import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.HealthComponent;
import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ZedConfig;
import io.github.zom.rendering.FontCache;
import io.github.zom.util.EntityFactory;

/**
 * In-game debug console.
 *
 * Toggle: F3 or backtick (`).
 * When open, a Scene2D dialog appears with a TextField for command input and a
 * scrollable output area. The Stage is drawn by GameScreen after the SpriteBatch.
 *
 * Supported commands:
 *   equip <slot> <itemId>              — equip item to player slot
 *   unequip <slot>                     — clear player slot
 *   give <itemId> [qty]                — add item to inventory
 *   spawn zed <type> [x] [y]           — spawn one zed at position (random skin)
 *   spawn zed <type> r<radius> <amount>— spawn N zeds in radius around player
 *   spawn zed random r<radius> <amount>— spawn N random-type zeds in radius
 *   spawn item <id> [x] [y]            — drop item in world
 *   heal [amount]                      — restore player HP
 *   god                                — toggle invulnerability
 *   kill_all                           — kill all zeds
 *   tp <x> <y>                         — teleport player
 *   hp                                 — print current HP
 *   inv                                — print inventory
 *   debug <type> [sub]                 — toggle debug overlays
 *   clear                              — clear output
 *   help                               — list commands
 */
public class DebugConsoleSystem extends BaseSystem {

    // ── Debug overlay flags (read by render systems) ──────────────────────────
    public static boolean showZedVisionCone   = false;
    public static boolean showZedRanges       = false;
    public static boolean showPlayerCollision = false;
    public static boolean showWorldCollision  = false;

    private static final String[] ALL_ZED_TYPES = {
        "normal", "fast", "army", "tank", "screamer", "shooter", "jumper", "buried"
    };

    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<HealthComponent>         mHealth;
    private ComponentMapper<InventoryComponent>      mInventory;
    private ComponentMapper<CombatComponent>         mCombat;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<ZedComponent>            mZed;
    private ComponentMapper<WorldItemComponent>      mWorldItem;

    private EntitySubscription playerSub;
    private EntitySubscription zedSub;

    private final OrthographicCamera camera;

    // ── Scene2D ───────────────────────────────────────────────────────────────
    private Stage     stage;
    private Skin      skin;
    private TextField inputField;
    private Label     outputLabel;
    private ScrollPane scrollPane;
    private Table     consoleRoot;

    private boolean open = false;
    private String outputBuffer = "";

    public DebugConsoleSystem(OrthographicCamera camera) {
        this.camera = camera;
    }

    @Override
    protected void initialize() {
        playerSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(PlayerComponent.class, TransformComponent.class));
        zedSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(ZedComponent.class));

        buildUI();
    }

    private void buildUI() {
        stage = new Stage(new ScreenViewport());
        skin  = new Skin(Gdx.files.internal("ui/uiskin.json"));

        BitmapFont mono = FontCache.get().regular(9);

        Label.LabelStyle outputStyle = new Label.LabelStyle(mono, Color.LIGHT_GRAY);
        outputLabel = new Label("", outputStyle);
        outputLabel.setAlignment(Align.bottomLeft);
        outputLabel.setWrap(true);

        scrollPane = new ScrollPane(outputLabel, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);

        inputField = new TextField("", skin);
        inputField.setMessageText("Enter command…");

        TextButton submitBtn = new TextButton("Run", skin);
        submitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { submitCommand(); }
        });

        consoleRoot = new Table(skin);
        consoleRoot.setBackground("window");
        consoleRoot.top().left();
        consoleRoot.pad(8f);

        // Title
        BitmapFont boldMono = FontCache.get().bold(10);
        Label.LabelStyle titleStyle = new Label.LabelStyle(boldMono, Color.YELLOW);
        consoleRoot.add(new Label("[DEBUG CONSOLE]  F3 or ` to close", titleStyle))
            .colspan(2).left().padBottom(4f).row();

        consoleRoot.add(scrollPane).colspan(2).width(460f).height(120f)
            .expandX().fillX().padBottom(4f).row();

        consoleRoot.add(inputField).expandX().fillX();
        consoleRoot.add(submitBtn).width(50f).padLeft(4f);

        consoleRoot.setVisible(false);
        consoleRoot.pack();

        stage.addActor(consoleRoot);
        positionConsole();

        // Enter key submits
        inputField.setTextFieldListener((field, c) -> {
            if (c == '\n' || c == '\r') submitCommand();
        });
    }

    private void positionConsole() {
        consoleRoot.setPosition(8f, Gdx.graphics.getHeight() - consoleRoot.getHeight() - 8f);
    }

    // ── ECS process (toggle check each frame) ────────────────────────────────

    @Override
    protected void processSystem() {
        boolean togglePressed =
            Gdx.input.isKeyJustPressed(Input.Keys.F3) ||
            Gdx.input.isKeyJustPressed(Input.Keys.GRAVE);

        if (togglePressed) {
            open = !open;
            consoleRoot.setVisible(open);
            if (open) {
                stage.setKeyboardFocus(inputField);
            } else {
                stage.setKeyboardFocus(null);
            }
        }

        if (open) stage.act(world.getDelta());
    }

    /** Called by GameScreen.render() after batch.end() so the overlay renders on top. */
    public void drawStage(float delta) {
        if (open) stage.draw();
    }

    public Stage getStage() { return stage; }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        positionConsole();
    }

    // ── Command execution ─────────────────────────────────────────────────────

    private void submitCommand() {
        String cmd = inputField.getText().trim();
        inputField.setText("");
        if (cmd.isEmpty()) return;
        print("> " + cmd);
        execute(cmd.split("\\s+"));
    }

    private void execute(String[] args) {
        if (args.length == 0) return;
        try {
            switch (args[0].toLowerCase()) {
                case "equip":    cmdEquip(args);    break;
                case "unequip":  cmdUnequip(args);  break;
                case "give":     cmdGive(args);     break;
                case "spawn":    cmdSpawn(args);    break;
                case "heal":     cmdHeal(args);     break;
                case "god":      cmdGod();          break;
                case "kill_all": cmdKillAll();      break;
                case "tp":       cmdTp(args);       break;
                case "hp":       cmdHp();           break;
                case "inv":      cmdInv();          break;
                case "debug":    cmdDebug(args);    break;
                case "clear":    outputBuffer = ""; outputLabel.setText(""); break;
                case "help":     cmdHelp();         break;
                default: print("Unknown command. Type 'help'.");
            }
        } catch (Exception e) {
            print("Error: " + e.getMessage());
        }
        scrollToBottom();
    }

    private void cmdEquip(String[] a) {
        requireArgs(a, 3);
        int pid = player(); if (pid < 0) return;
        mPlayer.get(pid).equip(a[1], Integer.parseInt(a[2]));
        print("Equipped " + a[2] + " to " + a[1]);
    }

    private void cmdUnequip(String[] a) {
        requireArgs(a, 2);
        int pid = player(); if (pid < 0) return;
        mPlayer.get(pid).unequip(a[1]);
        print("Unequipped " + a[1]);
    }

    private void cmdGive(String[] a) {
        requireArgs(a, 2);
        int pid = player(); if (pid < 0) return;
        int itemId = Integer.parseInt(a[1]);
        int qty    = a.length > 2 ? Integer.parseInt(a[2]) : 1;
        int left   = mInventory.get(pid).inventory.add(itemId, qty);
        print("Added " + (qty - left) + "×" + itemId + (left > 0 ? "  (" + left + " didn't fit)" : ""));
    }

    private void cmdSpawn(String[] a) {
        requireArgs(a, 2);
        int pid = player();
        float px = pid >= 0 ? mTransform.get(pid).x : GameScreen.WORLD_W / 2f;
        float py = pid >= 0 ? mTransform.get(pid).y : GameScreen.WORLD_H / 2f;

        switch (a[1].toLowerCase()) {
            case "zed": {
                requireArgs(a, 3);
                String type = a[2].toLowerCase();

                // Area spawn: spawn zed <type> r<radius> <amount>
                if (a.length > 3 && a[3].toLowerCase().startsWith("r")) {
                    float radius = Float.parseFloat(a[3].substring(1));
                    int amount = a.length > 4 ? Integer.parseInt(a[4]) : 1;
                    int spawned = 0;
                    for (int i = 0; i < amount; i++) {
                        String spawnType = type.equals("random")
                            ? ALL_ZED_TYPES[MathUtils.random(ALL_ZED_TYPES.length - 1)]
                            : type;
                        if (!spawnOneZed(spawnType,
                                px + MathUtils.random(-radius, radius),
                                py + MathUtils.random(-radius, radius))) {
                            print("Unknown zed type: " + spawnType);
                            return;
                        }
                        spawned++;
                    }
                    print("Spawned " + spawned + " zed(" + type + ") in r" + radius);
                } else {
                    // Single spawn: spawn zed <type> [x] [y]
                    float spawnX = a.length > 3 ? Float.parseFloat(a[3]) : px + 50f;
                    float spawnY = a.length > 4 ? Float.parseFloat(a[4]) : py;
                    if (!spawnOneZed(type, spawnX, spawnY)) {
                        print("Unknown zed type: " + type);
                        return;
                    }
                    print("Spawned zed(" + type + ") at " + spawnX + "," + spawnY);
                }
                break;
            }
            case "item": {
                requireArgs(a, 3);
                float spawnX = a.length > 3 ? Float.parseFloat(a[3]) : px + 50f;
                float spawnY = a.length > 4 ? Float.parseFloat(a[4]) : py;
                int itemId = Integer.parseInt(a[2]);
                EntityFactory.createWorldItem(world, spawnX, spawnY, itemId, 1);
                print("Spawned item " + itemId + " at " + spawnX + "," + spawnY);
                break;
            }
            default: print("Unknown spawn target: " + a[1]);
        }
    }

    /** Spawn a single zed with proper skin resolution. Returns false if type is unknown. */
    private boolean spawnOneZed(String type, float x, float y) {
        ZedConfig cfg = ConfigLoader.getZedConfig();
        if (cfg == null) return false;
        String skin = cfg.randomSkinName(type);
        if (skin == null) return false;
        String dead = cfg.getMatchingDeadSkinName(type, skin);
        EntityFactory.createZed(world, x, y, type, skin, dead);
        return true;
    }

    private void cmdHeal(String[] a) {
        int pid = player(); if (pid < 0) return;
        HealthComponent h = mHealth.get(pid);
        if (h == null) { print("Player has no health component."); return; }
        float amount = a.length > 1 ? Float.parseFloat(a[1]) : h.maxHp;
        h.heal(amount);
        h.dead = false;
        print("HP restored to " + h.hp + "/" + h.maxHp);
    }

    private void cmdGod() {
        int pid = player(); if (pid < 0) return;
        if (!mCombat.has(pid)) { print("Player has no combat component."); return; }
        CombatComponent c = mCombat.get(pid);
        c.godMode = !c.godMode;
        print("God mode: " + (c.godMode ? "ON" : "OFF"));
    }

    private void cmdKillAll() {
        IntBag zeds = zedSub.getEntities();
        int[]  data = zeds.getData();
        int    count = 0;
        for (int i = 0, n = zeds.size(); i < n; i++) {
            int zid = data[i];
            if (!mZed.has(zid)) continue;
            ZedComponent z = mZed.get(zid);
            if (z.alive) {
                if (mHealth.has(zid)) {
                    mHealth.get(zid).hp = 0f;
                } else {
                    z.die("die1");
                    if (mAnim.has(zid)) mAnim.get(zid).playOnce("die1", 1.5f);
                }
                count++;
            }
        }
        print("Killed " + count + " zeds.");
    }

    private void cmdTp(String[] a) {
        requireArgs(a, 3);
        int pid = player(); if (pid < 0) return;
        mTransform.get(pid).set(Float.parseFloat(a[1]), Float.parseFloat(a[2]));
        print("Teleported to " + a[1] + "," + a[2]);
    }

    private void cmdHp() {
        int pid = player(); if (pid < 0) return;
        if (!mHealth.has(pid)) { print("No health component."); return; }
        HealthComponent h = mHealth.get(pid);
        print("HP: " + h.hp + " / " + h.maxHp + (h.dead ? " [DEAD]" : ""));
    }

    private void cmdInv() {
        int pid = player(); if (pid < 0) return;
        if (!mInventory.has(pid)) { print("No inventory."); return; }
        print(mInventory.get(pid).inventory.toString());
    }

    private void cmdDebug(String[] a) {
        requireArgs(a, 2);
        switch (a[1].toLowerCase()) {
            case "zed":
                if (a.length > 2 && a[2].equalsIgnoreCase("vision")) {
                    showZedVisionCone = !showZedVisionCone;
                    print("Zed vision cone: " + (showZedVisionCone ? "ON" : "OFF"));
                } else if (a.length > 2 && a[2].equalsIgnoreCase("ranges")) {
                    showZedRanges = !showZedRanges;
                    print("Zed ranges: " + (showZedRanges ? "ON" : "OFF"));
                } else {
                    showZedVisionCone = !showZedVisionCone;
                    showZedRanges = showZedVisionCone;
                    print("Zed debug: " + (showZedVisionCone ? "ON" : "OFF"));
                }
                break;
            case "player":
                showPlayerCollision = !showPlayerCollision;
                print("Player collision: " + (showPlayerCollision ? "ON" : "OFF"));
                break;
            case "collision":
                showWorldCollision = !showWorldCollision;
                print("World collision: " + (showWorldCollision ? "ON" : "OFF"));
                break;
            case "all": {
                boolean on = !(showZedVisionCone || showZedRanges || showPlayerCollision || showWorldCollision);
                showZedVisionCone = showZedRanges = showPlayerCollision = showWorldCollision = on;
                print("All debug: " + (on ? "ON" : "OFF"));
                break;
            }
            default:
                print("Unknown debug type. Options: zed, zed vision, zed ranges, player, collision, all");
        }
    }

    private void cmdHelp() {
        print(
            "Commands:\n" +
            "  equip <slot> <id>              unequip <slot>\n" +
            "  give <id> [qty]                heal [amount]\n" +
            "  god                             kill_all\n" +
            "  tp <x> <y>                     hp   inv   clear\n" +
            "  spawn zed <type> [x] [y]       — single zed\n" +
            "  spawn zed <type> r<R> <N>      — N zeds in radius R\n" +
            "  spawn zed random r<R> <N>      — N random zeds\n" +
            "  spawn item <id> [x] [y]\n" +
            "  debug zed|player|collision|all  [vision|ranges]\n" +
            "Types: normal fast army tank screamer shooter jumper buried\n" +
            "Slots: held holstered vest helmet pants top backpack footwear"
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int player() {
        IntBag bag = playerSub.getEntities();
        if (bag.size() == 0) { print("No player found."); return -1; }
        return bag.getData()[0];
    }

    private void requireArgs(String[] a, int min) {
        if (a.length < min) throw new IllegalArgumentException("Usage: " + a[0] + " requires " + (min-1) + " arg(s)");
    }

    private void print(String msg) {
        outputBuffer += msg + "\n";
        outputLabel.setText(outputBuffer);
    }

    private void scrollToBottom() {
        scrollPane.layout();
        scrollPane.setScrollY(scrollPane.getMaxY());
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin  != null) skin.dispose();
    }
}