package com.cmc.testing.filetester;

import java.util.ArrayList;
import java.util.List;

public class ExportOptions {
    private final boolean delimited;
    private final String delimiter;
    private final List<Field> fields = new ArrayList<Field>();

    public ExportOptions(boolean delimited, String delimiter) {
        this.delimited = delimited;
        this.delimiter = "TAB".equals(delimiter) ? "\t" : delimiter;
    }

    public boolean isDelimited() {
        return delimited;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void addFields(List<Field> fields) {
        this.fields.addAll(fields);
    }
}
