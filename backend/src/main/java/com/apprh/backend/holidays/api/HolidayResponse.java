package com.apprh.backend.holidays.api;

import java.time.LocalDate;

public record HolidayResponse(
        LocalDate date,
        String name,
        HolidayType type
) {

    public enum HolidayType {
        NATIONAL,
        RELIGIOUS
    }
}
