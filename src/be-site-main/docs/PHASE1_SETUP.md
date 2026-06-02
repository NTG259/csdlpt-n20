# PHASE 1 — KHỞI TẠO PROJECT (chi tiết)

> **Phạm vi:** Đưa project Spring Boot từ skeleton ban đầu lên trạng thái chạy được + kết nối SQL Server + cấu trúc package rõ ràng.
> **Mục tiêu phase:** `./gradlew bootRun` khởi động không lỗi, nối DB thành công, sẵn sàng viết entity.

---

## 0. Hiện trạng project (đã có)

- Java 21, **Spring Boot 4.x**, Gradle Kotlin DSL (`build.gradle.kts`)
- Đã có dependency: `spring-boot-starter-webmvc`, `lombok`, `mssql-jdbc`, `spring-boot-devtools`
- `SitemainApplication.java` (`@SpringBootApplication`), package gốc `csdlpt.sitemain`
- `application.properties` mới chỉ có `spring.application.name`

> ⚠️ **Lưu ý Spring Boot 4.x:** starter web tên là `spring-boot-starter-webmvc` (không phải `-web`). Giữ quy ước này; các starter jpa/security/validation dùng tên chuẩn.

---

## Task 1.1 — Bổ sung dependency
- **Mục tiêu:** Đủ thư viện cho JPA, Security, Validation, JWT.
- **Thêm vào `build.gradle.kts`:**

| Dependency | Scope | Vai trò |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-data-jpa` | implementation | ORM/Hibernate + repository |
| `org.springframework.boot:spring-boot-starter-security` | implementation | bảo mật + PasswordEncoder |
| `org.springframework.boot:spring-boot-starter-validation` | implementation | `@Valid`, Jakarta Bean Validation |
| `io.jsonwebtoken:jjwt-api:0.12.x` | implementation | JWT API |
| `io.jsonwebtoken:jjwt-impl:0.12.x` | runtimeOnly | JWT impl |
| `io.jsonwebtoken:jjwt-jackson:0.12.x` | runtimeOnly | JWT ↔ JSON |

- **File:** `build.gradle.kts`
- **Kiểm chứng:** `./gradlew dependencies` hoặc reload Gradle → tải về không lỗi.

---

## Task 1.2 — Cấu trúc package
- **Quy ước gốc:** `csdlpt.sitemain`
- **Cây thư mục:**
```
csdlpt/sitemain
├── SitemainApplication.java
├── config/         # SecurityConfig, (CorsConfig, JacksonConfig nếu cần)
├── common/         # ApiResponse, ErrorResponse, PageResponse
├── exception/      # GlobalExceptionHandler + custom exception
├── security/       # JwtService, JwtAuthenticationFilter, CustomUserDetails(Service)
├── domain/
│   ├── entity/     # @Entity
│   ├── enums/      # VaiTro, ...
│   └── converter/  # BooleanToTinyIntConverter
├── repository/
├── dto/
│   ├── request/
│   ├── response/
│   └── projection/ # ProductListItemView
├── service/
│   └── impl/
├── controller/
└── util/
```
- **Quy ước tên:** entity tiếng Việt không dấu (`SanPhamCore`); `@Table/@Column` giữ đúng tên DDL; service/controller/DTO tiếng Anh (`ProductService`, `AuthController`).

---

## Task 1.3 — `application.yml` + kết nối SQL Server
- **Việc cần làm:** đổi `application.properties` → `application.yml`; tách `application-dev.yml`; `ddl-auto: validate` (DB có sẵn).
- **Khung cấu hình:**
```yaml
spring:
  application: { name: sitemain }
  profiles: { active: dev }
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=SiteMain;encrypt=true;trustServerCertificate=true
    username: ${DB_USER:sa}
    password: ${DB_PASS:}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
    hibernate: { ddl-auto: validate }   # KHÔNG để Hibernate sửa schema; validate giúp bắt lỗi mapping sớm
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        jdbc: { time_zone: Asia/Ho_Chi_Minh }
  jackson:
    serialization: { write-dates-as-timestamps: false }   # date ISO-8601
jwt:
  secret: ${JWT_SECRET:doi-thanh-chuoi-bi-mat-toi-thieu-32-ky-tu}
  expiration-ms: 31536000000
server:
  port: 8080
```
- **Lưu ý:**
  - `ddl-auto: validate` (an toàn hơn `none`: vẫn không sửa schema nhưng kiểm tra entity ↔ bảng khi khởi động).
  - Mật khẩu/secret qua biến môi trường; KHÔNG commit giá trị thật.
  - SQL Server `dialect` Spring Boot 4 tự nhận; chỉ khai báo nếu cần ép.
  - UTF-8: file Java/yml + JVM `-Dfile.encoding=UTF-8`.

---

## ✅ Checklist Phase 1
- [ ] Thêm đủ dependency (jpa, security, validation, jjwt ×3)
- [ ] Tạo cây package theo tầng
- [ ] `application.yml` + `application-dev.yml`, `ddl-auto: validate`
- [ ] Secret/DB password qua biến môi trường (không hardcode)
- [ ] `./gradlew bootRun` khởi động, nối SQL Server, không lỗi
- [ ] Log tiếng Việt hiển thị đúng (UTF-8)

---
*Hết Phase 1. Tiếp theo: Phase 2 — domain/entity (xem `PHASE2_DOMAIN_ENTITY.md`).*
