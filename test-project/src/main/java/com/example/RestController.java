package com.example;

import framework.annotations.ApiRest;
import framework.annotations.Controller;
import framework.annotations.Get;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller("/api")
public class RestController {

    @Get("/ping")
    @ApiRest
    public String ping() {
        return "pong";
    }

    @Get("/profile")
    @ApiRest
    public Map<String, Object> profile() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "APIrest");
        response.put("active", true);
        response.put("count", 3);
        response.put("items", java.util.List.of("alpha", "beta", "gamma"));
        return response;
    }
}