export type WarehouseExportType = "noi_bo" | "giao_khach"

export type WarehouseExportStatus =
  | "waiting_export"
  | "exported"
  | "cancelled"

export type WarehouseReceiveStatus = "waiting_receive" | "received"

export type WarehouseImportStatus =
  | "waiting_import"
  | "imported"
  | "cancelled"

export interface WarehouseContext {
  maNhanVien: string
  hoTen: string
  vaiTro: string
  maKhoPhuTrach: string
  tenKho?: string | null
  maKV?: string | null
}

export interface WarehouseDashboard {
  maKho: string
  waitingExportInternal: number
  waitingImportInternal: number
  readyToShipOrders: number
  waitingCustomerExport: number
  lowStockProducts: number
}

export interface PhieuXuatSummary {
  maPhieuXuat: string
  maDonHang: string
  maKhoXuat: string
  tenKhoXuat: string
  maKhoNhan?: string | null
  tenKhoNhan?: string | null
  loaiPhieu: WarehouseExportType
  trangThaiXuat: WarehouseExportStatus
  trangThaiNhan?: WarehouseReceiveStatus | null
  ngayTao: string
  soDongHang: number
  tongSoLuong: number
}

export interface ChiTietXuat {
  maCTXK: string
  maCTDH?: string | null
  maSP: string
  tenSP: string
  soLuongXuat: number
  soLuongTon?: number | null
  soLuongDatHang?: number | null
}

export interface PhieuXuatDetail extends PhieuXuatSummary {
  items: ChiTietXuat[]
}

export interface PhieuNhapSummary {
  maPhieuNhap: string
  maDonHang: string
  maKhoXuat: string
  tenKhoXuat?: string | null
  maKhoNhap: string
  tenKhoNhap: string
  trangThaiNhap: WarehouseImportStatus
  ngayNhap?: string | null
  soDongHang: number
  tongSoLuong: number
  sourceExportStatus?: string | null
}

export interface ChiTietNhap {
  maCTPN: string
  maSP: string
  tenSP: string
  soLuong: number
  donGiaNhap: number
}

export interface PhieuNhapDetail extends PhieuNhapSummary {
  maNhanVienNhap?: string | null
  items: ChiTietNhap[]
}

export interface ReadyToShipOrder {
  maDonHang: string
  ngayTao: string
  trangThaiDH: string
  maKhoXuat: string
  tenKhoXuat: string
  daCoPhieuXuatGiaoKhach: boolean
  maPhieuXuatGiaoKhach?: string | null
  soDongHang: number
  tongSoLuong: number
}

export interface WarehouseStockItem {
  maKho: string
  maSP: string
  tenSP: string
  soLuongTon: number
  soLuongDatHang: number
  soLuongKhaDung: number
  ngayCapNhat: string
}

export interface WarehouseActionResult {
  maPhieuXuat?: string | null
  maPhieuNhap?: string | null
  maDonHang?: string | null
  maKhoXuat?: string | null
  maKhoNhap?: string | null
  trangThaiMoi?: string | null
  message?: string | null
}

export interface WarehouseExportQuery {
  loai?: WarehouseExportType
  trangThaiXuat?: WarehouseExportStatus
  trangThaiNhan?: WarehouseReceiveStatus
  maDonHang?: string
  page?: number
  size?: number
  sort?: string
}

export interface WarehouseImportQuery {
  trangThaiNhap?: WarehouseImportStatus
  maDonHang?: string
  page?: number
  size?: number
  sort?: string
}

export interface WarehouseReadyOrderQuery {
  maDonHang?: string
  page?: number
  size?: number
  sort?: string
}

export interface WarehouseStockQuery {
  q?: string
  onlyReserved?: boolean
  onlyLowStock?: boolean
  page?: number
  size?: number
  sort?: string
}
