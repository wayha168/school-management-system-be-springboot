package com.project.school_management.controller.view;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("username", authentication != null ? authentication.getName() : "guest");
        return "dashboard";
    }
}
