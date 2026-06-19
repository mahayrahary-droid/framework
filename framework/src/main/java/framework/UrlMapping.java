package framework;

import java.lang.reflect.Method;

public class UrlMapping {

    private final String url;
    private final String httpMethod;
    private final Class<?> controllerClass;
    private final Method method;
    private final String annotationName;

    public UrlMapping(String url, String httpMethod, Class<?> controllerClass, Method method, String annotationName) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.method = method;
        this.annotationName = annotationName;
    }

    public String getUrl() {
        return url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getFullName() {
        return controllerClass.getName() + "." + method.getName();
    }
}
