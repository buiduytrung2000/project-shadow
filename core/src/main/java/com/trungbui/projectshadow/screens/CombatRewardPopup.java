package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.trungbui.projectshadow.combat.CombatReward;
import com.trungbui.projectshadow.i18n.I18n;

/**
 * Sprint 10 B3 — end-of-combat reward popup.
 *
 * <p>Renders the {@link CombatReward} (gold + items + stress relief target)
 * for a short window (~3 seconds), then auto-dismisses. Player can also
 * click "Continue" to dismiss early. On dismiss, the supplied
 * {@code onContinue} callback runs (usually transitions screen).</p>
 *
 * <p>Pre-Sprint-10 the reward was applied silently — players saw the gold
 * counter change next screen with no feedback. This popup makes the
 * positive feedback loop explicit.</p>
 */
public class CombatRewardPopup extends Table {

    /** Auto-dismiss after this many seconds if player hasn't clicked Continue. */
    public static final float AUTO_DISMISS_SECONDS = 3.0f;

    private final Runnable onContinue;
    private float elapsedSeconds = 0f;
    private boolean dismissed = false;

    public CombatRewardPopup(Skin skin, CombatReward reward, Runnable onContinue) {
        this.onContinue = onContinue;

        // Sprint 10 debug fix: original code called skin.getDrawable("default-pane")
        // which doesn't exist in our uiskin.json. Pick the first drawable that does
        // exist; gracefully skip the background if none of the candidates is present.
        Drawable bg = resolveFirstAvailableDrawable(skin, "tooltip", "window", "list");
        if (bg != null) setBackground(bg);
        pad(24);

        Label title = new Label(I18n.t("combat.reward.title"), skin);
        title.setColor(Color.GOLD);
        add(title).pad(8).row();

        if (reward.gold() > 0) {
            Label gold = new Label(I18n.t("combat.reward.gold", reward.gold()), skin);
            gold.setColor(Color.YELLOW);
            add(gold).pad(4).row();
        }
        for (String itemId : reward.items()) {
            Label it = new Label(I18n.t("combat.reward.item", itemId), skin);
            it.setColor(Color.LIGHT_GRAY);
            add(it).pad(4).row();
        }
        if (reward.stressReliefHeroId() != null && reward.stressReliefAmount() > 0) {
            Label sr = new Label(I18n.t("combat.reward.stress",
                    reward.stressReliefHeroId(), reward.stressReliefAmount()), skin);
            sr.setColor(Color.CYAN);
            add(sr).pad(4).row();
        }

        TextButton continueBtn = new TextButton(I18n.t("combat.reward.continue"), skin);
        continueBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss();
            }
        });
        add(continueBtn).pad(12).width(220).height(50).row();

        pack();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (dismissed) return;
        elapsedSeconds += delta;
        if (elapsedSeconds >= AUTO_DISMISS_SECONDS) {
            dismiss();
        }
    }

    private void dismiss() {
        if (dismissed) return;
        dismissed = true;
        remove(); // detach from stage
        if (onContinue != null) onContinue.run();
    }

    /** For tests: bypasses libGDX rendering, simulates time advance. */
    public boolean isDismissed() {
        return dismissed;
    }

    /**
     * Look up the first drawable name that actually exists in the skin.
     * Returns null if none match (caller skips setBackground gracefully).
     *
     * <p>Skin.getDrawable throws GdxRuntimeException when the name is missing —
     * which crashed end-of-combat the first time this popup fired. This helper
     * makes the lookup defensive against future skin tweaks.</p>
     */
    static Drawable resolveFirstAvailableDrawable(Skin skin, String... candidates) {
        if (skin == null || candidates == null) return null;
        for (String name : candidates) {
            if (name != null && skin.has(name, Drawable.class)) {
                return skin.getDrawable(name);
            }
        }
        return null;
    }
}
