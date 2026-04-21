package org.paul.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Opens a .jar file, loads every non-anonymous, non-synthetic class it contains,
 * and returns them in JAR-entry order.
 */
public class JarLoader {

    public static List<Class<?>> load(String jarPath) {
        List<Class<?>> classes = new ArrayList<>();
        File file = new File(jarPath);

        try (JarFile jar = new JarFile(file);
             URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()})) {

            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;

                // Skip anonymous / inner classes (compiler-generated $N names)
                if (name.contains("$")) continue;

                String className = name.replace('/', '.').replace(".class", "");

                try {
                    Class<?> clazz = loader.loadClass(className);
                    if (!clazz.isSynthetic() && !clazz.isAnonymousClass()) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    // Skip classes that cannot be resolved in this loader context
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JAR: " + jarPath, e);
        }

        return List.copyOf(classes);
    }
}
