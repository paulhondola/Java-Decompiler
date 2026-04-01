package org.paul;

import java.util.List;

public class YumlFormatter implements UmlFormatter {

    public enum Mode { SIMPLE, CLASSES }

    private final Mode mode;

    public YumlFormatter(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String format(List<Class<?>> classes) {
        return "";
    }
}
