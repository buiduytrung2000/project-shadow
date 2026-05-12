package com.trungbui.projectshadow.screens;

import com.trungbui.projectshadow.stage.NodeType;
import com.trungbui.projectshadow.ui.SkinLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 13 B3 — verify that every NodeType maps to a valid SkinLoader drawable constant.
 * Pure data-level test; no Gdx or Skin required.
 */
class StageMapScreenNodeIconTest {

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void drawableForNodeType_returnsNonNullForAllTypes(NodeType type) {
        String drawable = StageMapScreen.drawableForNodeType(type);
        assertThat(drawable).isNotNull().isNotBlank();
    }

    @Test
    void combat_mapsTo_nodeCombat() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.COMBAT))
                .isEqualTo(SkinLoader.NODE_COMBAT);
    }

    @Test
    void elite_mapsTo_nodeElite() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.ELITE))
                .isEqualTo(SkinLoader.NODE_ELITE);
    }

    @Test
    void miniboss_mapsTo_nodeMiniboss() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.MINIBOSS))
                .isEqualTo(SkinLoader.NODE_MINIBOSS);
    }

    @Test
    void boss_mapsTo_nodeBoss() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.BOSS))
                .isEqualTo(SkinLoader.NODE_BOSS);
    }

    @Test
    void event_mapsTo_nodeEvent() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.EVENT))
                .isEqualTo(SkinLoader.NODE_EVENT);
    }

    @Test
    void rest_mapsTo_nodeRest() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.REST))
                .isEqualTo(SkinLoader.NODE_REST);
    }

    @Test
    void reward_mapsTo_nodeReward() {
        assertThat(StageMapScreen.drawableForNodeType(NodeType.REWARD))
                .isEqualTo(SkinLoader.NODE_REWARD);
    }

    @Test
    void allDrawableConstants_areDistinct() {
        // Sanity: each NodeType maps to a different drawable key.
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (NodeType t : NodeType.values()) {
            keys.add(StageMapScreen.drawableForNodeType(t));
        }
        assertThat(keys).hasSize(NodeType.values().length);
    }
}
