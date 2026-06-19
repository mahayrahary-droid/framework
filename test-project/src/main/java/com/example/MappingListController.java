package com.example;

import framework.MappingRegistry;
import framework.UrlMapping;
import framework.annotations.Controller;
import framework.annotations.Get;
import java.util.List;

@Controller
public class MappingListController {

    @Get("/annotations")
    public String listMappings(MappingRegistry registry) {
        List<UrlMapping> mappings = registry.getAllMappings();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        html.append("<title>Annotations</title>");
        html.append("<style>");
        html.append("body{font-family:sans-serif;margin:2rem}");
        html.append("table{border-collapse:collapse;width:100%}");
        html.append("th,td{border:1px solid #ccc;padding:.5rem 1rem;text-align:left}");
        html.append("th{background:#f5f5f5}");
        html.append("</style></head><body>");
        html.append("<h1>Table de correspondance URL → méthode</h1>");
        html.append("<table><thead><tr>");
        html.append("<th>HTTP</th><th>URL</th><th>Annotation</th><th>Full-name</th>");
        html.append("</tr></thead><tbody>");

        for (UrlMapping mapping : mappings) {
            html.append("<tr>")
                .append("<td>").append(mapping.getHttpMethod()).append("</td>")
                .append("<td>").append(mapping.getUrl()).append("</td>")
                .append("<td>").append(mapping.getAnnotationName()).append("</td>")
                .append("<td><code>").append(mapping.getFullName()).append("</code></td>")
                .append("</tr>");
        }

        html.append("</tbody></table></body></html>");
        return html.toString();
    }
}
