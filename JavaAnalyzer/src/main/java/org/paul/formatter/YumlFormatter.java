package org.paul.formatter;

import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;
import org.paul.model.Relationship;

import java.util.List;
import java.util.stream.Collectors;

public class YumlFormatter implements UmlFormatter {

    @Override
    public String format(List<ClassInfo> classes, DecompileConfig config) {
        StringBuilder sb = new StringBuilder();

        for (ClassInfo info : classes) {
            String name = config.fullyQualified() ? info.clazz().getName() : info.clazz().getSimpleName();

            String fields = "";
            if (config.showAttributes()) {
                fields = info.fields().stream()
                        .map(f -> f.accessModifier() + " " + f.name() + ":" + f.typeName())
                        .collect(Collectors.joining(";"));
            }
            String methods = "";
            if (config.showMethods()) {
                methods = String.join(";", info.methods());
            }
            sb.append("[").append(name).append("|")
              .append(fields).append("|")
              .append(methods).append("]\n");

            for (Relationship rel : info.relationships()) {
                String line = switch (rel) {
                    case Relationship.Extends r ->
                            "[" + r.targetName() + "]^-[" + name + "]";
                    case Relationship.Implements r ->
                            "[" + r.targetName() + "]^-.-[" + name + "]";
                    case Relationship.Association r ->
                            "[" + name + "]->[" + r.targetName() + "]";
                    case Relationship.Dependency r ->
                            "[" + name + "]->[" + r.targetName() + "]";
                };
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }
}
