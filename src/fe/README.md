# fe — Frontend (Next.js)

Giao diện web cho hệ thống bán hàng đa kho phân tán. Xây dựng bằng **Next.js 16 (App Router)**,
React 19, TypeScript. Điểm đặc biệt: frontend **tự định tuyến request theo khu vực** — gọi site
trung tâm cho danh mục/quản trị, và gọi đúng site trạm (Bắc/Nam) cho giỏ hàng/đơn hàng/kho.

> Xem README tổng ở thư mục gốc dự án để biết bức tranh toàn hệ thống (3 site + frontend).

---

## Công nghệ

Next.js 16 (App Router) · React 19 · TypeScript · TanStack Query · React Hook Form + Zod ·
Tailwind CSS v4 · shadcn/ui · next-themes · sonner (toast). Quản lý gói bằng **Bun** (`bun.lock`).

---

## Định tuyến đa site (điểm cốt lõi)

Frontend nói chuyện với **3 backend** qua 3 biến môi trường:

| Client | File | Backend | Dùng cho |
|---|---|---|---|
| `main-api-client.ts` | `lib/` | `NEXT_PUBLIC_MAIN_API_URL` | Danh mục, quản trị, thống kê |
| `regional-api-client.ts` | `lib/` | `NEXT_PUBLIC_NORTH_API_URL` / `NEXT_PUBLIC_SOUTH_API_URL` | Giỏ hàng, đơn hàng, kho (theo khu vực `BAC`/`NAM` của người dùng) |

`api-fetch.ts` là wrapper `fetch` dùng chung (gắn token, xử lý lỗi/401). `auth-storage.ts` &
`auth-events.ts` quản lý token; `query-client.ts` cấu hình TanStack Query.

---

## Cấu trúc thư mục (`src/`)

```
app/                       # Next.js App Router
├── (auth)/login, register
├── products, products/[maSP]      # danh sách & chi tiết sản phẩm
├── cart, orders                   # giỏ hàng & đơn hàng (gọi site trạm)
├── admin/                         # khu vực quản trị (gọi site trung tâm)
│   ├── brands, categories, products, orders
│   ├── doanh-thu-theo-thang, san-pham-ban-chay, don-hang-nhieu-kho   # thống kê
│   └── forbidden
└── warehouse/                     # khu vực nhân viên kho
    ├── ton-kho, phieu-nhap, phieu-xuat, giao-khach
    └── forbidden
features/                  # Logic theo miền: auth, cart, orders, products, admin, warehouse
components/
├── ui/                    # shadcn/ui
└── shared/                # component dùng chung
lib/                       # api-fetch, main/regional-api-client, auth-*, query-client, format, utils
constants/                 # regions (BAC/NAM)...
types/
```

---

## Chạy local

```bash
cp .env.frontend.example .env.local   # rồi điền URL của 3 backend
bun install
bun run dev                            # http://localhost:3000
```

Các script khác: `bun run build`, `bun run start`, `bun run lint`.

Biến môi trường:

| Biến | Ý nghĩa |
|---|---|
| `NEXT_PUBLIC_MAIN_API_URL` | URL site trung tâm (danh mục, quản trị, thống kê) |
| `NEXT_PUBLIC_NORTH_API_URL` | URL site trạm Miền Bắc |
| `NEXT_PUBLIC_SOUTH_API_URL` | URL site trạm Miền Nam |
| `NEXT_ALLOWED_DEV_ORIGINS` | Origin được phép ở chế độ dev |
| `NEXT_IMAGE_ALLOW_LOCAL_IP` | Cho phép ảnh từ IP nội bộ |

### Docker

```bash
docker compose up -d --build
```

(Các `NEXT_PUBLIC_*` được truyền vào lúc **build** — xem `docker-compose.yml`.)

---

## Tài liệu thiết kế

Thư mục `docs/` chứa: `API.md`, `API_DON_HANG.md`, `API_USER.md`,
`THIET_KE_FE_WAREHOUSE_TASKS.md`.
