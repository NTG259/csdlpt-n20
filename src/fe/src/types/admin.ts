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
