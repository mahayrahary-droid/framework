package com.example;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller
public class DuplicateTestController {

    @Get("/duplicate")
    public String first() {
        return "<h1>First</h1>";
    }

    @Get("/duplicate")
    public String second() {
        return "<h1>Second</h1>";
    }
}
