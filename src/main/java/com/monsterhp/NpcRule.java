package com.monsterhp;

public class NpcRule {
    public final String namePattern;
    public final boolean numeric;
    public final Integer threshold;

    public NpcRule(String namePattern, boolean numeric, Integer threshold) {
        this.namePattern = namePattern;
        this.numeric = numeric;
        this.threshold = threshold;
    }
}