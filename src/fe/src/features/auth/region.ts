import { KHU_VUC_TO_REGION, type RegionCode } from "@/constants/regions"

export function getUserRegion(maKhuVuc?: string): RegionCode | null {
  if (!maKhuVuc) {
    return null
  }

  return KHU_VUC_TO_REGION[maKhuVuc.trim().toLowerCase()] ?? null
}
