import { apiFetch, type ApiFetchOptions } from "@/lib/api-fetch"

type Query = NonNullable<ApiFetchOptions["query"]>

type GetOptions = Pick<
  ApiFetchOptions,
  "cache" | "headers" | "next" | "query" | "signal" | "token"
>

type MutationOptions = Pick<
  ApiFetchOptions,
  "cache" | "headers" | "next" | "signal" | "token"
>

function getMainBaseUrl() {
  const baseUrl = process.env.NEXT_PUBLIC_MAIN_API_URL

  if (!baseUrl) {
    throw new Error("Missing NEXT_PUBLIC_MAIN_API_URL")
  }

  return baseUrl
}

export const mainApiClient = {
  get: <T>(path: string, options?: GetOptions) =>
    apiFetch<T>(path, {
      method: "GET",
      baseUrl: getMainBaseUrl(),
      token: options?.token,
      query: options?.query as Query | undefined,
      headers: options?.headers,
      signal: options?.signal,
      cache: options?.cache,
      next: options?.next,
    }),

  post: <T>(path: string, body: unknown, options?: MutationOptions) =>
    apiFetch<T>(path, {
      method: "POST",
      baseUrl: getMainBaseUrl(),
      body,
      token: options?.token,
      headers: options?.headers,
      signal: options?.signal,
      cache: options?.cache,
      next: options?.next,
    }),

  put: <T>(path: string, body: unknown, options?: MutationOptions) =>
    apiFetch<T>(path, {
      method: "PUT",
      baseUrl: getMainBaseUrl(),
      body,
      token: options?.token,
      headers: options?.headers,
      signal: options?.signal,
      cache: options?.cache,
      next: options?.next,
    }),

  patch: <T>(path: string, body: unknown, options?: MutationOptions) =>
    apiFetch<T>(path, {
      method: "PATCH",
      baseUrl: getMainBaseUrl(),
      body,
      token: options?.token,
      headers: options?.headers,
      signal: options?.signal,
      cache: options?.cache,
      next: options?.next,
    }),

  delete: <T>(path: string, options?: MutationOptions) =>
    apiFetch<T>(path, {
      method: "DELETE",
      baseUrl: getMainBaseUrl(),
      token: options?.token,
      headers: options?.headers,
      signal: options?.signal,
      cache: options?.cache,
      next: options?.next,
    }),
}
