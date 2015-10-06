package com.cmc.testing.filetester;

public class Field {
    private final String name;
    private final int length;

    public Field(final String name) {
        this(name, 0);
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        if (name != null ? !name.equals(field.name) : field.name != null) return false;

        return true;
    }
}
