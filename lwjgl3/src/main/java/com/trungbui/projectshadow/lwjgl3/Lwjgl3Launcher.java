package com.trungbui.projectshadow.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.trungbui.projectshadow.ProjectShadowGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new ProjectShadowGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("Project Shadow");

        // ─── Pixel HD viewport ───
        config.setWindowedMode(1920, 1080);                 // Native 1920x1080
        config.setResizable(true);                          // Cho phép resize
        config.setWindowSizeLimits(1280, 720, -1, -1);      // Min 1280x720
        config.useVsync(true);                              // Vsync ON cho smooth
        config.setForegroundFPS(60);                        // Cap 60 FPS

        // ─── Window icons (sau này thêm) ───
        // config.setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png");

        return config;
    }
}
