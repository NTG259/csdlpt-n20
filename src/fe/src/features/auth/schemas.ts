import { z } from "zod"

const phoneRegex = /^(0|\+84)\d{9,10}$/
const cccdRegex = /^\d{12}$/

const optionalText = (maxLength: number) =>
  z.string().max(maxLength).optional().or(z.literal(""))

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, "Email không được để trống")
    .email("Email không đúng định dạng")
    .max(100, "Email không được vượt quá 100 ký tự"),
  matKhau: z
    .string()
    .min(6, "Mật khẩu phải từ 6 đến 72 ký tự")
    .max(72, "Mật khẩu phải từ 6 đến 72 ký tự"),
})

export type LoginInput = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  hoTen: z
    .string()
    .min(1, "Họ tên không được để trống")
    .max(100, "Họ tên không được vượt quá 100 ký tự"),
  email: z
    .string()
    .min(1, "Email không được để trống")
    .email("Email không đúng định dạng")
    .max(100, "Email không được vượt quá 100 ký tự"),
  soDienThoai: z
    .string()
    .regex(phoneRegex, "Số điện thoại không đúng định dạng")
    .max(15, "Số điện thoại không được vượt quá 15 ký tự"),
  matKhau: z
    .string()
    .min(6, "Mật khẩu phải từ 6 đến 72 ký tự")
    .max(72, "Mật khẩu phải từ 6 đến 72 ký tự"),
  maKhuVuc: z
    .string()
    .min(1, "Vui lòng chọn khu vực")
    .max(10, "Mã khu vực không được vượt quá 10 ký tự"),
  diaChi: optionalText(300),
  ngaySinh: z
    .string()
    .optional()
    .or(z.literal(""))
    .refine((value) => {
      if (!value) {
        return true
      }

      const date = new Date(`${value}T00:00:00`)
      return !Number.isNaN(date.getTime()) && date < new Date()
    }, "Ngày sinh phải là ngày trong quá khứ"),
  gioiTinh: optionalText(10),
  cccd: z
    .string()
    .regex(cccdRegex, "CCCD phải gồm đúng 12 chữ số")
    .optional()
    .or(z.literal("")),
})

export type RegisterInput = z.infer<typeof registerSchema>
