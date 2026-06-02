import type { ApiErrorCode } from "@/types/api"

export class ApiError extends Error {
  status: number
  errorCode: ApiErrorCode | "UNKNOWN"
  details: string[]

  constructor(
    message: string,
    status: number,
    errorCode: ApiErrorCode | "UNKNOWN" = "UNKNOWN",
    details: string[] = []
  ) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.errorCode = errorCode
    this.details = details
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}
