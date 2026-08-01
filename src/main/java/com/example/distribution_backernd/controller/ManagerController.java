package com.example.distribution_backernd.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ManagerController {
    @Value("${GOOGLE_MAPS_KEY}")
    private String googleMapsKey;

    @GetMapping("/manager")
    public String getAdminDashboard(Model model) {
        model.addAttribute("googleMapsKey", googleMapsKey);

        return "manager";
    }
}
