package org.paul.formatter;

import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;

import java.util.List;

public interface UmlFormatter {
    String format(List<ClassInfo> classes, DecompileConfig config);
}
