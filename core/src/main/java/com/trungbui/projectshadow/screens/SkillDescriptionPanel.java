package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.trungbui.projectshadow.data.model.SkillData;
import com.trungbui.projectshadow.i18n.I18n;

/**
 * Sprint 9 (feature/skill-description-tooltip) — floating panel that shows the
 * details of the skill the player is hovering over in combat.
 *
 * <p>Contents (top → bottom):</p>
 * <ol>
 *   <li>Title (small caps "SKILL" / "KỸ NĂNG").</li>
 *   <li>Skill displayName (large, gold-colored).</li>
 *   <li>Multi-line description from {@link SkillData#formattedDescription()}.</li>
 * </ol>
 *
 * <p>The panel is initially hidden ({@code setVisible(false)}); callers invoke
 * {@link #showFor(SkillData)} to populate + reveal and {@link #hide()} to clear.</p>
 *
 * <p>Designed to sit in a top-right corner of the {@code CombatScreen} Stage. Width
 * fixed at 480px so it doesn't overlap the combat grid (1920px viewport).</p>
 */
public final class SkillDescriptionPanel extends Table {

    private static final float PANEL_WIDTH = 480f;
    private static final float TITLE_PAD = 4f;
    private static final float BODY_PAD = 6f;

    private final Label titleLabel;
    private final Label nameLabel;
    private final Label bodyLabel;

    public SkillDescriptionPanel(Skin skin) {
        super();
        this.titleLabel = new Label(I18n.t("skill.tooltip.title"), skin);
        this.titleLabel.setColor(Color.LIGHT_GRAY);

        this.nameLabel = new Label("", skin);
        this.nameLabel.setColor(Color.GOLD);

        this.bodyLabel = new Label("", skin);
        this.bodyLabel.setColor(Color.WHITE);
        this.bodyLabel.setWrap(true);

        // Layout: title row, name row, body row (wrapped)
        defaults().left().padLeft(12f).padRight(12f);
        add(titleLabel).padTop(8f).padBottom(TITLE_PAD).row();
        add(nameLabel).padBottom(BODY_PAD).row();
        add(bodyLabel).width(PANEL_WIDTH - 24f).padBottom(12f).row();

        setVisible(false);
    }

    /**
     * Populate the panel for the given skill and show it. Safe to call repeatedly
     * (the existing labels are overwritten in place — no actor churn).
     */
    public void showFor(SkillData skill) {
        if (skill == null) {
            hide();
            return;
        }
        // Refresh title in case locale changed since construction
        titleLabel.setText(I18n.t("skill.tooltip.title"));
        nameLabel.setText(skill.displayName());
        bodyLabel.setText(skill.formattedDescription());
        setVisible(true);
    }

    /** Hide the panel (clears visibility but keeps contents to avoid flicker). */
    public void hide() {
        setVisible(false);
    }

    public float panelWidth() {
        return PANEL_WIDTH;
    }
}
