package com.trungbui.projectshadow.domain;

public enum Position {
    POS_1, POS_2, POS_3, POS_4;

    public int rank() {
        return ordinal() + 1;
    }

    public boolean isFront() {
        return this == POS_1 || this == POS_2;
    }

    public boolean isBack() {
        return this == POS_3 || this == POS_4;
    }

    public static Position ofRank(int rank) {
        if (rank < 1 || rank > 4) {
            throw new IllegalArgumentException("Position rank must be 1..4, got " + rank);
        }
        return values()[rank - 1];
    }
}
