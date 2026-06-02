import type { RegionCode } from "@/constants/regions"
import type { VaiTro } from "@/types/domain"

export function hasWarehouseAccess(vaiTro?: VaiTro | string | null) {
  return vaiTro === "WAREHOUSE_STAFF" || vaiTro === "ADMIN"
}

export function assertWarehouseSession(
  region: RegionCode | null,
  token: string | null,
  vaiTro?: VaiTro | string | null
): asserts region is RegionCode {
  if (!token) {
    throw new Error("Bạn cần đăng nhập để vào hệ thống kho.")
  }

  if (!region) {
    throw new Error("Tài khoản chưa gắn khu vực hợp lệ.")
  }

  if (!hasWarehouseAccess(vaiTro)) {
    throw new Error("Tài khoản không có quyền kho.")
  }
}
