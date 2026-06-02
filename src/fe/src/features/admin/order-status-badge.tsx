import { cn } from "@/lib/utils"

const DH_MAP: Record<string, { label: string; className: string }> = {
  pending: {
    label: "Chờ xác nhận",
    className: "bg-yellow-100 text-yellow-800",
  },
  processing: {
    label: "Đang xử lý",
    className: "bg-blue-100 text-blue-800",
  },
  shipping: {
    label: "Đang vận chuyển",
    className: "bg-purple-100 text-purple-800",
  },
  completed: {
    label: "Đã giao",
    className: "bg-emerald-100 text-emerald-800",
  },
  cancelled: {
    label: "Đã huỷ",
    className: "bg-red-100 text-red-800",
  },
}

const TT_MAP: Record<string, { label: string; className: string }> = {
  waiting_cod: {
    label: "Chờ COD",
    className: "bg-orange-100 text-orange-800",
  },
  paid: {
    label: "Đã thanh toán",
    className: "bg-emerald-100 text-emerald-800",
  },
  failed: {
    label: "TT thất bại",
    className: "bg-red-100 text-red-800",
  },
  cancelled: {
    label: "Đã huỷ TT",
    className: "bg-slate-100 text-slate-600",
  },
}

function StatusChip({
  label,
  className,
}: {
  label: string
  className: string
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        className
      )}
    >
      {label}
    </span>
  )
}

export function TrangThaiDHBadge({ value }: { value: string }) {
  const cfg = DH_MAP[value] ?? { label: value, className: "bg-slate-100 text-slate-700" }
  return <StatusChip {...cfg} />
}

export function TrangThaiTTBadge({ value }: { value: string }) {
  const cfg = TT_MAP[value] ?? { label: value, className: "bg-slate-100 text-slate-700" }
  return <StatusChip {...cfg} />
}

export const TRANG_THAI_DH_OPTIONS = [
  { value: "pending", label: "Chờ xác nhận" },
  { value: "processing", label: "Đang xử lý" },
  { value: "shipping", label: "Đang vận chuyển" },
  { value: "completed", label: "Đã giao" },
  { value: "cancelled", label: "Đã huỷ" },
]

export const TRANG_THAI_TT_OPTIONS = [
  { value: "waiting_cod", label: "Chờ COD" },
  { value: "paid", label: "Đã thanh toán" },
  { value: "failed", label: "TT thất bại" },
  { value: "cancelled", label: "Đã huỷ TT" },
]
