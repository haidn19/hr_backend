package com.example.demo.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Converter
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public String convertToDatabaseColumn(YearMonth month) {
        return month == null ? null : month.format(FORMATTER);
    }

    @Override
    public YearMonth convertToEntityAttribute(String value) {
        return value == null || value.isBlank() ? null : YearMonth.parse(value, FORMATTER);
    }
}
