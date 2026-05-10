package com.trungbui.projectshadow.ui;

public final class VietnameseCharset {

    public static final String ALL = build();

    private static String build() {
        StringBuilder sb = new StringBuilder(512);
        for (char c = 0x0020; c <= 0x007E; c++) sb.append(c);
        for (char c = 0x00A0; c <= 0x017F; c++) sb.append(c);
        for (char c = 0x1EA0; c <= 0x1EF9; c++) sb.append(c);
        sb.append("…—–€™");
        return sb.toString();
    }

    private VietnameseCharset() {
    }
}
