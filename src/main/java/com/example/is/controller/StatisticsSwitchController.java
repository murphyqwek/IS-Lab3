package com.example.is.controller;

import com.example.is.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics-switch")
public class StatisticsSwitchController {
    private final StatisticsService statisticsService;
    public StatisticsSwitchController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public boolean getStatistics() {
        return statisticsService.isOn();
    }

    @PostMapping
    public void setStatistics(@RequestParam boolean value) {
        statisticsService.setIsOn(value);
    }
}
