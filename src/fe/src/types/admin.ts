export interface ProductUpsert {
  maSP: string
  tenSP: string
  maDanhMuc: string
  maThuongHieu: string
  giaBan: number
  donViTinh: string
  hinhAnh?: string
  trangThai: boolean
  moTa?: string
  thongSoKyThuat?: string
}

export interface CategoryUpsert {
  maDanhMuc: string
  tenDanhMuc: string
  maDanhMucCha?: string
  moTa?: string
  trangThai: boolean
}

export interface BrandUpsert {
  maThuongHieu: string
  tenThuongHieu: string
  trangThai: boolean
}

export interface RevenueStatsFilter {
  tuNgay?: string
  denNgay?: string
  maKho?: string
  maKhuVuc?: string
  maSP?: string
  chiTinhDaXuat?: boolean
}

export interface DoanhThuTheoKho {
  siteXuat: string
  maKhuVuc: string
  maKhoXuat: string
  tenKho: string
  soDonHang: number
  soPhieuXuat: number
  tongSoLuongXuat: number
  doanhThu: number
}

export interface DoanhThuTheoVung {
  maKhuVuc: string
  soDonHang: number
  soPhieuXuat: number
  soKhoThamGiaXuat: number
  tongSoLuongXuat: number
  doanhThu: number
}

export interface DoanhThuToanHeThong {
  tongSoDonHang: number
  tongSoPhieuXuat: number
  tongSoKhoThamGiaXuat: number
  tongSoLuongXuat: number
  tongDoanhThu: number
}

export interface ThongKeDoanhThu {
  theoKho: DoanhThuTheoKho[]
  theoVung: DoanhThuTheoVung[]
  toanHeThong: DoanhThuToanHeThong
}

export interface DoanhThuTheoThang {
  siteNguon: string
  nam: number
  thang: number
  maKho: string | null
  tenKho: string
  doanhThu: number
}

export interface SanPhamBanChay {
  maSP: string
  tenSP: string
  tongSoLuongBan: number
  tongDoanhThu: number
}

export interface DonHangNhieuKho {
  maDonHang: string
  soKhoXuat: number
  danhSachKhoXuat: string
}

export type TrangThaiDH =
  | "pending"
  | "processing"
  | "shipping"
  | "completed"
  | "cancelled"

export type TrangThaiTT = "waiting_cod" | "paid" | "failed" | "cancelled"

export interface AdminOrderItem {
  siteNguon: string
  maDonHang: string
  maND: string
  ngayDat: string
  hoTenNguoiNhan: string
  soDienThoaiNhan: string
  maKhuVucXuLi: string
  tongTien: number
  phuongThucTT: string
  trangThaiTT: TrangThaiTT
  trangThaiDH: TrangThaiDH
}

export interface AdminOrderChiTiet {
  maCTDH: string
  maSP: string
  tenSP: string
  soLuong: number
  donGia: number
  thanhTien: number
}

export interface AdminOrderDetail extends AdminOrderItem {
  diaChiGiao: string
  ghiChu?: string
  chiTiet: AdminOrderChiTiet[]
}

export interface AdminOrderPage {
  items: AdminOrderItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface AdminOrderFilter {
  siteNguon?: string
  trangThaiDH?: string
  trangThaiTT?: string
  tuNgay?: string
  denNgay?: string
  page?: number
  size?: number
}

export interface UpdateOrderStatusBody {
  siteNguon: string
  trangThaiDH?: string | null
  trangThaiTT?: string | null
}
