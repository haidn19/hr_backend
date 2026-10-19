# Chạy và phát hành bằng Docker

Mỗi thành phần có Dockerfile riêng:

- `Dockerfile`: build Spring Boot bằng Maven và chạy JAR trên JRE 21.
- `../Frontend/Dockerfile`: build Next.js thành standalone server và chạy cổng 3000.

## Chuẩn bị

Chạy tại thư mục `Backend`:

```powershell
Copy-Item .env.example .env
```

Điền thông tin SMTP, database và `NEXTAUTH_SECRET` hợp lệ vào `.env`. Không commit file `.env`.

## Chạy frontend và backend

```powershell
docker compose up -d --build
docker compose ps
```

Truy cập frontend tại `http://localhost:3000` và backend tại `http://localhost:8080`. Backend dùng database được khai báo bởi `DB_URL`, `DB_USERNAME` và `DB_PASSWORD` trong `.env`.

## Chạy MySQL local

MySQL local là tùy chọn. Đặt `DB_URL=jdbc:mysql://mysql:3306/hrm_db?serverTimezone=UTC`, `DB_USERNAME=root` và `DB_PASSWORD` bằng `MYSQL_ROOT_PASSWORD` trong `.env`, sau đó chạy:

```powershell
docker compose --profile local-db up -d --build
```

## Build và đẩy image

Thay `YOUR_DOCKERHUB_USER` bằng Docker Hub username và dùng tag phiên bản mới cho mỗi lần phát hành:

```powershell
docker build --pull -t YOUR_DOCKERHUB_USER/hrm-backend:v1 .
docker build --pull --build-arg NEXT_PUBLIC_API_URL=http://backend:8080/api -t YOUR_DOCKERHUB_USER/hrm-frontend:v1 -f ../Frontend/Dockerfile ../Frontend
docker login
docker push YOUR_DOCKERHUB_USER/hrm-backend:v1
docker push YOUR_DOCKERHUB_USER/hrm-frontend:v1
```

`NEXT_PUBLIC_API_URL` là URL Next.js dùng để chuyển tiếp `/backend-api/*`; khi các container cùng mạng Docker, giữ giá trị `http://backend:8080/api`.

## Log và dừng dịch vụ

```powershell
docker compose logs -f frontend backend
docker compose down
```

`docker compose down` giữ lại volume database. Chỉ dùng `docker compose down -v` khi thực sự muốn xóa dữ liệu MySQL local.
