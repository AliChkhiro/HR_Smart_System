package com.apprh.backend.holidays.application;

import com.apprh.backend.holidays.api.HolidayResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HolidayService {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    private record FixedHoliday(int month, int day, String name) {
    }

    private static final List<FixedHoliday> FIXED_HOLIDAYS = List.of(
            new FixedHoliday(1, 1, "Nouvel an"),
            new FixedHoliday(1, 11, "Manifeste de l'Indépendance"),
            new FixedHoliday(5, 1, "Fête du Travail"),
            new FixedHoliday(7, 30, "Fête du Trône"),
            new FixedHoliday(8, 14, "Allégeance Oued Eddahab"),
            new FixedHoliday(8, 20, "Révolution du Roi et du Peuple"),
            new FixedHoliday(8, 21, "Fête de la Jeunesse"),
            new FixedHoliday(11, 6, "Marche Verte"),
            new FixedHoliday(11, 18, "Fête de l'Indépendance"));

    public List<HolidayResponse> forYear(int year) {
        int safeYear = Math.min(Math.max(year, MIN_YEAR), MAX_YEAR);
        List<HolidayResponse> holidays = new ArrayList<>();
        LocalDate start = LocalDate.of(safeYear, 1, 1);
        LocalDate end = LocalDate.of(safeYear, 12, 31);

        for (FixedHoliday fixed : FIXED_HOLIDAYS) {
            holidays.add(new HolidayResponse(LocalDate.of(safeYear, fixed.month(), fixed.day()),
                    fixed.name(), HolidayResponse.HolidayType.NATIONAL));
        }
        holidays.addAll(religiousHolidays(start, end));
        holidays.sort(Comparator.comparing(HolidayResponse::date));
        return holidays;
    }

    private List<HolidayResponse> religiousHolidays(LocalDate start, LocalDate end) {
        HijrahChronology hijrah = HijrahChronology.INSTANCE;
        List<HolidayResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            HijrahDate hijrahDate = hijrah.date(date);
            int month = hijrahDate.get(ChronoField.MONTH_OF_YEAR);
            int day = hijrahDate.get(ChronoField.DAY_OF_MONTH);
            if (month == 1 && day == 1) {
                result.add(new HolidayResponse(date, "Awal Moharram (Nouvel an hégirien)",
                        HolidayResponse.HolidayType.RELIGIOUS));
            } else if (month == 3 && day == 12) {
                result.add(new HolidayResponse(date, "Aïd al-Mawlid",
                        HolidayResponse.HolidayType.RELIGIOUS));
            } else if (month == 10 && day == 1) {
                result.add(new HolidayResponse(date, "Aïd el-Fitr",
                        HolidayResponse.HolidayType.RELIGIOUS));
            } else if (month == 12 && day == 10) {
                result.add(new HolidayResponse(date, "Aïd al-Adha",
                        HolidayResponse.HolidayType.RELIGIOUS));
            }
        }
        return result;
    }
}
