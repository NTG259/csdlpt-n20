"use client"

import { useMutation } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { useAuth } from "@/features/auth/auth-context"
import { isApiError } from "@/lib/api-error"
import { mainApiClient } from "@/lib/main-api-client"
import type { AuthResponse } from "@/types/domain"

import type { LoginInput, RegisterInput } from "./schemas"

function getErrorMessage(error: unknown) {
  return isApiError(error) ? error.message : "Có lỗi xảy ra"
}

function compactRegisterInput(input: RegisterInput) {
  return Object.fromEntries(
    Object.entries(input).filter(([, value]) => value !== "" && value != null)
  )
}

function getSafeRedirectPath() {
  if (typeof window === "undefined") {
    return null
  }

  const redirect = new URLSearchParams(window.location.search).get("redirect")

  if (!redirect || !redirect.startsWith("/") || redirect.startsWith("//")) {
    return null
  }

  return redirect
}

export function useLogin() {
  const { login } = useAuth()
  const router = useRouter()

  return useMutation({
    mutationFn: (input: LoginInput) =>
      mainApiClient.post<AuthResponse>(MAIN_ENDPOINTS.LOGIN, input),
    onSuccess: (auth) => {
      login(auth)
      toast.success("Đăng nhập thành công")
      router.replace(getSafeRedirectPath() ?? "/products")
    },
    onError: (error) => {
      toast.error(getErrorMessage(error))
    },
  })
}

export function useRegister() {
  const { login } = useAuth()
  const router = useRouter()

  return useMutation({
    mutationFn: (input: RegisterInput) =>
      mainApiClient.post<AuthResponse>(
        MAIN_ENDPOINTS.REGISTER,
        compactRegisterInput(input)
      ),
    onSuccess: (auth) => {
      login(auth)
      toast.success("Đăng ký thành công")
      router.replace("/products")
    },
    onError: (error) => {
      toast.error(getErrorMessage(error))
    },
  })
}
