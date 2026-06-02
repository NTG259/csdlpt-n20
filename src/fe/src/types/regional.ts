// Temporary contracts for North/South services until their API docs are available.
export interface CartItem {
  maSP: string
  tenSP: string
  hinhAnh: string
  giaBan: number
  soLuong: number
  thanhTien: number
}

export interface Cart {
  items: CartItem[]
  tongTien: number
}

export interface OrderItem {
  maSP: string
  tenSP: string
  soLuong: number
  donGia: number
  thanhTien: number
}

export interface Order {
  maDonHang: string
  ngayDat: string
  trangThai: string
  tongTien: number
  items: OrderItem[]
}
