package framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingRegistry {

    private final Map<String, UrlMapping> mappings = new HashMap<>();

    public void register(UrlMapping mapping) {
        String key = key(mapping.getHttpMethod(), mapping.getUrl());
        mappings.put(key, mapping);
    }

    public UrlMapping find(String httpMethod, String url) {
        return mappings.get(key(httpMethod, url));
    }

    public List<UrlMapping> getAllMappings() {
        List<UrlMapping> list = new ArrayList<>(mappings.values());
        list.sort((a, b) -> {
            int cmp = a.getUrl().compareTo(b.getUrl());
            return cmp != 0 ? cmp : a.getHttpMethod().compareTo(b.getHttpMethod());
        });
        return Collections.unmodifiableList(list);
    }

    private static String key(String httpMethod, String url) {
        return httpMethod.toUpperCase() + ":" + url;
    }
}
