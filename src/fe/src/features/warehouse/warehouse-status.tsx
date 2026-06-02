import { Badge } from "@/components/ui/badge"

const labels: Record<string, string> = {
  waiting_export: "Chờ xuất",
  exported: "Đã xuất",
  cancelled: "Đã hủy",
  waiting_receive: "Chờ nhận",
  received: "Đã nhận",
  waiting_import: "Chờ nhập",
  imported: "Đã nhập",
  noi_bo: "Nội bộ",
  giao_khach: "Giao khách",
  processing: "Đang xử lý",
  shipping: "Đang giao",
}

const classes: Record<string, string> = {
  waiting_export: "border-amber-200 bg-amber-50 text-amber-800",
  waiting_import: "border-amber-200 bg-amber-50 text-amber-800",
  waiting_receive: "border-sky-200 bg-sky-50 text-sky-800",
  exported: "border-emerald-200 bg-emerald-50 text-emerald-800",
  imported: "border-emerald-200 bg-emerald-50 text-emerald-800",
  received: "border-emerald-200 bg-emerald-50 text-emerald-800",
  cancelled: "border-slate-200 bg-slate-50 text-slate-700",
  giao_khach: "border-cyan-200 bg-cyan-50 text-cyan-800",
  noi_bo: "border-violet-200 bg-violet-50 text-violet-800",
}

export function warehouseStatusLabel(status?: string | null) {
  if (!status) {
    return "-"
  }

  return labels[status] ?? status
}

export function WarehouseStatusBadge({ status }: { status?: string | null }) {
  if (!status) {
    return <Badge variant="outline">-</Badge>
  }

  return (
    <Badge variant="outline" className={classes[status]}>
      {warehouseStatusLabel(status)}
    </Badge>
  )
}
