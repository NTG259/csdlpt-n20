# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.x REST API backend for a distributed multi-warehouse e-commerce system (hệ thống bán hàng đa kho phân tán). Java 21, Gradle (Kotlin DSL), SQL Server, JWT authentication.

## Commands

```bash
# Run locally
./gradlew bootRun

# Build (includes tests)
./gradlew build

# Tests only
./gradlew test

# Single test class (existing classes: CatalogControllerTest, GioHangControllerTest,
# GioHangServiceImplTest, ProductServiceImplTest, JwtServiceTest, SecurityConfigTest,
# GlobalExceptionHandlerTest)
./gradlew test --tests "csdlpt.sitemain.controller.GioHangControllerTest"

# All checks (lint + tests)
./gradlew check
```

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running.

## Development Setup

Copy `src/main/resources/application-dev.example.yml` to `application-dev.yml` and fill in values. The active profile is `dev`.

Required environment variables (or values in `application-dev.yml`):
- `DB_URL` — JDBC URL for SQL Server (e.g. `jdbc:sqlserver://localhost:1433;databaseName=SiteMain;encrypt=true;trustServerCertificate=true`)
- `DB_USERNAME` / `DB_PASSWORD`
- `JWT_SECRET` — min 32 characters
- `JWT_EXPIRATION_MS` — e.g. `86400000` (24h)
- `SERVER_PORT` — default `8080`

Database schema must be created manually before running — JPA is set to `ddl-auto: validate` (Hibernate validates against the existing schema; it will not create tables).

### Implemented surface (important)
Only catalog reads and the cart are wired up. **Auth endpoints (`/api/auth/**`) do not exist yet** — there is no `AuthController`; `UserService` only exposes `getProfile`. Likewise there is no admin catalog CRUD or admin-stats endpoint. Controllers present: `ProductController`, `CategoryController`, `BrandController`, `RegionController` (all read-only `GET`), and `GioHangController` (`/api/cart`, the only writes and the only auth-protected routes).

## Architecture

### Package Structure

```
src/main/java/csdlpt/sitemain/
├── config/          # SecurityConfig, OpenApiConfig, CORS
├── common/          # ApiResponse<T>, ErrorResponse, PageResponse, ErrorCodes
├── exception/       # GlobalExceptionHandler + custom exception classes
├── security/        # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── domain/
│   ├── entity/      # JPA entities (7 core entities)
│   ├── enums/       # VaiTro (role enum)
│   └── converter/   # BooleanToTinyIntConverter
├── dto/
│   ├── request/     # LoginRequest, RegisterRequest, product/category requests
│   ├── response/    # AuthResponse, ProductDetailResponse, etc.
│   └── projection/  # Optimized query projections
├── repository/      # Spring Data JPA repositories (8 repos)
├── service/         # Business logic interfaces
│   └── impl/        # Service implementations
├── controller/      # REST controllers (5 controllers)
└── util/            # Utility classes
```

### Response Contract

All endpoints return `ApiResponse<T>` on success or `ErrorResponse` on failure. Error codes are centralized in `ErrorCodes`. Jackson is configured to omit null fields.

### Security

- JWT filter (`JwtAuthenticationFilter`) runs before every request
- Public routes (per `SecurityConfig`): Swagger/OpenAPI, `/error`, and `GET` on `/api/products/**`, `/api/categories/**`, `/api/brands/**`, `/api/regions/**`. Note: `/api/auth/**` is **not** public/registered — those endpoints aren't built.
- All other routes (currently just `/api/cart`) require a valid Bearer token
- `@EnableMethodSecurity` is on; `VaiTro` = `ADMIN`/`WAREHOUSE_STAFF`/`USER` for future `@PreAuthorize`
- CORS allows `localhost:3000`, `5173`, `4173`

### Domain Entities (Vietnamese naming)

| Entity | Vietnamese | Notes |
|---|---|---|
| `NguoiDung` | User | UUID PK, role (`VaiTro`), region |
| `SanPhamCore` | Product core | String PK, category, brand, price |
| `SanPhamDetail` | Product detail | 1-to-1 with `SanPhamCore` |
| `DanhMuc` | Category | |
| `ThuongHieu` | Brand | |
| `KhuVuc` | Region | `maKhuVuc` is `"Bac"` / `"Nam"` |
| `GioHang` | Cart | per-user cart |
| `ChiTietGioHang` | Cart line item | belongs to `GioHang` |

UUID fields use `@JdbcTypeCode(SqlTypes.UUID)`. Booleans map to SQL Server `TINYINT` via `BooleanToTinyIntConverter`. All `@ManyToOne`/`@OneToOne` use `LAZY` fetch.

### Documentation

`docs/` contains 10 phase implementation plans and API documentation in Vietnamese. `docs/API_DOCUMENTATION.md` is the canonical API reference.
