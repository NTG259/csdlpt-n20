import { z } from "zod"

export const brandSchema = z.object({
  maThuongHieu: z.string().trim().min(1, "Mã thương hiệu bắt buộc").max(20),
  tenThuongHieu: z.string().trim().min(1, "Tên thương hiệu bắt buộc").max(100),
  trangThai: z.boolean(),
})

export type BrandFormInput = z.infer<typeof brandSchema>
