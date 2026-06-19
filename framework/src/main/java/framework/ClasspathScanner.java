package framework;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathScanner {

    public static Set<Class<?>> findClasses(String packageName, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                scanDirectory(new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8)), packageName, classes, classLoader);
            } else if ("jar".equals(protocol)) {
                scanJar(resource, path, classes, classLoader);
            }
        }

        return classes;
    }

    private static void scanDirectory(File directory, String packageName, Set<Class<?>> classes, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className, false, classLoader));
            }
        }
    }

    private static void scanJar(URL resource, String path, Set<Class<?>> classes, ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    classes.add(Class.forName(className, false, classLoader));
                }
            }
        }
    }
}
