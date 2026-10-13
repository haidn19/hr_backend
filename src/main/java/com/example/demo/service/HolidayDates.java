package com.example.demo.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

final class HolidayDates {
    private HolidayDates() {}
    static Set<LocalDate> parse(String dates) {
        if (dates == null || dates.isBlank()) return Set.of();
        return Arrays.stream(dates.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(LocalDate::parse).collect(Collectors.toUnmodifiableSet());
    }
}
