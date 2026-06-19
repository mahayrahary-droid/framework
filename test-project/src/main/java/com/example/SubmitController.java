package com.example;

import framework.annotations.Controller;
import framework.annotations.Post;

@Controller("/demo")
public class SubmitController {

    @Post("/submit")
    public String submit() {
        return "<h1>Form submitted</h1>";
    }
}
