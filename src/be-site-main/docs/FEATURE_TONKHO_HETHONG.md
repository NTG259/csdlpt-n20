# FEATURE — Tồn kho toàn hệ thống cho trang chi tiết sản phẩm

> **Mục tiêu UX:** Ở trang chi tiết sản phẩm, hiển thị **một con số tổng** = tổng tồn kho khả dụng của sản phẩm trên **toàn hệ thống** (gộp Site Bắc + Site Nam). Khi người dùng **bấm vào con số đó**, mở ra **bảng chi tiết tồn kho theo từng kho**.
>
> **Bản chất phân tán:** số liệu lấy từ stored procedure `sp_TonKho_ToanHeThong` chạy trên DB `store_management` của Site Main, dùng `OPENQUERY` tới 2 linked server `SITE_BAC` / `SITE_NAM` → đây chính là phần minh hoạ truy vấn phân tán của đồ án.

Doc gồm 2 phần: **A — Backend (tạo API)** và **B — Frontend (tích hợp trang chi tiết)**.

---

## 0. Nguồn dữ liệu — Stored procedure (đã có sẵn)

File: `src/main/resources/db/proceduce.sql` → `dbo.sp_TonKho_ToanHeThong @MaSP`.

Procedure trả về **mỗi dòng = một kho** (union 2 site), các cột:

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `Site` | varchar | `'SITE_BAC'` hoặc `'SITE_NAM'` |
| `MaSP` | varchar | Mã sản phẩm |
| `TenSP` | nvarchar | Tên sản phẩm |
| `MaKho` | varchar | Mã kho |
| `TenKho` | nvarchar | Tên kho |
| `SoLuongTon` | int | Tồn vật lý trong kho |
| `SoLuongDatHang` | int | Đã đặt/giữ chỗ |
| `SoLuongKhaDung` | int | `SoLuongTon - SoLuongDatHang` |

> **Con số tổng** mà UI hiển thị = `SUM(SoLuongKhaDung)` trên tất cả dòng. **Chi tiết các kho** = chính danh sách dòng này.
>
> ⚠️ Một sản phẩm tồn tại nhưng **không có dòng nào** (chưa nhập kho ở cả 2 site) → procedure trả rỗng → tổng = 0.
> ⚠️ Nếu một linked server **chết**, `OPENQUERY` ném lỗi SQL → API trả `500 INTERNAL_ERROR` (xem xử lý lỗi mục A.6).

---

# PHẦN A — BACKEND (Site Main)

## A.1. Hợp đồng API

**`GET /api/products/{maSP}/ton-kho`** — Tồn kho toàn hệ thống của 1 sản phẩm.

- **Auth:** ❌ Công khai (nằm trong nhóm `GET /api/products/**` đã `permitAll` ở `SecurityConfig`). Không cần đổi cấu hình security.
- **Path:** `maSP` — mã sản phẩm.
- **Thành công:** `200 OK`, `data` = `TonKhoHeThongResponse`.

```jsonc
// GET /api/products/SP001/ton-kho
{
  "success": true,
  "message": "OK",
  "data": {
    "maSP": "SP001",
    "tenSP": "iPhone 15 128GB",
    "tongTonKho": 50,        // SUM(SoLuongTon)
    "tongDatHang": 8,        // SUM(SoLuongDatHang)
    "tongKhaDung": 42,       // SUM(SoLuongKhaDung) — CON SỐ TỔNG hiển thị
    "soLuongKho": 3,         // số kho có dữ liệu
    "chiTietKho": [
      { "site": "SITE_BAC", "maKho": "K_BAC_01", "tenKho": "Kho Hà Nội",  "soLuongTon": 30, "soLuongDatHang": 5, "soLuongKhaDung": 25 },
      { "site": "SITE_BAC", "maKho": "K_BAC_02", "tenKho": "Kho Hải Phòng","soLuongTon": 10, "soLuongDatHang": 2, "soLuongKhaDung": 8  },
      { "site": "SITE_NAM", "maKho": "K_NAM_01", "tenKho": "Kho HCM",      "soLuongTon": 10, "soLuongDatHang": 1, "soLuongKhaDung": 9  }
    ]
  },
  "timestamp": "2026-06-02T17:00:00"
}
```

