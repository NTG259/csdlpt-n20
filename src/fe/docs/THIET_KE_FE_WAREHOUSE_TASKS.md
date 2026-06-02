# Task plan FE warehouse

Muc tieu: tao mot trang he thong rieng cho kho trong cung source frontend, tuong tu `/admin`, khong tach project moi.

Workspace FE de xuat:

- Route root: `src/app/warehouse`
- Feature folder: `src/features/warehouse`
- Types: `src/types/warehouse.ts`
- API: goi backend chi nhanh qua `getRegionalApiClient(region, token)`, khong goi Site Main.

Backend da co base API:

- `GET /api/warehouse/me`
- `GET /api/warehouse/dashboard`
- `GET /api/warehouse/phieu-xuat`
- `GET /api/warehouse/phieu-xuat/{maPhieuXuat}`
- `POST /api/warehouse/phieu-xuat/{maPhieuXuat}/xac-nhan-noi-bo`
- `POST /api/warehouse/phieu-xuat/{maPhieuXuat}/xac-nhan-giao-khach`
- `GET /api/warehouse/phieu-nhap`
- `GET /api/warehouse/phieu-nhap/{maPhieuNhap}`
- `POST /api/warehouse/phieu-nhap/{maPhieuNhap}/xac-nhan`
- `GET /api/warehouse/orders/ready-to-ship`
- `POST /api/warehouse/orders/{maDonHang}/tao-phieu-giao-khach`
- `GET /api/warehouse/ton-kho`

## 1. Nguyen tac to chuc

Trang warehouse la mot workspace noi bo rieng, giong admin:

- Co layout rieng: `src/app/warehouse/layout.tsx`
- Co sidebar rieng: `src/features/warehouse/warehouse-sidebar.tsx`
- Co guard rieng: `src/features/warehouse/require-warehouse.tsx`
- An global `Header` va `Footer` khi pathname bat dau bang `/warehouse`
- Dung role `WAREHOUSE_STAFF`; co the cho `ADMIN` vao de debug/quan tri
- Dung region dang dang nhap (`useAuth().region`) de chon site Bac/Nam
- Goi `GET /api/warehouse/me` sau khi vao workspace de xac nhan user co `maKhoPhuTrach`

Khong de client truyen `maKhoPhuTrach`; backend da tu lay theo token.

## 2. Route map

| Route | Muc dich | API chinh |
| --- | --- | --- |
| `/warehouse` | Dashboard kho | `GET /api/warehouse/me`, `GET /api/warehouse/dashboard` |
| `/warehouse/phieu-xuat` | Danh sach + chi tiet + xac nhan phieu xuat | `GET /phieu-xuat`, `GET /phieu-xuat/{id}`, `POST /xac-nhan-*` |
| `/warehouse/phieu-nhap` | Danh sach + chi tiet + xac nhan phieu nhap | `GET /phieu-nhap`, `GET /phieu-nhap/{id}`, `POST /xac-nhan` |
| `/warehouse/giao-khach` | Don san sang giao va phieu xuat giao khach | `GET /orders/ready-to-ship`, `POST /tao-phieu-giao-khach`, `GET /phieu-xuat?loai=giao_khach` |
| `/warehouse/ton-kho` | Ton kho cua kho phu trach | `GET /ton-kho` |
| `/warehouse/forbidden` | Trang khong co quyen | none |

## 3. Files can tao/sua

### 3.1. Routing va layout

Can tao:

- `src/app/warehouse/layout.tsx`
- `src/app/warehouse/page.tsx`
- `src/app/warehouse/phieu-xuat/page.tsx`
- `src/app/warehouse/phieu-nhap/page.tsx`
- `src/app/warehouse/giao-khach/page.tsx`
- `src/app/warehouse/ton-kho/page.tsx`
- `src/app/warehouse/forbidden/page.tsx`

Can sua:

- `src/components/shared/header.tsx`
  - Neu `pathname.startsWith("/warehouse")` thi return `null`
  - Neu user la `WAREHOUSE_STAFF`, hien nut vao `/warehouse`
- `src/components/shared/footer.tsx`
  - Neu `pathname.startsWith("/warehouse")` thi return `null`

### 3.2. Feature warehouse

Can tao:

- `src/features/warehouse/require-warehouse.tsx`
- `src/features/warehouse/warehouse-sidebar.tsx`
- `src/features/warehouse/warehouse-error.ts`
- `src/features/warehouse/use-warehouse-context.ts`
- `src/features/warehouse/use-warehouse-dashboard.ts`
- `src/features/warehouse/use-warehouse-exports.ts`
- `src/features/warehouse/use-warehouse-imports.ts`
- `src/features/warehouse/use-warehouse-ready-orders.ts`
- `src/features/warehouse/use-warehouse-stock.ts`
- `src/features/warehouse/warehouse-status.ts`
- `src/features/warehouse/confirm-warehouse-action.tsx`
- `src/features/warehouse/phieu-xuat-detail-dialog.tsx`
- `src/features/warehouse/phieu-nhap-detail-dialog.tsx`

