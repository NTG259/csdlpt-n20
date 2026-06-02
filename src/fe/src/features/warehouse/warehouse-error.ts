import { isApiError } from "@/lib/api-error"

export function warehouseErrorMessage(error: unknown, fallback: string) {
  if (!isApiError(error)) {
    return fallback
  }

  switch (error.errorCode) {
    case "INVALID_CREDENTIALS":
      return "Site chi nhánh không chấp nhận token hiện tại. Kiểm tra JWT_SECRET và user trên site chi nhánh."
    case "WAREHOUSE_NOT_ASSIGNED":
      return "Tài khoản chưa được gán kho phụ trách."
    case "WAREHOUSE_SCOPE_DENIED":
    case "ACCESS_DENIED":
      return "Bạn không có quyền thao tác trên kho này."
    case "INVALID_SLIP_STATUS":
    case "INVALID_ORDER_STATE":
      return "Phiếu không còn ở trạng thái cho phép thao tác."
    case "ORDER_NOT_READY_TO_SHIP":
      return "Đơn hàng chưa sẵn sàng giao khách."
    case "OUT_OF_STOCK":
      return "Tồn kho không đủ để thực hiện thao tác."
    case "SLIP_NOT_FOUND":
      return "Không tìm thấy phiếu kho."
    case "DISTRIBUTED_TRANSACTION_ERROR":
      return "Lỗi giao dịch phân tán hoặc linked server. Có thể thử lại sau."
    default:
      return error.message || fallback
  }
}