- **Sản phẩm có thật nhưng chưa nhập kho:** `200`, `tongKhaDung = 0`, `soLuongKho = 0`, `chiTietKho = []`.
- **Lỗi:**
  - `404 RESOURCE_NOT_FOUND` — `maSP` không tồn tại trong `SanPham_Core`.
  - `500 INTERNAL_ERROR` — linked server lỗi / procedure lỗi.

> Vì Jackson `non_null`, mảng rỗng vẫn là `[]` (không bị lược); các field số luôn có mặt.

---

## A.2. DTO (`dto/response/`)

```java
// TonKhoChiTietKhoResponse.java — một dòng / một kho
public record TonKhoChiTietKhoResponse(
        String site,           // "SITE_BAC" | "SITE_NAM"
        String maKho,
        String tenKho,
        int soLuongTon,
        int soLuongDatHang,
        int soLuongKhaDung
) {}
```

```java
// TonKhoHeThongResponse.java — tổng hợp + danh sách kho
import java.util.List;

public record TonKhoHeThongResponse(
        String maSP,
        String tenSP,
        int tongTonKho,
        int tongDatHang,
        int tongKhaDung,
        int soLuongKho,
        List<TonKhoChiTietKhoResponse> chiTietKho
) {}
```

---

## A.3. Repository — gọi stored procedure bằng `JdbcTemplate`

Vì procedure trả cột động qua `OPENQUERY` (không map sạch sang entity JPA), dùng `JdbcTemplate` là gọn nhất. Spring Boot đã auto-config `JdbcTemplate` từ datasource — chỉ cần inject.

```java
// repository/TonKhoRepository.java
package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TonKhoRepository {

    private final JdbcTemplate jdbcTemplate;

    public TonKhoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TonKhoChiTietKhoResponse> timTonKhoToanHeThong(String maSP) {
        return jdbcTemplate.query(
                "EXEC dbo.sp_TonKho_ToanHeThong ?",
                (rs, rowNum) -> new TonKhoChiTietKhoResponse(
                        rs.getString("Site"),
                        rs.getString("MaKho"),
                        rs.getString("TenKho"),
                        rs.getInt("SoLuongTon"),
                        rs.getInt("SoLuongDatHang"),
                        rs.getInt("SoLuongKhaDung")
                ),
                maSP
        );
    }
}
```

> Tên cột trong `rs.getString(...)` phải **khớp alias** procedure trả về (`Site`, `MaKho`, `TenKho`, `SoLuongTon`, ...).

---

## A.4. Service (`service/` + `service/impl/`)

```java
// service/TonKhoService.java
public interface TonKhoService {
    TonKhoHeThongResponse getTonKhoToanHeThong(String maSP);
}
```

```java
// service/impl/TonKhoServiceImpl.java
package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import csdlpt.sitemain.dto.response.TonKhoHeThongResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.TonKhoRepository;
import csdlpt.sitemain.service.TonKhoService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TonKhoServiceImpl implements TonKhoService {

    private final TonKhoRepository tonKhoRepository;
    private final SanPhamCoreRepository sanPhamCoreRepository;

    public TonKhoServiceImpl(TonKhoRepository tonKhoRepository,
                             SanPhamCoreRepository sanPhamCoreRepository) {
        this.tonKhoRepository = tonKhoRepository;
        this.sanPhamCoreRepository = sanPhamCoreRepository;
    }

    @Override
    public TonKhoHeThongResponse getTonKhoToanHeThong(String maSP) {
        // 404 rõ ràng nếu sản phẩm không tồn tại (thay vì trả tổng 0 gây hiểu nhầm)
        var sanPham = sanPhamCoreRepository.findById(maSP)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        List<TonKhoChiTietKhoResponse> chiTiet = tonKhoRepository.timTonKhoToanHeThong(maSP);

        int tongTon = chiTiet.stream().mapToInt(TonKhoChiTietKhoResponse::soLuongTon).sum();
        int tongDat = chiTiet.stream().mapToInt(TonKhoChiTietKhoResponse::soLuongDatHang).sum();
        int tongKhaDung = chiTiet.stream().mapToInt(TonKhoChiTietKhoResponse::soLuongKhaDung).sum();

        return new TonKhoHeThongResponse(
                maSP,
                sanPham.getTenSP(),
                tongTon,
                tongDat,
                tongKhaDung,
                chiTiet.size(),
                chiTiet
        );
    }
}
```

