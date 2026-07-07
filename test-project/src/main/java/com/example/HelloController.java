package com.example;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller("/demo")
public class HelloController {

    @Get("/hello")
    public String hello() {
        return "<h1>Hello from HelloController</h1>";
    }
    
}
