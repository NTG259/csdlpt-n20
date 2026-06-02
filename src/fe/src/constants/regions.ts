export type RegionCode = "BAC" | "NAM"

export const KHU_VUC_TO_REGION: Record<string, RegionCode> = {
  bac: "BAC",
  nam: "NAM",
}

export const REGION_LABEL: Record<RegionCode, string> = {
  BAC: "Miền Bắc",
  NAM: "Miền Nam",
}
