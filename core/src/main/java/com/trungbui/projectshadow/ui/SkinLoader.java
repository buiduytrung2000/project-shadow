package com.trungbui.projectshadow.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Loads the libGDX UI skin and overlays PixelLab-generated UI components
 * (ui/components.atlas) on top — registering NinePatch-based button states,
 * panel backgrounds, and icon drawables alongside the default skin.
 *
 * <p>Screens call {@link #load()} instead of {@code new Skin(...)}. If the
 * components atlas hasn't been packed yet (fresh checkout, before
 * {@code ./gradlew packUIComponents}), the loader falls back gracefully to
 * the default skin without registering the new styles.
 */
public final class SkinLoader {

    public static final String COMPONENTS_ATLAS = "ui/components.atlas";

    // Style names exposed to screens. Use these in skin.get(name, StyleClass.class).
    public static final String STYLE_PRIMARY_BUTTON = "primary";
    public static final String STYLE_PRIMARY_IMAGE_BUTTON = "primary";
    public static final String STYLE_PANEL_WINDOW = "panel";
    public static final String STYLE_POPUP_WINDOW = "popup";
    public static final String STYLE_PANEL_TOOLTIP = "panel";

    // Drawable names — accessible via skin.getDrawable(name).
    public static final String DRAWABLE_BTN_UP = "ui-btn-up";
    public static final String DRAWABLE_BTN_DOWN = "ui-btn-down";
    public static final String DRAWABLE_BTN_OVER = "ui-btn-over";
    public static final String DRAWABLE_BTN_DISABLED = "ui-btn-disabled";
    public static final String DRAWABLE_PANEL_MAIN = "ui-panel-main";
    public static final String DRAWABLE_PANEL_TOOLTIP = "ui-panel-tooltip";
    public static final String DRAWABLE_POPUP_BG = "ui-popup-bg";

    public static final String ICON_STAGECOACH = "ui-icon-stagecoach";
    public static final String ICON_GUILD = "ui-icon-guild";
    public static final String ICON_SURVIVALIST = "ui-icon-survivalist";
    public static final String ICON_CARETAKER = "ui-icon-caretaker";
    public static final String ICON_GOLD = "ui-icon-gold";
    public static final String ICON_HEIRLOOM = "ui-icon-heirloom";
    public static final String ICON_SETTINGS = "ui-icon-settings";
    public static final String ICON_CLOSE = "ui-icon-close";

    // Round 2 — Stage map node icons. Map NodeType.COMBAT/ELITE/... → these.
    public static final String NODE_COMBAT = "ui-node-combat";
    public static final String NODE_ELITE = "ui-node-elite";
    public static final String NODE_MINIBOSS = "ui-node-miniboss";
    public static final String NODE_BOSS = "ui-node-boss";
    public static final String NODE_EVENT = "ui-node-event";
    public static final String NODE_REST = "ui-node-rest";
    public static final String NODE_REWARD = "ui-node-reward";

    // Round 2 — Combat HUD status icons.
    public static final String ICON_HP = "ui-icon-hp";
    public static final String ICON_STRESS = "ui-icon-stress";
    public static final String ICON_SHIELD = "ui-icon-shield";

    // Round 2 — Decorative frames.
    public static final String FRAME_PORTRAIT = "ui-frame-portrait";
    public static final String BANNER_TITLE = "ui-banner-title";

    // NinePatch margins per source region. Tuned to the PixelLab outputs —
    // adjust if the generated borders feel too thick/thin.
    private static final int BTN_MARGIN = 16;
    private static final int PANEL_MAIN_MARGIN = 24;
    private static final int PANEL_TOOLTIP_MARGIN = 12;
    private static final int POPUP_MARGIN = 28;
    private static final int FRAME_PORTRAIT_MARGIN = 16;
    private static final int BANNER_TITLE_MARGIN = 20;

    private SkinLoader() {}

