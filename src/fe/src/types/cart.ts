export interface ChiTietGioHang {
  maSP: string
  tenSP: string
  hinhAnh: string
  donViTinh: string
  soLuong: number
  giaBan: number
  thanhTien: number
  soLuongKhaDung: number
}

export interface GioHang {
  sanPhamHopLe: ChiTietGioHang[]
  sanPhamHetHang: ChiTietGioHang[]
  tongSoLuong: number
  tongTien: number
}

export interface ThemVaoGioRequest {
  maSP: string
  soLuong: number
}

export interface CapNhatSoLuongRequest {
  soLuong: number
}
