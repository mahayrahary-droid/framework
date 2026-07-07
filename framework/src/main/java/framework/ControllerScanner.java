package framework;

import framework.annotations.Controller;
import framework.annotations.Delete;
import framework.annotations.Get;
import framework.annotations.Post;
import framework.annotations.Put;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

public class ControllerScanner {

    public static MappingRegistry scan(String packageName, ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        MappingRegistry registry = new MappingRegistry();
        Set<Class<?>> classes = ClasspathScanner.findClasses(packageName, classLoader);

        for (Class<?> clazz : classes) {
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }

            String basePath = normalizePath(clazz.getAnnotation(Controller.class).value());
            registerMethods(registry, clazz, basePath);
        }

        return registry;
    }

    private static void registerMethods(MappingRegistry registry, Class<?> clazz, String basePath) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(framework.annotations.UrlMapping.class)) {
                framework.annotations.UrlMapping urlMapping = method.getAnnotation(framework.annotations.UrlMapping.class);
                String path = combinePaths(basePath, urlMapping.value());
                registry.register(new UrlMapping(path, "GET", clazz, method, framework.annotations.UrlMapping.class.getName()));
            }
            registerIfPresent(registry, clazz, method, basePath, Get.class, "GET");
            registerIfPresent(registry, clazz, method, basePath, Post.class, "POST");
            registerIfPresent(registry, clazz, method, basePath, Put.class, "PUT");
            registerIfPresent(registry, clazz, method, basePath, Delete.class, "DELETE");
        }
    }

    private static <A extends Annotation> void registerIfPresent(
            MappingRegistry registry,
            Class<?> clazz,
            Method method,
            String basePath,
            Class<A> annotationType,
            String httpMethod) {
        if (!method.isAnnotationPresent(annotationType)) {
            return;
        }

        A annotation = method.getAnnotation(annotationType);
        String path = combinePaths(basePath, readPath(annotation));
        String annotationName = annotation.annotationType().getName();

        registry.register(new UrlMapping(path, httpMethod, clazz, method, annotationName));
    }

    private static String readPath(Annotation annotation) {
        try {
            return (String) annotation.annotationType().getMethod("value").invoke(annotation);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Annotation must define a value() method", e);
        }
    }

    static String combinePaths(String basePath, String methodPath) {
        String combined = normalizePath(basePath) + "/" + normalizePath(methodPath);
        if (combined.equals("/")) {
            return "/";
        }
        return combined.replaceAll("/+", "/").replaceAll("/$", "");
    }

    static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.replaceAll("/+", "/").replaceAll("/$", "");
    }
}
