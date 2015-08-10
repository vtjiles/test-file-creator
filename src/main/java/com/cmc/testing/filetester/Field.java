package com.cmc.testing.filetester;

public class Field {
    private final String name;
    private final int length;

    public Field(final String name, final int length) {
        this.name = name;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }
}
