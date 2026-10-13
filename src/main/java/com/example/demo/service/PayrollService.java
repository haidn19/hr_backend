package com.example.demo.service;

import com.example.demo.dto.response.PayrollResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.entity.Payroll;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;
import com.example.demo.repository.PayrollRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {
    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);
    private static final long STANDARD_WORK_MINUTES = 420;
    private static final int STANDARD_WORKING_DAYS = 22;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRepository payrollRepository;
    private final LeaveRepository leaveRepository;
    private final Set<LocalDate> holidays;

    public PayrollService(EmployeeRepository employeeRepository, AttendanceRepository attendanceRepository,
            PayrollRepository payrollRepository, LeaveRepository leaveRepository,
            @Value("${app.holidays:}") String holidayDates) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollRepository = payrollRepository;
        this.leaveRepository = leaveRepository;
        this.holidays = HolidayDates.parse(holidayDates);
    }

    @Transactional
    public PayrollResponse calculateCurrentPayroll(Long employeeId, LocalDate toDate) {
        // API hiện tại tính lại từ đầu tháng tới toDate và lưu kết quả vào payroll.
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (employee.getBaseSalary() == null) {
            throw new IllegalStateException("Employee base salary is not configured");
        }
        LocalDate fromDate = toDate.withDayOfMonth(1);
        List<Attendance> days = attendanceRepository.findByEmployeeIdAndWorkDateBetween(employeeId, fromDate, toDate)
                .stream().filter(day -> !"LEAVE".equals(day.getStatus()))
                .filter(day -> day.getCheckIn() != null)
                .map(day -> AttendanceService.calculate(day, holidays, shouldClose(day.getWorkDate())))
                .filter(day -> day.getEffectiveOut() != null).toList();
        attendanceRepository.saveAll(days);

        long paidLeaveDays = leaveRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId, "APPROVED", toDate, fromDate)
                .stream()
                .mapToLong(leave -> paidLeaveDaysWithin(leave, fromDate, toDate))
                .sum();

        // Chỉ mất công sáng khi IN >= 09:30. Các phút thiếu trong buổi có công được xử lý
        // bằng Bù tháng, nên không trừ chúng thêm lần nữa khỏi lương cơ bản.
        long payableMinutes = days.stream().mapToLong(day ->
                (day.getMorningMinutes() > 0 ? 210 : 0) +
                (day.getAfternoonMinutes() > 0 ? 210 : 0)).sum()
                + paidLeaveDays * STANDARD_WORK_MINUTES;
        long overtimeMinutes = days.stream().mapToLong(Attendance::getOvertimeMinutes).sum();
        int monthlyBalance = days.stream().mapToInt(Attendance::getBalanceMinutes).sum();
        boolean deductionRequired = monthlyBalance > 60;

        // Quy đổi lương cơ bản theo công sáng/chiều; công thức khấu trừ Bù sẽ được
        // bổ sung khi có Payroll Specification riêng.
        BigDecimal minuteRate = employee.getBaseSalary()
                .divide(BigDecimal.valueOf(STANDARD_WORKING_DAYS * STANDARD_WORK_MINUTES), 8, RoundingMode.HALF_UP);
        BigDecimal salary = minuteRate.multiply(BigDecimal.valueOf(payableMinutes))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overtimePay = days.stream()
                .map(day -> minuteRate.multiply(BigDecimal.valueOf(day.getOvertimeMinutes()))
                        .multiply(BigDecimal.valueOf(day.getOvertimeMultiplier())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // SRS §17 mới xác định ngưỡng Bù > 60 phút; chưa có công thức đổi phút Bù thành tiền.
        // Khi cần khấu trừ, chưa thể xác định lương thực nhận; để null thay vì
        // lưu một số tiền có vẻ như đã quyết toán.
        BigDecimal totalSalary = deductionRequired ? null : salary.add(overtimePay);
        Payroll payroll = payrollRepository.findByEmployeeAndMonth(employee, YearMonth.from(toDate))
                .orElseGet(Payroll::new);
        payroll.setEmployee(employee);
        payroll.setMonth(YearMonth.from(toDate));
        payroll.setBaseSalary(employee.getBaseSalary());
        payroll.setOvertimeMinutes(overtimeMinutes);
        payroll.setMonthlyBalanceMinutes(monthlyBalance);
        payroll.setDeductionRequired(deductionRequired);
        BigDecimal leaveDaysUsed = BigDecimal.valueOf(paidLeaveDays);
        payroll.setLeaveDaysUsed(leaveDaysUsed);
        payroll.setGrossSalary(salary);
        payroll.setNetSalary(totalSalary);
        payrollRepository.save(payroll);

        log.info("event=payroll_calculated payrollId={} employeeId={} month={} deductionRequired={}",
                payroll.getId(), employeeId, payroll.getMonth(), deductionRequired);

        return new PayrollResponse(employeeId, fromDate, toDate, employee.getBaseSalary(),
                (double) payableMinutes / STANDARD_WORK_MINUTES, salary, overtimePay, totalSalary,
                leaveDaysUsed, monthlyBalance, deductionRequired);
    }

    private long paidLeaveDaysWithin(Leave leave, LocalDate fromDate, LocalDate toDate) {
        return leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1))
                .filter(date -> AttendanceService.isNormalDay(date, holidays))
                .filter(date -> !date.isBefore(fromDate) && !date.isAfter(toDate))
                .count();
    }

    private boolean shouldClose(LocalDate date) {
        return date.isBefore(LocalDate.now()) ||
                (date.equals(LocalDate.now()) && !LocalTime.now().isBefore(LocalTime.of(17, 30)));
    }
}
