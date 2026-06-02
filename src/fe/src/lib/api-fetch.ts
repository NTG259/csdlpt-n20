import { emitUnauthorized } from "@/lib/auth-events"
import { ApiError } from "@/lib/api-error"
import type { ApiErrorResponse, ApiResponse } from "@/types/api"

type QueryValue = string | number | boolean | null | undefined

export interface ApiFetchOptions extends Omit<RequestInit, "body"> {
  baseUrl: string
  token?: string | null
  body?: unknown
  query?: Record<string, QueryValue>
}

function buildUrl(
  baseUrl: string,
  path: string,
  query?: ApiFetchOptions["query"]
) {
  const url = new URL(path, baseUrl)

  if (!query) {
    return url.toString()
  }

  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === "") {
      continue
    }

    url.searchParams.set(key, String(value))
  }

  return url.toString()
}

async function parseResponseBody<T>(response: Response) {
  const rawText = await response.text()

  if (!rawText) {
    return null
  }

  try {
    return JSON.parse(rawText) as ApiResponse<T> | ApiErrorResponse
  } catch {
    return null
  }
}

function buildHeaders(
  headers: HeadersInit | undefined,
  token: string | null | undefined,
  hasBody: boolean
) {
  const requestHeaders = new Headers(headers)

  if (!requestHeaders.has("Accept")) {
    requestHeaders.set("Accept", "application/json")
  }

  if (hasBody && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json")
  }

  if (token && !requestHeaders.has("Authorization")) {
    requestHeaders.set("Authorization", `Bearer ${token}`)
  }

  return requestHeaders
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions
): Promise<T> {
  const { baseUrl, token, body, query, headers, ...rest } = options
  const hasBody = body !== undefined

  const response = await fetch(buildUrl(baseUrl, path, query), {
    ...rest,
    headers: buildHeaders(headers, token, hasBody),
    body: hasBody ? JSON.stringify(body) : undefined,
  })

  const parsed = await parseResponseBody<T>(response)

  if (response.ok && parsed?.success) {
    return parsed.data
  }

  const errorResponse = parsed && !parsed.success ? parsed : null

  if (response.status === 401 && token) {
    emitUnauthorized()
  }

  throw new ApiError(
    errorResponse?.message ?? `Server error (${response.status})`,
    response.status,
    errorResponse?.errorCode ?? "UNKNOWN",
    errorResponse?.details ?? []
  )
}
