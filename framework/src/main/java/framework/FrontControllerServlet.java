package framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import framework.annotations.Authorize;

@MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    public static final String MAPPING_REGISTRY_ATTR = "mappingRegistry";
    public static final String SESSION_MANAGER_ATTR = "sessionManager";
    private static final long DEFAULT_SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private MappingRegistry registry;
    private SessionManager sessionManager;

    @Override
    public void init() throws ServletException {
        String packageName = getInitParameter("controller-package");
        if (packageName == null || packageName.isBlank()) {
            throw new ServletException("Missing init-param: controller-package");
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            registry = ControllerScanner.scan(packageName, classLoader);
            getServletContext().setAttribute(MAPPING_REGISTRY_ATTR, registry);
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
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
        path = normalizeRequestPath(path);

        if (registry == null) {
            throw new ServletException("Controller mappings are not initialized");
        }

        UrlMapping mapping = registry.find(req.getMethod(), path);
        resp.setContentType("text/html; charset=UTF-8");

        if (mapping == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("404 Not Found: " + path);
            return;
        }

        if (!isAuthorized(mapping, req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("403 Forbidden");
            return;
        }

        try {
            Object result = invoke(mapping, req, resp);
            processResult(mapping, result, req, resp);
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
            throws ReflectiveOperationException, IOException, ServletException {
        Object controller = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Method method = mapping.getMethod();
        Object[] args = resolveArguments(method, req, resp);
        return method.invoke(controller, args);
    }

    private boolean isAuthorized(UrlMapping mapping, HttpServletRequest req) {
        Authorize authorization = mapping.getMethod().getAnnotation(Authorize.class);
        if (authorization == null) {
            authorization = mapping.getControllerClass().getAnnotation(Authorize.class);
        }
        if (authorization == null) {
            return true;
        }

        for (String role : authorization.value()) {
            if (role == null || role.isBlank()) {
                throw new IllegalStateException("Authorization role cannot be blank");
            }
            if (req.isUserInRole(role)) {
                return true;
            }
        }
        return false;
    }

    private void processResult(UrlMapping mapping, Object result, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (result == null) {
            return;
        }

        if (mapping.isJsonResponse()) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(JsonSerializer.toJson(result));
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

    private Object[] resolveArguments(Method method, HttpServletRequest req, HttpServletResponse resp)
            throws ReflectiveOperationException, IOException, ServletException {
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
            } else if (Part.class.isAssignableFrom(type)) {
                args[i] = resolvePart(resolveParamName(parameters[i]), req);
            } else if (type == UploadedFile.class) {
                args[i] = resolveUploadedFile(resolveParamName(parameters[i]), req);
            } else if (type == byte[].class) {
                UploadedFile file = resolveUploadedFile(resolveParamName(parameters[i]), req);
                args[i] = file.getContent();
            } else if (isSimpleType(type)) {
                String paramName = resolveParamName(parameters[i]);
                String paramValue = req.getParameter(paramName);
                args[i] = convertSimpleType(paramValue, type, paramName);
            } else {
                args[i] = bindObject(type, resolveParamName(parameters[i]), req);
            }
        }

        return args;
    }

    private Part resolvePart(String fieldName, HttpServletRequest req) throws IOException, ServletException {
        Part part = req.getPart(fieldName);
        if (part == null || part.getSubmittedFileName() == null) {
            throw new IllegalArgumentException("Missing uploaded file: " + fieldName);
        }
        return part;
    }

    private UploadedFile resolveUploadedFile(String fieldName, HttpServletRequest req)
            throws IOException, ServletException {
        Part part = resolvePart(fieldName, req);
        try (var input = part.getInputStream()) {
            return new UploadedFile(fieldName, part.getSubmittedFileName(), part.getContentType(),
                    input.readAllBytes());
        }
    }

    private Object bindObject(Class<?> type, String prefix, HttpServletRequest req)
            throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        if (!constructor.canAccess(null)) {
            constructor.setAccessible(true);
        }
        Object target = constructor.newInstance();

        for (Method setter : type.getMethods()) {
            if (!isSetter(setter)) {
                continue;
            }
            String property = decapitalize(setter.getName().substring(3));
            String parameterName = prefix == null || prefix.isBlank() ? property : prefix + "." + property;
            Object value = bindValue(setter.getGenericParameterTypes()[0], setter.getParameterTypes()[0],
                    parameterName, req);
            if (value != null) {
                setter.invoke(target, value);
            }
        }

        return target;
    }

    private Object bindValue(Type genericType, Class<?> type, String parameterName, HttpServletRequest req)
            throws ReflectiveOperationException {
        if (isSimpleType(type)) {
            String value = req.getParameter(parameterName);
            return value == null ? null : convertSimpleType(value, type, parameterName);
        }

        if (Collection.class.isAssignableFrom(type)) {
            if (!(genericType instanceof ParameterizedType parameterizedType)) {
                throw new IllegalArgumentException("Collection parameter must declare an element type: "
                        + parameterName);
            }
            Type elementType = parameterizedType.getActualTypeArguments()[0];
            if (!(elementType instanceof Class<?> elementClass)) {
                throw new IllegalArgumentException("Unsupported collection element type for parameter: "
                        + parameterName);
            }
            String[] values = req.getParameterValues(parameterName);
            if (values == null) {
                return null;
            }
            List<Object> result = new ArrayList<>();
            for (String value : values) {
                result.add(convertSimpleType(value, elementClass, parameterName));
            }
            return result;
        }

        return bindObject(type, parameterName, req);
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getName().length() > 3
                && method.getParameterCount() == 1
                && method.getReturnType() == void.class;
    }

    private String decapitalize(String value) {
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }

    private String resolveParamName(Parameter parameter) {
        if (parameter.isAnnotationPresent(framework.annotations.Param.class)) {
            return parameter.getAnnotation(framework.annotations.Param.class).value();
        }
        return parameter.getName();
    }

    private String normalizeRequestPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "/" : normalized;
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
