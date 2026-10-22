package com.monsterhp;

public class NpcRule {
    public static enum DisplayType {
        DEFAULT,
        NUMERIC,
        PERCENTAGE
    }

    public final String namePattern;
    public final DisplayType displayType;
    public final Integer threshold;

    public NpcRule(String namePattern, DisplayType displayType, Integer threshold) {
        this.namePattern = namePattern;
        this.displayType = displayType;
        this.threshold = threshold;
    }
}