> Lấy `tenSP` từ `SanPham_Core` (luôn có) thay vì từ dòng procedure (có thể rỗng khi chưa nhập kho).

---

## A.5. Controller — thêm endpoint vào `ProductController`

Thêm một method `GET /{maSP}/ton-kho` vào `ProductController` (cùng nhóm public với các GET sản phẩm khác). Inject thêm `TonKhoService`.

```java
@GetMapping("/{maSP}/ton-kho")
public ResponseEntity<ApiResponse<TonKhoHeThongResponse>> getTonKhoHeThong(
        @PathVariable("maSP") String maSP) {
    return ResponseEntity.ok(ApiResponse.ok(tonKhoService.getTonKhoToanHeThong(maSP)));
}
```

> Đặt ở `ProductController` để giữ URL gắn với tài nguyên sản phẩm (`/api/products/{maSP}/ton-kho`). Nếu muốn tách bạch có thể tạo `TonKhoController` riêng — không bắt buộc.

---

## A.6. Xử lý lỗi & bảo mật

- **404:** `ResourceNotFoundException` đã được `GlobalExceptionHandler` map sang `RESOURCE_NOT_FOUND`. Không cần thêm gì.
- **500 (linked server down / OPENQUERY fail):** `JdbcTemplate` ném `DataAccessException`. Đảm bảo `GlobalExceptionHandler` có nhánh fallback trả `INTERNAL_ERROR` (đã có nhánh `Exception` chung). *Tùy chọn nâng cao:* thêm exception riêng `TonKhoUnavailableException` + thông điệp "Không truy vấn được tồn kho từ các kho khu vực, vui lòng thử lại" nếu muốn message thân thiện hơn `INTERNAL_ERROR`.
- **Security:** không sửa gì — `GET /api/products/**` đã `permitAll`.
- **Swagger:** thêm `@Operation(summary = "Tồn kho toàn hệ thống của sản phẩm")` + `@ApiResponses` (200/404/500) cho method, đồng bộ style các controller khác.

---

## A.7. Test (tham khảo PHASE9 style)

- **Service test (`TonKhoServiceImplTest`):** mock `SanPhamCoreRepository` + `TonKhoRepository`.
  - Sản phẩm không tồn tại → ném `ResourceNotFoundException`.
  - Procedure trả 3 dòng → `tongKhaDung` = tổng đúng, `soLuongKho = 3`.
  - Procedure trả rỗng → tổng = 0, `chiTietKho = []`.
- **Controller test (`@WebMvcTest`):** GET trả `200` shape đúng; `maSP` lạ → `404 RESOURCE_NOT_FOUND`.

> Test repository thật cần linked server → để ở integration test thủ công (không chạy trong CI), vì phụ thuộc 2 site ngoài.

---

## A.8. Checklist Backend

- [x] `TonKhoChiTietKhoResponse`, `TonKhoHeThongResponse` (record, `dto/response/`).
- [x] `TonKhoRepository` — `JdbcTemplate` gọi `EXEC dbo.sp_TonKho_ToanHeThong ?`, RowMapper khớp alias.
- [x] `TonKhoService` + `TonKhoServiceImpl` — validate tồn tại sản phẩm (404), cộng tổng.
- [x] `ProductController` — `GET /{maSP}/ton-kho` + Swagger annotation.
- [x] `GlobalExceptionHandler` phủ được lỗi linked server → `INTERNAL_ERROR`.
- [x] Unit test service + controller.

