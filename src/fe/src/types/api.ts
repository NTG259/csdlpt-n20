export interface ApiResponse<T> {
  success: true
  message: string
  data: T
  timestamp: string
}

export type ApiErrorCode =
  | "VALIDATION_ERROR"
  | "INVALID_CREDENTIALS"
  | "ACCESS_DENIED"
  | "RESOURCE_NOT_FOUND"
  | "DUPLICATE_EMAIL"
  | "DUPLICATE_PHONE"
  | "INVALID_REGION"
  | "CART_EMPTY"
  | "OUT_OF_STOCK"
  | "PRODUCT_INVALID"
  | "ORDER_NOT_FOUND"
  | "INVALID_ORDER_STATE"
  | "SLIP_NOT_FOUND"
  | "WAREHOUSE_NOT_ASSIGNED"
  | "WAREHOUSE_SCOPE_DENIED"
  | "INVALID_SLIP_STATUS"
  | "ORDER_NOT_READY_TO_SHIP"
  | "STOCK_RECORD_NOT_FOUND"
  | "DISTRIBUTED_TRANSACTION_ERROR"
  | "PAYMENT_NOT_SUPPORTED"
  | "INTERNAL_ERROR"

export interface ApiErrorResponse {
  success: false
  message: string
  errorCode: ApiErrorCode
  details: string[]
  timestamp: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
