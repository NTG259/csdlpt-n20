export type Site = "SITE_BAC" | "SITE_NAM"

export interface TonKhoChiTietKho {
  site: Site
  maKho: string
  tenKho: string
  soLuongTon: number
  soLuongDatHang: number
  soLuongKhaDung: number
}

export interface TonKhoHeThong {
  maSP: string
  tenSP: string
  tongTonKho: number
  tongDatHang: number
  tongKhaDung: number
  soLuongKho: number
  chiTietKho: TonKhoChiTietKho[]
}

export const SITE_LABEL: Record<Site, string> = {
  SITE_BAC: "Miền Bắc",
  SITE_NAM: "Miền Nam",
}
