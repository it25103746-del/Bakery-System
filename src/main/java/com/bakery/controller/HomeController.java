package com.bakery.controller;

import com.bakery.service.ModuleCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ModuleCatalog moduleCatalog;

    public HomeController(ModuleCatalog moduleCatalog) {
        this.moduleCatalog = moduleCatalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("modules", moduleCatalog.findAll());
        return "index";
    }
}
