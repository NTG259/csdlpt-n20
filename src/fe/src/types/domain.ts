export type VaiTro = "ADMIN" | "WAREHOUSE_STAFF" | "USER"

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: string
  hoTen: string
  email: string
  maKhuVuc: string
  vaiTro: VaiTro
}

export interface CheckAvailability {
  available: boolean
}

export interface ProductListItem {
  maSP: string
  tenSP: string
  giaBan: number
  donViTinh: string
  hinhAnh: string
  trangThai: boolean
  tenDanhMuc: string
  tenThuongHieu: string
}

export interface ProductDetail {
  maSP: string
  tenSP: string
  giaBan: number
  donViTinh: string
  hinhAnh: string
  trangThai: boolean
  ngayTao: string
  maDanhMuc: string
  tenDanhMuc: string
  maThuongHieu: string
  tenThuongHieu: string
  moTa?: string
  thongSoKyThuat?: string
}

export interface Category {
  maDanhMuc: string
  tenDanhMuc: string
  maDanhMucCha?: string
  moTa?: string
  trangThai: boolean
}

export interface Brand {
  maThuongHieu: string
  tenThuongHieu: string
  trangThai: boolean
}

export interface Region {
  maKhuVuc: string
  tenKhuVuc: string
}
