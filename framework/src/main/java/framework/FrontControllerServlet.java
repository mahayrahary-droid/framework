package framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

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
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            processResult(result, req, resp);
        } catch (ReflectiveOperationException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Throwable cause = e instanceof InvocationTargetException ite ? ite.getCause() : e;
            resp.getWriter().write("500 Internal Server Error: " + cause.getMessage());
        } catch (ServletException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("500 Servlet Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("400 Bad Request: " + e.getMessage());
        }
    }

    private Object invoke(UrlMapping mapping, HttpServletRequest req, HttpServletResponse resp)
            throws ReflectiveOperationException {
        Object controller = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Method method = mapping.getMethod();
        Object[] args = resolveArguments(method, req);
        return method.invoke(controller, args);
    }

    private void processResult(Object result, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (result == null) {
            return;
        }

        if (result instanceof ModelView mv) {
            String viewUrl = mv.getUrl();
            if (viewUrl == null || viewUrl.isBlank()) {
                throw new ServletException("ModelView has no view URL defined");
            }
            Map<String, Object> data = mv.getData();
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewUrl);
            if (dispatcher == null) {
                throw new ServletException("No RequestDispatcher found for view: " + viewUrl);
            }
            dispatcher.forward(req, resp);
        } else {
            resp.getWriter().write(result.toString());
        }
    }

    private Object[] resolveArguments(Method method, HttpServletRequest req) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (HttpServletRequest.class.isAssignableFrom(type)) {
                args[i] = req;
            } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                args[i] = null;
            } else if (MappingRegistry.class.isAssignableFrom(type)) {
                args[i] = registry;
            } else if (isSimpleType(type)) {
                String paramName = resolveParamName(parameters[i]);
                String paramValue = req.getParameter(paramName);
                args[i] = convertSimpleType(paramValue, type, paramName);
            } else {
                throw new IllegalArgumentException("Unsupported parameter type: " + type.getName()
                        + " for parameter '" + parameters[i].getName() + "'");
            }
        }

        return args;
    }

    private String resolveParamName(Parameter parameter) {
        if (parameter.isAnnotationPresent(framework.annotations.Param.class)) {
            return parameter.getAnnotation(framework.annotations.Param.class).value();
        }
        return parameter.getName();
    }

    private boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class;
    }

    private Object convertSimpleType(String value, Class<?> type, String paramName) {
        if (type == String.class) {
            return value;
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + paramName);
        }
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type.getSimpleName()
                    + " for parameter '" + paramName + "'", e);
        }
        throw new IllegalArgumentException("Unsupported simple type: " + type.getName());
    }
}
