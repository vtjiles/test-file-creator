package com.cmc.testing.filetester;


import java.util.Collections;
import java.util.List;

public class FileTransformException extends RuntimeException {

    private final List<String> errors;

    public FileTransformException(List<String> errors) {
        this.errors = errors;
    }

    public FileTransformException(Exception e) {
        this.errors = Collections.singletonList(e.getMessage());
    }

    public List<String> getErrors() {
        return errors;
    }
}
