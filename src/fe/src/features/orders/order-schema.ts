import { z } from "zod"

export const createOrderSchema = z.object({
  hoTenNguoiNhan: z
    .string()
    .trim()
    .min(1, "Vui long nhap ho ten nguoi nhan")
    .max(100, "Ho ten nguoi nhan toi da 100 ky tu"),
  soDienThoaiNhan: z
    .string()
    .trim()
    .regex(/^0\d{9}$/, "So dien thoai phai gom 10 so va bat dau bang 0"),
  diaChiGiao: z
    .string()
    .trim()
    .min(1, "Vui long nhap dia chi giao hang")
    .max(300, "Dia chi giao toi da 300 ky tu"),
  phuongThucTT: z.literal("COD"),
  ghiChu: z.string().trim().max(500, "Ghi chu toi da 500 ky tu").optional(),
})

export type CreateOrderInput = z.infer<typeof createOrderSchema>