### 3.3. Types/constants

Can tao:

- `src/types/warehouse.ts`

Can sua:

- `src/constants/endpoints.ts`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_ME`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_DASHBOARD`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_EXPORTS`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_EXPORT_DETAIL(id)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_INTERNAL_EXPORT(id)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_CUSTOMER_EXPORT(id)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_IMPORTS`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_IMPORT_DETAIL(id)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_IMPORT(id)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_READY_TO_SHIP`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_CREATE_CUSTOMER_EXPORT(orderId)`
  - Them `REGIONAL_ENDPOINTS.WAREHOUSE_STOCK`
- `src/constants/query-keys.ts`
  - Them key group `warehouse`
- `src/types/api.ts`
  - Them error code moi tu backend:
    - `WAREHOUSE_NOT_ASSIGNED`
    - `WAREHOUSE_SCOPE_DENIED`
    - `INVALID_SLIP_STATUS`
    - `ORDER_NOT_READY_TO_SHIP`
    - `STOCK_RECORD_NOT_FOUND`
    - `DISTRIBUTED_TRANSACTION_ERROR`

## 4. Task breakdown

### Task 1: Warehouse shell

Muc tieu: co workspace rieng vao duoc bang `/warehouse`.

Can lam:

1. Tao `RequireWarehouse`.
2. Tao `WarehouseSidebar`.
3. Tao `warehouse/layout.tsx`.
4. Tao trang `/warehouse/forbidden`.
5. Sua Header/Footer de an tren `/warehouse`.
6. Them link "Kho" cho user `WAREHOUSE_STAFF` trong global Header.

Definition of done:

- User chua login vao `/warehouse` bi day ve login.
- User role `USER` vao `/warehouse` bi day ve `/warehouse/forbidden`.
- User `WAREHOUSE_STAFF` vao duoc workspace.
- Global header/footer khong hien trong workspace warehouse.

### Task 2: API types va hooks nen

Muc tieu: FE noi duoc backend warehouse.

Can lam:

1. Tao `src/types/warehouse.ts`.
2. Them endpoint constants.
3. Them query keys.
4. Tao helper assert session:
   - Can `token`
   - Can `region`
   - Can role `WAREHOUSE_STAFF` hoac `ADMIN`
5. Tao hooks:
   - `useWarehouseContext`
   - `useWarehouseDashboard`

Definition of done:

- `/warehouse` goi duoc `GET /api/warehouse/me`.
- Dashboard goi duoc `GET /api/warehouse/dashboard`.
- Loi `WAREHOUSE_NOT_ASSIGNED` hien thong bao ro rang.

### Task 3: Dashboard warehouse

Muc tieu: trang tong quan cho nhan vien kho.

UI can co:

- Header noi bo: ten kho, ma kho, khu vuc, nut refresh.
- Metric counters:
  - Cho xuat noi bo
  - Cho nhap noi bo
  - Don san sang giao
  - Cho xuat giao khach
  - San pham sap het
- Quick action links sang cac tab/list.

Definition of done:

- Loading state co skeleton.
- Error state co retry.
- Metric click duoc dieu huong sang page tuong ung.

### Task 4: Phieu xuat noi bo va giao khach

Muc tieu: xem va xac nhan phieu xuat.

Can lam:

1. Tao hook `useWarehouseExports`.
2. Tao hook `useWarehouseExportDetail`.
3. Tao mutation:
   - `useConfirmInternalExport`
   - `useConfirmCustomerExport`
4. Tao page `/warehouse/phieu-xuat`.
5. Tao detail dialog/drawer.
6. Tao confirm dialog truoc khi goi action.

Bo loc:

- `loai`: tat ca, `noi_bo`, `giao_khach`
- `trangThaiXuat`
- `trangThaiNhan`
- `maDonHang`

Button rule:

- `Xac nhan xuat noi bo` chi hien/enable khi `loaiPhieu = noi_bo` va `trangThaiXuat = waiting_export`
- `Xac nhan giao khach` chi hien/enable khi `loaiPhieu = giao_khach` va `trangThaiXuat = waiting_export`

Definition of done:

- Sau mutation thanh cong invalidate dashboard va list phieu xuat.
- Loi `INVALID_SLIP_STATUS` hien "Phieu khong con o trang thai cho phep".
- Loi `OUT_OF_STOCK` hien "Ton kho khong du".

### Task 5: Phieu nhap noi bo

Muc tieu: xem va xac nhan phieu nhap.

Can lam:

1. Tao hook `useWarehouseImports`.
2. Tao hook `useWarehouseImportDetail`.
3. Tao mutation `useConfirmInternalImport`.
4. Tao page `/warehouse/phieu-nhap`.
5. Tao detail dialog/drawer.

Bo loc:

