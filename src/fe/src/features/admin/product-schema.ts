import { z } from "zod"

export const productSchema = z.object({
  maSP: z.string().trim().min(1, "Mã sản phẩm bắt buộc").max(20),
  tenSP: z.string().trim().min(1, "Tên sản phẩm bắt buộc").max(255),
  maDanhMuc: z.string().trim().min(1, "Chọn danh mục").max(20),
  maThuongHieu: z.string().trim().min(1, "Chọn thương hiệu").max(20),
  giaBan: z.number().positive("Giá bán phải lớn hơn 0"),
  donViTinh: z.string().trim().min(1, "Đơn vị tính bắt buộc").max(20),
  hinhAnh: z.string().trim().max(500).optional(),
  trangThai: z.boolean(),
  moTa: z.string().optional(),
  thongSoKyThuat: z.string().optional(),
})

export type ProductFormInput = z.infer<typeof productSchema>
