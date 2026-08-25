package com.apprh.backend.holidays.api;

import com.apprh.backend.holidays.application.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<HolidayResponse> list(@RequestParam(name = "year", required = false) Integer year) {
        return holidayService.forYear(year != null ? year : LocalDate.now().getYear());
    }
}
