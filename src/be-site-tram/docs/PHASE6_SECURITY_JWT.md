# PHASE 6 — BẢO MẬT CƠ BẢN (Spring Security + JWT) — chi tiết

> **Phạm vi:** Hash mật khẩu, sinh/verify JWT, lọc request, phân loại public/protected.
> **Mục tiêu phase:** API sản phẩm/đăng ký/đăng nhập **public**; có sẵn cơ chế JWT để mở rộng (giỏ hàng, đơn hàng) giai đoạn sau.

> **Tiến độ cập nhật 2026-06-02:**
> - Đã tạo `SecurityConfig` stateless với `BCryptPasswordEncoder`, CORS và phân loại public/protected route
> - Đã tạo `JwtService` với claims `userId`, `maKhuVuc`, `vaiTro`; hỗ trợ secret dạng plain text hoặc Base64
> - Đã tạo `JwtAuthenticationFilter`, `CustomUserDetails`, `CustomUserDetailsService`
> - 401 từ security entry point đang trả JSON `INVALID_CREDENTIALS`; 403 đang trả JSON `ACCESS_DENIED`
> - Đã bật `@EnableMethodSecurity` để sẵn cho phân quyền theo role ở phase sau
> - Đã có test cho JWT claims và routing security: public 200, protected 401, forbidden 403

---

## 0. Mô hình bảo mật giai đoạn đầu
- **Stateless** (không session), xác thực bằng **JWT Bearer**.
- Giai đoạn đầu hầu hết API **public** → JWT chủ yếu để **demo đăng nhập** và **chuẩn bị** cho site khu vực (token mang `maKhuVuc`, `vaiTro`).
- KHÔNG dùng OAuth2/refresh token phức tạp — chỉ access token đơn giản.

---

## Task 6.1 — PasswordEncoder
- **Class:** bean trong `config/SecurityConfig`.
- **Nội dung:** `@Bean PasswordEncoder passwordEncoder() → new BCryptPasswordEncoder()`.
- **Dùng ở:** `AuthService` (đăng ký: `encode`; đăng nhập: `matches`).

---

## Task 6.2 — JwtService (`security/JwtService`)
- **Đọc cấu hình:** `jwt.secret`, `jwt.expiration-ms` (từ `application.yml`).
- **Method skeleton:**
```
String generateToken(NguoiDung user);     // subject = email; claims: userId, maKhuVuc, vaiTro
String extractUsername(String token);     // email
boolean isTokenValid(String token);       // chữ ký + hạn
<T> T extractClaim(String token, ...);     // tiện ích
```
- **Claims khuyến nghị:** `sub=email`, `userId=maND`, `maKhuVuc`, `vaiTro` → site khu vực dùng được ngay (Phase 10).
- **Lưu ý jjwt 0.12.x:** dùng `Jwts.builder()...signWith(key)` với `SecretKey` tạo từ `Keys.hmacShaKeyFor(secret.getBytes())`; secret ≥ 32 byte.

---

## Task 6.3 — JwtAuthenticationFilter (`security/`)
- **Class:** `extends OncePerRequestFilter`.
- **Luồng:**
  1. Đọc header `Authorization: Bearer <token>`; không có → `filterChain.doFilter` (bỏ qua).
  2. `extractUsername` → load `UserDetails` qua `CustomUserDetailsService`.
  3. `isTokenValid` → set `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.
- Đăng ký **trước** `UsernamePasswordAuthenticationFilter` trong `SecurityConfig`.

---

## Task 6.4 — UserDetails
- **`CustomUserDetailsService implements UserDetailsService`:** `loadUserByUsername(email)` → `NguoiDungRepository.findByEmailFetchKhuVuc` → wrap.
- **`CustomUserDetails implements UserDetails`:** bọc `NguoiDung`; `getUsername()=email`, `getPassword()=matKhau`, `getAuthorities()` = `ROLE_<vaiTro>`.
- **`enabled`:** map theo `trangThai` (Boolean) — user khóa thì `false`.

---

## Task 6.5 — SecurityConfig (`config/`)
- **Class:** `@Configuration @EnableWebSecurity`, expose `SecurityFilterChain`.
- **Cấu hình:**
```
csrf: disable
sessionManagement: STATELESS
authorizeHttpRequests:
   permitAll:  POST /api/auth/**
               GET  /api/products/**, /api/categories/**, /api/brands/**, /api/regions/**
   anyRequest: authenticated
addFilterBefore: JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter
cors: bật cho FE demo (localhost:5173/3000...)
exceptionHandling: 401 entryPoint trả ErrorResponse INVALID_CREDENTIALS/Unauthorized
                   403 accessDenied trả ErrorResponse ACCESS_DENIED/Access denied
```
- **CORS:** `CorsConfigurationSource` cho phép origin FE, method GET/POST, header `Authorization`, `Content-Type`.
- **AuthenticationManager:** expose `@Bean` (nếu dùng `authenticate()` trong AuthService) — hoặc tự `matches` thủ công (đơn giản hơn cho đồ án).

---

## ✅ Checklist Phase 6
- [x] `PasswordEncoder` (BCrypt) bean
- [x] `JwtService` generate/validate, claims có `maKhuVuc` + `vaiTro`
- [x] `JwtAuthenticationFilter` đọc Bearer, set SecurityContext
- [x] `CustomUserDetailsService` + `CustomUserDetails`
- [x] `SecurityConfig`: stateless, public đúng danh sách, còn lại authenticated
- [x] CORS bật cho FE
- [x] 401/403 trả `ErrorResponse` (không trang lỗi HTML mặc định)
- [x] Test: `GET /api/products` không token → 200; route protected không token → 401; route forbidden → 403

---
*Hết Phase 6. Tiếp theo: Phase 7 — Service nghiệp vụ.*
