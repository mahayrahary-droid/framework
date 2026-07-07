package com.example;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller
public class TestController {

    @Get("/Test")
    public String test() {
        return "<h1>Test Controller</h1>";
    }
}
