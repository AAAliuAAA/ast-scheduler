package com.ast.scheduler.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "dashboard";
    }

    @GetMapping("/agents")
    public String agents() {
        return "agents";
    }


}