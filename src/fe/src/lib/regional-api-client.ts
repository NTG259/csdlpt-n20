import type { RegionCode } from "@/constants/regions"
import { apiFetch, type ApiFetchOptions } from "@/lib/api-fetch"

type Query = NonNullable<ApiFetchOptions["query"]>

type RequestOptions = Pick<
  ApiFetchOptions,
  "cache" | "headers" | "next" | "query" | "signal"
>

function getRegionalBaseUrl(region: RegionCode) {
  const baseUrlByRegion: Record<RegionCode, string | undefined> = {
    BAC: process.env.NEXT_PUBLIC_NORTH_API_URL,
    NAM: process.env.NEXT_PUBLIC_SOUTH_API_URL,
  }

  const baseUrl = baseUrlByRegion[region]

  if (!baseUrl) {
    const envName =
      region === "BAC"
        ? "NEXT_PUBLIC_NORTH_API_URL"
        : "NEXT_PUBLIC_SOUTH_API_URL"

    throw new Error(`Missing ${envName}`)
  }

  return baseUrl
}

export function getRegionalApiClient(region: RegionCode, token?: string | null) {
  const baseUrl = getRegionalBaseUrl(region)

  return {
    get: <T>(path: string, options?: RequestOptions) =>
      apiFetch<T>(path, {
        method: "GET",
        baseUrl,
        token,
        logoutOnUnauthorized: false,
        query: options?.query as Query | undefined,
        headers: options?.headers,
        signal: options?.signal,
        cache: options?.cache,
        next: options?.next,
      }),

    post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "query">) =>
      apiFetch<T>(path, {
        method: "POST",
        baseUrl,
        token,
        logoutOnUnauthorized: false,
        body,
        headers: options?.headers,
        signal: options?.signal,
        cache: options?.cache,
        next: options?.next,
      }),

    put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "query">) =>
      apiFetch<T>(path, {
        method: "PUT",
        baseUrl,
        token,
        logoutOnUnauthorized: false,
        body,
        headers: options?.headers,
        signal: options?.signal,
        cache: options?.cache,
        next: options?.next,
      }),

    patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "query">) =>
      apiFetch<T>(path, {
        method: "PATCH",
        baseUrl,
        token,
        logoutOnUnauthorized: false,
        body,
        headers: options?.headers,
        signal: options?.signal,
        cache: options?.cache,
        next: options?.next,
      }),

    delete: <T>(path: string, options?: RequestOptions) =>
      apiFetch<T>(path, {
        method: "DELETE",
        baseUrl,
        token,
        logoutOnUnauthorized: false,
        query: options?.query as Query | undefined,
        headers: options?.headers,
        signal: options?.signal,
        cache: options?.cache,
        next: options?.next,
      }),
  }
}
