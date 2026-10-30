package framework;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public static final String COOKIE_NAME = "FRAMEWORK_SESSION";

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long maxInactiveIntervalMillis;

    public SessionManager(long maxInactiveIntervalMillis) {
        if (maxInactiveIntervalMillis <= 0) {
            throw new IllegalArgumentException("Session timeout must be positive");
        }
        this.maxInactiveIntervalMillis = maxInactiveIntervalMillis;
    }

    public Session getOrCreate(HttpServletRequest request, HttpServletResponse response) {
        removeExpiredSessions();
        String sessionId = readSessionId(request);
        Session session = sessionId == null ? null : sessions.get(sessionId);
        if (session == null) {
            sessionId = UUID.randomUUID().toString();
            session = new Session(sessionId, this);
            sessions.put(sessionId, session);
            Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
            cookie.setHttpOnly(true);
            cookie.setPath(resolveCookiePath(request));
            response.addCookie(cookie);
        }
        return session;
    }

    public void invalidate(Session session, HttpServletResponse response, HttpServletRequest request) {
        if (session == null) {
            return;
        }
        sessions.remove(session.getId());
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath(resolveCookiePath(request));
        response.addCookie(cookie);
    }

    int size() {
        return sessions.size();
    }

    void remove(Session session) {
        sessions.remove(session.getId(), session);
    }

    private String readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void removeExpiredSessions() {
        long expirationTime = System.currentTimeMillis() - maxInactiveIntervalMillis;
        sessions.entrySet().removeIf(entry -> entry.getValue().getLastAccessedAt() < expirationTime);
    }

    private String resolveCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isBlank() ? "/" : contextPath;
    }
}