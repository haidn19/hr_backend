package com.example.demo.dto.request;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PayrollRequest(Long employeeId, YearMonth month, BigDecimal bonus, BigDecimal deduction) {}