- `trangThaiNhap`
- `maDonHang`

Button rule:

- `Xac nhan nhap` chi enable khi `trangThaiNhap = waiting_import`
- Neu `sourceExportStatus` khac `exported` va khac `remote`, disable va hien ly do "Kho xuat chua exported"

Definition of done:

- Sau mutation thanh cong invalidate dashboard, list phieu nhap, list ton kho.
- Loi `DISTRIBUTED_TRANSACTION_ERROR` hien thong bao co the thu lai.

### Task 6: Giao khach

Muc tieu: gom 2 viec trong mot trang:

- Don san sang tao phieu xuat giao khach.
- Phieu xuat giao khach dang cho xac nhan.

Can lam:

1. Tao hook `useReadyToShipOrders`.
2. Tao mutation `useCreateCustomerExportSlip`.
3. Tai su dung `useWarehouseExports` voi `loai = giao_khach`.
4. Tao page `/warehouse/giao-khach`.

UI de xuat:

- Section 1: bang don san sang giao
  - Ma don
  - Ngay dat
  - Tong so luong
  - Da co phieu giao khach hay chua
  - Nut tao phieu
- Section 2: bang phieu giao khach
  - Ma phieu
  - Ma don
  - Trang thai xuat
  - Nut xac nhan giao khach

Definition of done:

- Tao phieu xong invalidate ready orders va phieu xuat giao khach.
- Xac nhan giao khach xong don se ra khoi ready/shipping flow.

### Task 7: Ton kho

Muc tieu: xem ton kho cua kho phu trach.

Can lam:

1. Tao hook `useWarehouseStock`.
2. Tao page `/warehouse/ton-kho`.
3. Them filter:
   - `q`
   - `onlyReserved`
   - `onlyLowStock`
4. Hien cac cot:
   - Ma san pham
   - Ten san pham
   - So luong ton
   - So luong dang giu
   - So luong kha dung
   - Ngay cap nhat

Definition of done:

- Filter chay dung voi query API.
- San pham sap het duoc highlight bang badge.
- Khong co action ghi du lieu tren trang nay.

### Task 8: UX va QA

Muc tieu: trang kho dung on dinh cho nghiep vu hang ngay.

Can lam:

1. Chuan hoa status label:
   - `waiting_export` -> Cho xuat
   - `exported` -> Da xuat
   - `cancelled` -> Da huy
   - `waiting_receive` -> Cho nhan
   - `received` -> Da nhan
   - `waiting_import` -> Cho nhap
   - `imported` -> Da nhap
2. Chuan hoa error label cho warehouse error codes.
3. Test responsive desktop/mobile.
4. Chay `bun run build`.

Definition of done:

- Build pass.
- Khong co text tran khoi button/table tren mobile.
- Action co confirm dialog, loading state, disable state.

## 5. Thu tu implement de xuat

Lam theo thu tu nay de co the build kiem tra sau tung moc:

1. Task 1: shell + guard + sidebar
2. Task 2: types/endpoints/hooks context/dashboard
3. Task 3: dashboard page
4. Task 4: phieu xuat
5. Task 5: phieu nhap
6. Task 6: giao khach
7. Task 7: ton kho
8. Task 8: polish + build

## 6. Ranh gioi voi admin

Warehouse khong nen dung `src/features/admin`:

- Admin la quan tri catalog/thong ke Site Main.
- Warehouse la van hanh kho chi nhanh va goi regional backend.
- Neu dung chung component, chi nen dung component UI chung o `src/components/ui` hoac helper chung o `src/lib`.

Nen tach sidebar, guard, hooks va type rieng de tranh nham API main/regional.

## 7. Ranh gioi API main va regional

Warehouse phai dung:

```ts
getRegionalApiClient(region, token)
```

Khong dung:

```ts
mainApiClient
```

Ly do:

- API warehouse nam tren site chi nhanh.
- Du lieu `DonHang`, `PhieuXuatKho`, `PhieuNhapKho`, `TonKho` khong thuoc Site Main.
- Region cua user quyet dinh call Bac hay Nam.

## 8. Checklist truoc khi code

- Backend da compile/test pass.
- DB chi nhanh can co cot `PhieuNhapKho.MaDonHang` theo SP.
- User test can co role `WAREHOUSE_STAFF`.
- User test can co `MaKhoPhuTrach`.
- Env FE can co:
  - `NEXT_PUBLIC_NORTH_API_URL`
  - `NEXT_PUBLIC_SOUTH_API_URL`

## 9. Ket qua mong doi sau khi xong FE

Nguoi kho dang nhap vao `/warehouse` co the:

1. Xem kho minh phu trach va dashboard cong viec.
2. Xem/xac nhan phieu xuat noi bo.
3. Xem/xac nhan phieu nhap noi bo.
4. Tao phieu xuat giao khach khi don da du hang.
5. Xac nhan xuat giao khach.
6. Xem ton kho va hang dang giu cho don.
