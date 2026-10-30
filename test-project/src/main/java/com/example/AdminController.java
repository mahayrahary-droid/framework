package com.example;

import framework.annotations.Authorize;
import framework.annotations.Controller;
import framework.annotations.Get;

@Controller("/admin")
public class AdminController {

    @Get("/dashboard")
    @Authorize("ADMIN")
    public String dashboard() {
        return "<h1>Admin dashboard</h1>";
    }
}