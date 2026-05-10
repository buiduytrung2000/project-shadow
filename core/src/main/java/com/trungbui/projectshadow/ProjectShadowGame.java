package com.trungbui.projectshadow;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.trungbui.projectshadow.data.GameData;
import com.trungbui.projectshadow.screens.CombatScreen;

import java.nio.file.Path;

public class ProjectShadowGame extends Game {

    public static final int VIRTUAL_WIDTH = 1920;
    public static final int VIRTUAL_HEIGHT = 1080;

    private GameData gameData;

    @Override
    public void create() {
        gameData = loadGameData();
        setScreen(new CombatScreen(gameData));
    }

    private static GameData loadGameData() {
        try {
            Path explicitDir = Path.of("assets", "data");
            if (java.nio.file.Files.isDirectory(explicitDir)) {
                return GameData.loadFromDirectory(explicitDir);
            }
            Path internal = Path.of(Gdx.files.internal("data").file().getAbsolutePath());
            return GameData.loadFromDirectory(internal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load game data", e);
        }
    }

    public GameData gameData() {
        return gameData;
    }
}
