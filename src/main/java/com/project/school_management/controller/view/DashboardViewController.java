package com.project.school_management.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    @GetMapping({"/", "/dashboard"})
    public String root() {
        return "redirect:/admin/dashboard";
    }
}
