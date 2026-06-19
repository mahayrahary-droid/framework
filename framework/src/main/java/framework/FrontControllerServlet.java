package framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class FrontControllerServlet extends HttpServlet {

    public static final String MAPPING_REGISTRY_ATTR = "mappingRegistry";

    private MappingRegistry registry;

    @Override
    public void init() throws ServletException {
        String packageName = getInitParameter("controller-package");
        if (packageName == null || packageName.isBlank()) {
            throw new ServletException("Missing init-param: controller-package");
        }

        try {
            registry = ControllerScanner.scan(packageName, getClass().getClassLoader());
            getServletContext().setAttribute(MAPPING_REGISTRY_ATTR, registry);
        } catch (IOException | ClassNotFoundException e) {
            throw new ServletException("Failed to scan controllers in package: " + packageName, e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        if (path.isEmpty()) {
            path = "/";
        }

        UrlMapping mapping = registry.find(req.getMethod(), path);
        resp.setContentType("text/html; charset=UTF-8");

        if (mapping == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("404 Not Found: " + path);
            return;
        }

        try {
            Object result = invoke(mapping, req, resp);
            if (result != null) {
                resp.getWriter().write(result.toString());
            }
        } catch (ReflectiveOperationException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Throwable cause = e instanceof InvocationTargetException ite ? ite.getCause() : e;
            resp.getWriter().write("500 Internal Server Error: " + cause.getMessage());
        }
    }

    private Object invoke(UrlMapping mapping, HttpServletRequest req, HttpServletResponse resp)
            throws ReflectiveOperationException {
        Object controller = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Method method = mapping.getMethod();
        Object[] args = resolveArguments(method, req, resp);
        return method.invoke(controller, args);
    }

    private Object[] resolveArguments(Method method, HttpServletRequest req, HttpServletResponse resp) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (HttpServletRequest.class.isAssignableFrom(type)) {
                args[i] = req;
            } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                args[i] = resp;
            } else if (MappingRegistry.class.isAssignableFrom(type)) {
                args[i] = registry;
            } else {
                throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
            }
        }

        return args;
    }
}
