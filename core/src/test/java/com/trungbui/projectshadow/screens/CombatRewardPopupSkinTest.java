package com.trungbui.projectshadow.screens;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the runtime crash hit on Sprint 10 B3 release:
 * {@code GdxRuntimeException: No Drawable... registered with name: default-pane}.
 * Trigger path was killing the last enemy of a combat → reward popup
 * constructor → skin.getDrawable("default-pane") throws.
 *
 * <p>libGDX's {@code Skin} needs a {@code Files} context to instantiate, so a
 * direct unit test would require HeadlessApplication. Instead we verify the
 * <strong>contract</strong> at the file level: the skin JSON must contain at
 * least one of the drawable names {@code CombatRewardPopup} falls back to,
 * and must NOT have an entry the buggy name re-appears under.</p>
 *
 * <p>This is a cheap canary — if a designer renames the drawable in
 * {@code uiskin.json}, this test fails fast instead of waiting until end-of-combat
 * for the runtime crash.</p>
 */
class CombatRewardPopupSkinTest {

    private static String readSkin() throws Exception {
        Path[] candidates = {
                Paths.get("..", "assets", "ui", "uiskin.json"),
                Paths.get("assets", "ui", "uiskin.json"),
                Paths.get("..", "..", "assets", "ui", "uiskin.json"),
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) return Files.readString(p);
        }
        throw new IllegalStateException(
                "Cannot locate assets/ui/uiskin.json from cwd=" + Paths.get("").toAbsolutePath());
    }

    @Test
    void skin_doesNotContain_default_pane_drawable() throws Exception {
        // The buggy original code called skin.getDrawable("default-pane"). If anyone
        // re-introduces a TintedDrawable / TextureRegion with that name in uiskin.json,
        // we'll want to know — but until they do, the popup's fallback should not
        // try to look it up.
        String skin = readSkin();
        // The literal name should not appear as a registered drawable key. (We only
        // grep the file as a heuristic; CombatRewardPopup itself uses defensive
        // skin.has(...) check, so even if this slips, runtime won't crash.)
        assertThat(skin).doesNotContain("default-pane:");
    }

    @Test
    void skin_contains_atLeastOneFallbackDrawable() throws Exception {
        // CombatRewardPopup tries: tooltip → window → list. At least ONE must be
        // present, else the popup renders backgroundless (still functional, but
        // visually invisible).
        String skin = readSkin();
        boolean anyPresent = skin.contains("tooltip:")
                || skin.contains("window:")
                || skin.contains("list:");
        assertThat(anyPresent)
                .as("uiskin.json must define at least one of: tooltip / window / list "
                        + "(CombatRewardPopup background fallback chain)")
                .isTrue();
    }

    @Test
    void skin_currentlyHas_tooltipDrawable() throws Exception {
        // Pin the current expected drawable. If the skin is rebuilt and tooltip is
        // removed, the popup falls back to window/list — but we want to flag the
        // change so the visual style can be re-checked.
        String skin = readSkin();
        assertThat(skin).contains("tooltip:");
    }
}
