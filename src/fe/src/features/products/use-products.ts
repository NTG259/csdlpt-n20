import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { mainApiClient } from "@/lib/main-api-client"
import type { PageResponse } from "@/types/api"
import type { ProductDetail, ProductListItem } from "@/types/domain"

export interface ProductQuery {
  page?: number
  size?: number
  sort?: string
  maDanhMuc?: string
  maThuongHieu?: string
  trangThai?: boolean
}

export function useProducts(params: ProductQuery = {}) {
  return useQuery({
    queryKey: QK.products(params),
    queryFn: () =>
      mainApiClient.get<PageResponse<ProductListItem>>(MAIN_ENDPOINTS.PRODUCTS, {
        query: {
          size: 12,
          ...params,
        },
      }),
    placeholderData: keepPreviousData,
  })
}

export function useProduct(maSP: string) {
  return useQuery({
    queryKey: QK.product(maSP),
    queryFn: () =>
      mainApiClient.get<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_DETAIL(maSP)),
    enabled: Boolean(maSP),
  })
}
