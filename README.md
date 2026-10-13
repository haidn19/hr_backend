# Gửi email

Backend gửi email qua SMTP bằng `JavaMailSender`. Mailpit đã được loại khỏi
Docker Compose.

`mail.tm` là hộp thư nhận email tạm thời, không phải máy chủ SMTP để gửi thư.
Để kiểm thử việc nhận thư:

1. Tạo một hộp thư tại [mail.tm](https://mail.tm) và giữ trang đó mở.
2. Sao chép `.env.example` thành `.env`, rồi điền thông tin SMTP hợp lệ của
   dịch vụ gửi thư (Brevo, Gmail, Microsoft 365 hoặc SMTP công ty). Không đưa
   file `.env` vào Git.
3. Điền địa chỉ mail.tm vừa tạo vào `MAIL_RECIPIENT_OVERRIDE`.
4. Khởi động backend. File `.env` được tự nạp; hoặc có thể đặt các biến môi
   trường tương đương. Ví dụ PowerShell:

   ```powershell
   $env:MAIL_HOST = 'smtp.example.com'
   $env:MAIL_PORT = '587'
   $env:MAIL_USERNAME = '...'
   $env:MAIL_PASSWORD = '...'
   $env:MAIL_FROM = 'hr@example.com'
   $env:MAIL_RECIPIENT_OVERRIDE = 'your-inbox@mail.tm'
   .\mvnw.cmd spring-boot:run
   ```

Khi có `MAIL_RECIPIENT_OVERRIDE`, **toàn bộ** email đi ra sẽ được gửi vào hộp
thư mail.tm đó. Khi để trống, báo cáo được gửi đến email lưu trong hồ sơ từng
nhân viên.

## Luồng gửi báo cáo chấm công

```text
Lịch chạy lúc 10:00 ngày 5 hằng tháng, hoặc API POST
/api/monthly-attendance-reports/send
  -> MonthlyAttendanceReportEmailService đọc dữ liệu chấm công và nghỉ phép
  -> EmailService tạo nội dung HTML
  -> SMTP provider gửi thư
  -> mail.tm (khi có MAIL_RECIPIENT_OVERRIDE), hoặc employee.email
```
