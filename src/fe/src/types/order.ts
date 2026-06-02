export interface TaoDonHangRequest {
  hoTenNguoiNhan: string
  soDienThoaiNhan: string
  diaChiGiao: string
  phuongThucTT: "COD"
  ghiChu?: string
}

export interface ChiTietDonHang {
  maSP: string
  tenSP: string
  soLuong: number
  donGia: number
  thanhTien: number
}

export interface DonHang {
  maDonHang: string
  trangThaiDH: string
  trangThaiTT: string
  tongTien: number
  ngayDat: string
  khuVucXuLi: string
  hoTenNguoiNhan: string
  soDienThoaiNhan: string
  diaChiGiao: string
  phuongThucTT: string
  ghiChu?: string
  items: ChiTietDonHang[]
}

export interface DonHangSummary {
  maDonHang: string
  trangThaiDH: string
  trangThaiTT: string
  tongTien: number
  ngayDat: string
  khuVucXuLi: string
  hoTenNguoiNhan: string
  soDienThoaiNhan: string
  diaChiGiao: string
  phuongThucTT: string
}