    public static Skin load() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        FileHandle atlasFile = Gdx.files.internal(COMPONENTS_ATLAS);
        if (atlasFile.exists()) {
            try {
                TextureAtlas components = new TextureAtlas(atlasFile);
                skin.addRegions(components);
                registerComponentStyles(skin);
                Gdx.app.log("SkinLoader",
                        "Loaded components atlas: " + components.getRegions().size + " regions, "
                                + "btn-up style registered: " + hasComponents(skin));
            } catch (RuntimeException e) {
                Gdx.app.error("SkinLoader", "Failed to load components atlas: " + e.getMessage());
            }
        } else {
            Gdx.app.log("SkinLoader", "components.atlas NOT FOUND at " + atlasFile.path()
                    + " (using default uiskin only)");
        }
        return skin;
    }

    private static void registerComponentStyles(Skin skin) {
        registerNinePatch(skin, DRAWABLE_BTN_UP, "ui_btn_up", BTN_MARGIN);
        registerNinePatch(skin, DRAWABLE_BTN_DOWN, "ui_btn_down", BTN_MARGIN);
        registerNinePatch(skin, DRAWABLE_BTN_OVER, "ui_btn_over", BTN_MARGIN);
        registerNinePatch(skin, DRAWABLE_BTN_DISABLED, "ui_btn_disabled", BTN_MARGIN);

        registerNinePatch(skin, DRAWABLE_PANEL_MAIN, "ui_panel_main", PANEL_MAIN_MARGIN);
        registerNinePatch(skin, DRAWABLE_PANEL_TOOLTIP, "ui_panel_tooltip", PANEL_TOOLTIP_MARGIN);
        registerNinePatch(skin, DRAWABLE_POPUP_BG, "ui_popup_bg", POPUP_MARGIN);

        registerIcon(skin, ICON_STAGECOACH, "ui_icon_stagecoach");
        registerIcon(skin, ICON_GUILD, "ui_icon_guild");
        registerIcon(skin, ICON_SURVIVALIST, "ui_icon_survivalist");
        registerIcon(skin, ICON_CARETAKER, "ui_icon_caretaker");
        registerIcon(skin, ICON_GOLD, "ui_icon_gold");
        registerIcon(skin, ICON_HEIRLOOM, "ui_icon_heirloom");
        registerIcon(skin, ICON_SETTINGS, "ui_icon_settings");
        registerIcon(skin, ICON_CLOSE, "ui_icon_close");

        // Round 2 — stage map node type icons.
        registerIcon(skin, NODE_COMBAT, "ui_node_combat");
        registerIcon(skin, NODE_ELITE, "ui_node_elite");
        registerIcon(skin, NODE_MINIBOSS, "ui_node_miniboss");
        registerIcon(skin, NODE_BOSS, "ui_node_boss");
        registerIcon(skin, NODE_EVENT, "ui_node_event");
        registerIcon(skin, NODE_REST, "ui_node_rest");
        registerIcon(skin, NODE_REWARD, "ui_node_reward");

        // Round 2 — combat status icons.
        registerIcon(skin, ICON_HP, "ui_icon_hp");
        registerIcon(skin, ICON_STRESS, "ui_icon_stress");
        registerIcon(skin, ICON_SHIELD, "ui_icon_shield");

        // Round 2 — decorative frames (NinePatch — stretchable).
        registerNinePatch(skin, FRAME_PORTRAIT, "ui_frame_portrait", FRAME_PORTRAIT_MARGIN);
        registerNinePatch(skin, BANNER_TITLE, "ui_banner_title", BANNER_TITLE_MARGIN);

        if (skin.has(DRAWABLE_BTN_UP, NinePatchDrawable.class)) {
            registerButtonStyles(skin);
        }
        if (skin.has(DRAWABLE_PANEL_MAIN, NinePatchDrawable.class)) {
            registerWindowStyles(skin);
        }
        if (skin.has(DRAWABLE_PANEL_TOOLTIP, NinePatchDrawable.class)) {
            registerTooltipStyle(skin);
        }
    }

    private static void registerNinePatch(Skin skin, String drawableName, String regionName, int margin) {
        TextureAtlas.AtlasRegion r = findRegion(skin, regionName);
        if (r == null) return;
        NinePatch patch = new NinePatch(r, margin, margin, margin, margin);
        NinePatchDrawable d = new NinePatchDrawable(patch);
        // Register under both NinePatchDrawable and Drawable so skin.has(name, Drawable.class)
        // and skin.getDrawable(name) both succeed without surprises.
        skin.add(drawableName, d, NinePatchDrawable.class);
        skin.add(drawableName, d, Drawable.class);
    }

    private static void registerIcon(Skin skin, String drawableName, String regionName) {
        TextureAtlas.AtlasRegion r = findRegion(skin, regionName);
        if (r == null) return;
        TextureRegionDrawable d = new TextureRegionDrawable(r);
        skin.add(drawableName, d, TextureRegionDrawable.class);
        skin.add(drawableName, d, Drawable.class);
    }

    /** Resolve a region from any atlas registered on the skin (scans both default + components). */
    private static TextureAtlas.AtlasRegion findRegion(Skin skin, String name) {
        TextureRegion region = skin.optional(name, TextureRegion.class);
        if (region instanceof TextureAtlas.AtlasRegion ar) return ar;
        return null;
    }

    private static void registerButtonStyles(Skin skin) {
        NinePatchDrawable up = skin.get(DRAWABLE_BTN_UP, NinePatchDrawable.class);
        NinePatchDrawable down = skin.has(DRAWABLE_BTN_DOWN, NinePatchDrawable.class)
                ? skin.get(DRAWABLE_BTN_DOWN, NinePatchDrawable.class) : up;
        NinePatchDrawable over = skin.has(DRAWABLE_BTN_OVER, NinePatchDrawable.class)
                ? skin.get(DRAWABLE_BTN_OVER, NinePatchDrawable.class) : up;
        NinePatchDrawable disabled = skin.has(DRAWABLE_BTN_DISABLED, NinePatchDrawable.class)
                ? skin.get(DRAWABLE_BTN_DISABLED, NinePatchDrawable.class) : up;

        TextButton.TextButtonStyle defaultText = skin.get(TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle primary = new TextButton.TextButtonStyle();
        primary.up = up;
        primary.down = down;
        primary.over = over;
        primary.disabled = disabled;
        primary.font = defaultText.font;
        primary.fontColor = defaultText.fontColor;
        primary.overFontColor = defaultText.overFontColor;
        primary.downFontColor = defaultText.downFontColor;
        primary.disabledFontColor = defaultText.disabledFontColor;
        primary.pressedOffsetX = 1;
        primary.pressedOffsetY = -1;
        skin.add(STYLE_PRIMARY_BUTTON, primary, TextButton.TextButtonStyle.class);

        ImageTextButton.ImageTextButtonStyle primaryImg = new ImageTextButton.ImageTextButtonStyle();
        primaryImg.up = up;
        primaryImg.down = down;
        primaryImg.over = over;
        primaryImg.disabled = disabled;
        primaryImg.font = defaultText.font;
        primaryImg.fontColor = defaultText.fontColor;
        primaryImg.overFontColor = defaultText.overFontColor;
        primaryImg.downFontColor = defaultText.downFontColor;
        primaryImg.disabledFontColor = defaultText.disabledFontColor;
        primaryImg.pressedOffsetX = 1;
        primaryImg.pressedOffsetY = -1;
        skin.add(STYLE_PRIMARY_IMAGE_BUTTON, primaryImg, ImageTextButton.ImageTextButtonStyle.class);
    }

    private static void registerWindowStyles(Skin skin) {
        Window.WindowStyle defaultWindow = skin.get(Window.WindowStyle.class);

        Window.WindowStyle panel = new Window.WindowStyle();
        panel.background = skin.get(DRAWABLE_PANEL_MAIN, NinePatchDrawable.class);
        panel.titleFont = defaultWindow.titleFont;
        panel.titleFontColor = defaultWindow.titleFontColor;
        skin.add(STYLE_PANEL_WINDOW, panel, Window.WindowStyle.class);

        if (skin.has(DRAWABLE_POPUP_BG, NinePatchDrawable.class)) {
            Window.WindowStyle popup = new Window.WindowStyle();
            popup.background = skin.get(DRAWABLE_POPUP_BG, NinePatchDrawable.class);
            popup.titleFont = defaultWindow.titleFont;
            popup.titleFontColor = defaultWindow.titleFontColor;
            skin.add(STYLE_POPUP_WINDOW, popup, Window.WindowStyle.class);
        }
    }

    private static void registerTooltipStyle(Skin skin) {
        TextTooltip.TextTooltipStyle defaultTip = skin.get(TextTooltip.TextTooltipStyle.class);
        TextTooltip.TextTooltipStyle panel = new TextTooltip.TextTooltipStyle();
        panel.background = skin.get(DRAWABLE_PANEL_TOOLTIP, NinePatchDrawable.class);
        panel.label = defaultTip.label;
        skin.add(STYLE_PANEL_TOOLTIP, panel, TextTooltip.TextTooltipStyle.class);
    }

    /** True iff the PixelLab components have been packed and loaded successfully. */
    public static boolean hasComponents(Skin skin) {
        return skin.has(DRAWABLE_BTN_UP, NinePatchDrawable.class);
    }

    /**
     * Overrides the font on the "primary" button/image-button styles. Screens load a
     * Vietnamese-capable BitmapFont via FontFactory and call this to ensure custom
     * button styles render VN characters (default ASCII font causes glyph misses).
     *
     * <p>Safe no-op if components atlas wasn't loaded.</p>
     */
    public static void overrideFont(Skin skin, BitmapFont font) {
        if (skin.has(STYLE_PRIMARY_BUTTON, TextButton.TextButtonStyle.class)) {
            skin.get(STYLE_PRIMARY_BUTTON, TextButton.TextButtonStyle.class).font = font;
        }
        if (skin.has(STYLE_PRIMARY_IMAGE_BUTTON, ImageTextButton.ImageTextButtonStyle.class)) {
            skin.get(STYLE_PRIMARY_IMAGE_BUTTON, ImageTextButton.ImageTextButtonStyle.class).font = font;
        }
    }
}
