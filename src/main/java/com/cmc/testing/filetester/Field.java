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

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
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
        return name.equals(field.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
