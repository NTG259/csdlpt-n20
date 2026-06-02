import type { ChiTietGioHang, GioHang } from "@/types/cart"

export type CartLineStatus = "ok" | "vuot_ton" | "het_hang"

export interface CartValidation {
  hasOrderableItems: boolean
  overStockItems: ChiTietGioHang[]
  hasHetHang: boolean
  canCheckout: boolean
  blockReason?: string
}

export function getLineStatus(item: ChiTietGioHang): CartLineStatus {
  if (item.soLuongKhaDung <= 0) {
    return "het_hang"
  }

  if (item.soLuong > item.soLuongKhaDung) {
    return "vuot_ton"
  }

  return "ok"
}

export function validateCart(cart?: GioHang | null): CartValidation {
  const sanPhamHopLe = cart?.sanPhamHopLe ?? []
  const sanPhamHetHang = cart?.sanPhamHetHang ?? []
  const overStockItems = sanPhamHopLe.filter(
    (item) => item.soLuong > item.soLuongKhaDung
  )
  const hasOrderableItems = sanPhamHopLe.length > 0

  let blockReason: string | undefined

  if (!hasOrderableItems) {
    blockReason = "Giỏ chưa có sản phẩm còn hàng để đặt."
  } else if (overStockItems.length > 0) {
    blockReason = `Có ${overStockItems.length} sản phẩm vượt quá tồn kho. Vui lòng giảm số lượng trước khi đặt.`
  }

  return {
    hasOrderableItems,
    overStockItems,
    hasHetHang: sanPhamHetHang.length > 0,
    canCheckout: hasOrderableItems && overStockItems.length === 0,
    blockReason,
  }
}