---

# PHẦN B — FRONTEND (trang chi tiết sản phẩm)

Tích hợp vào trang chi tiết đã dựng ở [`FRONTEND_PHASE10_PRODUCTS.md`](FRONTEND_PHASE10_PRODUCTS.md). Catalog/sản phẩm gọi **Site Main** qua `mainApiClient` → tồn kho toàn hệ thống cũng gọi Site Main (vì chính Site Main chạy procedure phân tán).

## B.1. Types (`src/types/ton-kho.ts`)

```ts
export type Site = "SITE_BAC" | "SITE_NAM";

export interface TonKhoChiTietKho {
  site: Site;
  maKho: string;
  tenKho: string;
  soLuongTon: number;
  soLuongDatHang: number;
  soLuongKhaDung: number;
}

export interface TonKhoHeThong {
  maSP: string;
  tenSP: string;
  tongTonKho: number;
  tongDatHang: number;
  tongKhaDung: number;   // con số tổng hiển thị
  soLuongKho: number;
  chiTietKho: TonKhoChiTietKho[];
}

export const SITE_LABEL: Record<Site, string> = {
  SITE_BAC: "Miền Bắc",
  SITE_NAM: "Miền Nam",
};
```

## B.2. Hook (`src/features/products/use-ton-kho.ts`)

Vì UX cần thấy con số tổng ngay sau khi trang chi tiết load xong, FE **eager-load** tồn kho khi component mount. Người dùng bấm vào khối tồn kho chỉ để mở/đóng bảng chi tiết; không cần bấm mới thấy số tổng.

```ts
import { useQuery } from "@tanstack/react-query";
import { mainApiClient } from "@/lib/main-api-client";
import { QK } from "@/constants/query-keys";
import type { TonKhoHeThong } from "@/types/ton-kho";

export function useTonKhoHeThong(maSP: string, enabled = true) {
  return useQuery({
    queryKey: QK.tonKho(maSP),
    enabled: Boolean(maSP) && enabled,
    staleTime: 30_000,             // tồn kho biến động → cache ngắn
    queryFn: () =>
      mainApiClient().get<TonKhoHeThong>(`/api/products/${maSP}/ton-kho`),
  });
}
```

> `QK.tonKho(maSP)` khai ở `@/constants/query-keys`. Endpoint public nên **không cần token**.
>
> Đánh đổi: mỗi lượt xem chi tiết sản phẩm sẽ kích hoạt truy vấn cross-site. Bù lại UX rõ ràng hơn vì con số tồn kho tổng xuất hiện ngay.

## B.3. Component (`src/features/products/ton-kho-he-thong.tsx`)

Hành vi: hiển thị nút/badge "Tồn kho toàn hệ thống"; con số tổng fetch ngay khi component mount; bấm → toggle bảng chi tiết theo kho.

