package com.trungbui.projectshadow.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.trungbui.projectshadow.data.model.ItemData;
import com.trungbui.projectshadow.i18n.I18n;

import java.util.List;
import java.util.function.Consumer;

/**
 * Sprint 13 B2 — reward card picker shown after winning an Elite / Miniboss / Boss fight.
 *
 * <p>Displays up to 3 item cards with name, description and rarity tint. The player
 * must click one card to add it to the run inventory — there is NO auto-dismiss timer
 * (unlike {@link CombatRewardPopup}) because a choice is required.</p>
 *
 * <p>Reuses {@link CombatRewardPopup#resolveFirstAvailableDrawable} for skin-fallback
 * background so the popup is robust against missing skin drawables (Sprint 10 B3 fix).</p>
 */
public class RewardCardPickerPopup extends Table {

    /** Color tint per rarity. */
    private static final Color COLOR_COMMON = Color.WHITE;
    private static final Color COLOR_RARE = Color.CYAN;
    private static final Color COLOR_LEGENDARY = Color.GOLD;
    private static final Color COLOR_UNKNOWN = Color.LIGHT_GRAY;

    private final Consumer<ItemData> onPick;
    private boolean picked = false;

    /**
     * @param skin   libGDX skin (skin-fallback applied internally)
     * @param cards  up to 3 items to display (may be empty — shows "No rewards" message)
     * @param onPick callback invoked with the chosen item when player clicks a card;
     *               also invoked with {@code null} if cards is empty and player dismisses
     */
    public RewardCardPickerPopup(Skin skin, List<ItemData> cards, Consumer<ItemData> onPick) {
        this.onPick = onPick;

        Drawable bg = CombatRewardPopup.resolveFirstAvailableDrawable(skin, "tooltip", "window", "list");
        if (bg != null) setBackground(bg);
        pad(28);

        Label title = new Label(I18n.t("reward.card.title"), skin);
        title.setColor(Color.GOLD);
        add(title).colspan(cards.isEmpty() ? 1 : cards.size()).pad(8).row();

        if (cards.isEmpty()) {
            Label empty = new Label(I18n.t("reward.card.empty"), skin);
            empty.setColor(Color.GRAY);
            add(empty).pad(12).row();

            TextButton dismiss = new TextButton(I18n.t("combat.reward.continue"), skin);
            dismiss.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    pick(null);
                }
            });
            add(dismiss).pad(12).width(200).height(50);
        } else {
            // One card button per item, side-by-side.
            for (ItemData item : cards) {
                TextButton card = buildCardButton(skin, item);
                add(card).pad(12).width(260).height(120);
            }
        }

        pack();
    }

    private TextButton buildCardButton(Skin skin, ItemData item) {
        // Build label text: name + rarity + short description
        String label = item.nameVn()
                + "\n[" + rarityLabel(item.rarity()) + "]"
                + (item.descriptionVn() != null && !item.descriptionVn().isBlank()
                        ? "\n" + truncate(item.descriptionVn(), 40) : "");
        TextButton btn = new TextButton(label, skin);
        btn.getLabel().setColor(rarityColor(item.rarity()));
        btn.getLabel().setWrap(true);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pick(item);
            }
        });
        return btn;
    }

    private void pick(ItemData item) {
        if (picked) return;
        picked = true;
        remove(); // detach from stage
        if (onPick != null) onPick.accept(item);
    }

    /** For tests: check whether a card has been picked and popup dismissed. */
    public boolean isPicked() {
        return picked;
    }

    private static Color rarityColor(String rarity) {
        if (rarity == null) return COLOR_UNKNOWN;
        return switch (rarity.toLowerCase()) {
            case "common" -> COLOR_COMMON;
            case "rare" -> COLOR_RARE;
            case "legendary" -> COLOR_LEGENDARY;
            default -> COLOR_UNKNOWN;
        };
    }

    private static String rarityLabel(String rarity) {
        if (rarity == null || rarity.isBlank()) return "?";
        return switch (rarity.toLowerCase()) {
            case "common" -> I18n.t("reward.card.rarity.common");
            case "rare" -> I18n.t("reward.card.rarity.rare");
            case "legendary" -> I18n.t("reward.card.rarity.legendary");
            default -> rarity;
        };
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }
}
