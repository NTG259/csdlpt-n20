import { z } from "zod"

export const categorySchema = z.object({
  maDanhMuc: z.string().trim().min(1, "Mã danh mục bắt buộc").max(20),
  tenDanhMuc: z.string().trim().min(1, "Tên danh mục bắt buộc").max(100),
  maDanhMucCha: z.string().trim().max(20).optional(),
  moTa: z.string().trim().max(500).optional(),
  trangThai: z.boolean(),
})

export type CategoryFormInput = z.infer<typeof categorySchema>
