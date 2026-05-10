package com.trungbui.projectshadow.stage;

public record DropEntry(
        String type,
        String itemId,
        String category,
        Integer valueMin,
        Integer valueMax
) {
    public static DropEntry gold(int min, int max) {
        return new DropEntry("gold", null, null, min, max);
    }

    public static DropEntry item(String itemId) {
        return new DropEntry("item", itemId, null, null, null);
    }

    public static DropEntry itemRandom(String category) {
        return new DropEntry("item_random", null, category, null, null);
    }
}
