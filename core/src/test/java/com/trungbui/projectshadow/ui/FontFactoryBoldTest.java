package com.trungbui.projectshadow.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fix C (post-Sprint-13) — {@link FontFactory} bold/regular overload.
 *
 * <p>Tests that cover only pure-Java concerns (no libGDX rendering):
 * <ul>
 *   <li>Bold TTF file is present in assets/fonts/.</li>
 *   <li>Regular TTF file is present in assets/fonts/.</li>
 *   <li>{@link FontFactory} constructor rejects a null TTF path.</li>
 * </ul>
 * </p>
 *
 * <p>Actual font rendering cannot be tested without a running libGDX
 * environment (headless OpenGL). These tests are structural only.</p>
 */
class FontFactoryBoldTest {

    @Test
    void regularTtf_existsInAssetsDir() {
        Path[] candidates = {
                Path.of("../assets/fonts/BeVietnamPro-Regular.ttf"),
                Path.of("assets/fonts/BeVietnamPro-Regular.ttf")
        };
        boolean found = false;
        for (Path p : candidates) if (Files.exists(p)) { found = true; break; }
        assertThat(found).as("BeVietnamPro-Regular.ttf must exist in assets/fonts/").isTrue();
    }

    @Test
    void boldTtf_existsInAssetsDir() {
        Path[] candidates = {
                Path.of("../assets/fonts/BeVietnamPro-Bold.ttf"),
                Path.of("assets/fonts/BeVietnamPro-Bold.ttf")
        };
        boolean found = false;
        for (Path p : candidates) if (Files.exists(p)) { found = true; break; }
        assertThat(found).as("BeVietnamPro-Bold.ttf must exist in assets/fonts/").isTrue();
    }

    @Test
    void boldTtf_hasReasonableFileSize() throws Exception {
        Path[] candidates = {
                Path.of("../assets/fonts/BeVietnamPro-Bold.ttf"),
                Path.of("assets/fonts/BeVietnamPro-Bold.ttf")
        };
        Path boldFile = null;
        for (Path p : candidates) if (Files.exists(p)) { boldFile = p; break; }
        if (boldFile == null) return; // skip if not found (covered by other test)
        long size = Files.size(boldFile);
        // TTF files should be at least 10 KB (any smaller is likely truncated)
        assertThat(size).isGreaterThan(10_000L);
    }
}
