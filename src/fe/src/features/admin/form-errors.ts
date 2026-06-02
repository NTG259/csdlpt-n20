import type { FieldValues, Path, UseFormReturn } from "react-hook-form"

import { isApiError } from "@/lib/api-error"

export function applyServerErrors<T extends FieldValues>(
  form: UseFormReturn<T>,
  error: unknown
) {
  if (!isApiError(error) || error.errorCode !== "VALIDATION_ERROR") {
    return false
  }

  for (const detail of error.details) {
    const separatorIndex = detail.indexOf(":")
    if (separatorIndex <= 0) {
      continue
    }

    const field = detail.slice(0, separatorIndex).trim() as Path<T>
    const message = detail.slice(separatorIndex + 1).trim()
    form.setError(field, {
      type: "server",
      message: message || "Giá trị không hợp lệ",
    })
  }

  return error.details.length > 0
}

export function adminErrorMessage(error: unknown, fallback: string) {
  if (!isApiError(error)) {
    return fallback
  }

  if (error.status === 403) {
    return "Không đủ quyền quản trị"
  }

  return error.message || fallback
}
