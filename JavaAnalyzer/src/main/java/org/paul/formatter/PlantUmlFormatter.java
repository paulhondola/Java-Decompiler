package org.paul.formatter;

import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;
import org.paul.model.Relationship;

import java.util.List;

public class PlantUmlFormatter implements UmlFormatter {

    @Override
    public String format(List<ClassInfo> classes, DecompileConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n\n");

        for (ClassInfo info : classes) {
            String name = config.fullyQualified() ? info.clazz().getName() : info.clazz().getSimpleName();
            String keyword = info.clazz().isInterface() ? "interface" : "class";

            sb.append(keyword).append(" ").append(name).append("{\n");

            if (config.showAttributes()) {
                for (var field : info.fields()) {
                    sb.append("  ").append(field.accessModifier())
                            .append(field.name()).append(":").append(field.typeName()).append("\n");
                }
            }

            if (config.showMethods()) {
                for (String method : info.methods()) {
                    sb.append("  ").append(method).append("\n");
                }
            }

            sb.append("}\n\n");
        }

        for (ClassInfo info : classes) {
            String name = config.fullyQualified() ? info.clazz().getName() : info.clazz().getSimpleName();

            for (Relationship rel : info.relationships()) {
                String line = switch (rel) {
                    case Relationship.Extends r -> r.targetName() + " <|--- " + name;
                    case Relationship.Implements r -> r.targetName() + " <|--- " + name;
                    case Relationship.Association r -> name + " ---> " + r.targetName();
                    case Relationship.Dependency r -> name + " ---> " + r.targetName();
                };
                sb.append(line).append("\n\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }
}
