package framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private final String id;
    private final SessionManager manager;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private volatile long lastAccessedAt;

    Session(String id, SessionManager manager) {
        this.id = id;
        this.manager = manager;
        touch();
    }

    public String getId() {
        return id;
    }

    public Object get(String name) {
        touch();
        return attributes.get(name);
    }

    public <T> T get(String name, Class<T> type) {
        Object value = get(name);
        return value == null ? null : type.cast(value);
    }

    public void set(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Session attribute name cannot be blank");
        }
        if (value == null) {
            remove(name);
            return;
        }
        attributes.put(name, value);
        touch();
    }

    public Object remove(String name) {
        touch();
        return attributes.remove(name);
    }

    public void clear() {
        attributes.clear();
        touch();
    }

    public void invalidate() {
        manager.remove(this);
        attributes.clear();
    }

    long getLastAccessedAt() {
        return lastAccessedAt;
    }

    private void touch() {
        lastAccessedAt = System.currentTimeMillis();
    }
}