```tsx
"use client";
import { useState } from "react";
import { useTonKhoHeThong } from "./use-ton-kho";
import { SITE_LABEL, type Site } from "@/types/ton-kho";

export function TonKhoHeThong({ maSP }: { maSP: string }) {
  const [open, setOpen] = useState(false);
  const { data, isLoading, isError, refetch } = useTonKhoHeThong(maSP, open);

  return (
    <section className="rounded-lg border p-4">
      {/* Con số tổng — bấm để mở chi tiết */}
      <button
        className="flex w-full items-center justify-between"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="font-medium">Tồn kho toàn hệ thống</span>
        <span className="flex items-center gap-2">
          {data ? (
            <strong className={data.tongKhaDung > 0 ? "text-emerald-600" : "text-destructive"}>
              {data.tongKhaDung > 0 ? `Còn ${data.tongKhaDung}` : "Hết hàng"}
            </strong>
          ) : (
            <span className="text-muted-foreground">Xem tồn kho</span>
          )}
          <span aria-hidden>{open ? "▲" : "▼"}</span>
        </span>
      </button>

      {open && (
        <div className="mt-3">
          {isLoading && <p className="text-sm text-muted-foreground">Đang tải tồn kho…</p>}
          {isError && (
            <p className="text-sm text-destructive">
              Không tải được tồn kho.{" "}
              <button className="underline" onClick={() => refetch()}>Thử lại</button>
            </p>
          )}
          {data && data.chiTietKho.length === 0 && (
            <p className="text-sm text-muted-foreground">Sản phẩm chưa có ở kho nào.</p>
          )}
          {data && data.chiTietKho.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-muted-foreground">
                  <th>Khu vực</th><th>Kho</th>
                  <th className="text-right">Tồn</th>
                  <th className="text-right">Giữ chỗ</th>
                  <th className="text-right">Khả dụng</th>
                </tr>
              </thead>
              <tbody>
                {data.chiTietKho.map((k) => (
                  <tr key={`${k.site}-${k.maKho}`} className="border-t">
                    <td>{SITE_LABEL[k.site as Site] ?? k.site}</td>
                    <td>{k.tenKho}</td>
                    <td className="text-right">{k.soLuongTon}</td>
                    <td className="text-right">{k.soLuongDatHang}</td>
                    <td className="text-right font-medium">{k.soLuongKhaDung}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t font-semibold">
                  <td colSpan={2}>Tổng ({data.soLuongKho} kho)</td>
                  <td className="text-right">{data.tongTonKho}</td>
                  <td className="text-right">{data.tongDatHang}</td>
                  <td className="text-right">{data.tongKhaDung}</td>
                </tr>
              </tfoot>
            </table>
          )}
        </div>
      )}
    </section>
  );
}
```

## B.4. Gắn vào trang chi tiết

Trong `src/app/products/[maSP]/page.tsx` (Phase 10), render `<TonKhoHeThong maSP={maSP} />` dưới khối thông tin sản phẩm (gần nút "Thêm vào giỏ").

> Có thể dùng `data.tongKhaDung` để **vô hiệu hoá nút "Thêm vào giỏ"** khi `= 0` — nhưng nút giỏ thuộc luồng regional (Phase 11) và backend giỏ tự kiểm tra khả dụng, nên đây chỉ là gợi ý UX, không bắt buộc.

## B.5. Checklist Frontend

- [x] `src/types/ton-kho.ts` — types + `SITE_LABEL`.
- [x] `QK.tonKho(maSP)` trong `@/constants/query-keys`.
- [x] `use-ton-kho.ts` — query eager sau khi trang chi tiết load xong, không token, `staleTime` ngắn.
- [x] `ton-kho-he-thong.tsx` — con số tổng bấm để mở; bảng chi tiết theo kho (nhóm site); trạng thái loading/error/empty.
- [x] Gắn vào trang chi tiết sản phẩm `[maSP]/page.tsx`.
- [x] Kiểm tra build FE: `bunx tsc --noEmit`, `bun run lint`, `bun run build`.
- [ ] Kiểm tra E2E: mở trang chi tiết sẽ gọi API tồn kho; lỗi linked server → hiện "Thử lại" (cần backend Main + linked servers chạy được).

---

## Phụ lục — Quyết định thiết kế

- **Một endpoint duy nhất** trả cả tổng lẫn chi tiết: tổng = `SUM` của chính danh sách kho, nên tách 2 API không có lợi (đằng nào cũng phải chạy procedure cross-site một lần). FE bấm xem chi tiết = dùng lại payload đã fetch, không gọi thêm.
- **Eager sau khi trang chi tiết load xong** giúp người dùng thấy ngay tổng tồn kho. Đây là đánh đổi có chủ ý: tăng số request cross-site, nhưng UX rõ hơn cho demo.
- **`tongKhaDung`** (không phải `tongTonKho`) là con số người mua quan tâm: lượng thực sự có thể đặt.
