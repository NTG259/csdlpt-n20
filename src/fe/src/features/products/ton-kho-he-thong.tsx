"use client"

import { useState } from "react"
import {
  ChevronDownIcon,
  ChevronUpIcon,
  Loader2Icon,
  RotateCcwIcon,
  WarehouseIcon,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { cn } from "@/lib/utils"
import { SITE_LABEL, type Site, type TonKhoChiTietKho } from "@/types/ton-kho"

import { useTonKhoHeThong } from "./use-ton-kho"

interface TonKhoHeThongProps {
  maSP: string
}

function getStockLabel(total?: number) {
  if (total == null) {
    return "Đang tải..."
  }

  return total > 0 ? `Còn ${total}` : "Hết hàng"
}

function getStockClassName(total?: number) {
  if (total == null) {
    return "text-muted-foreground"
  }

  return total > 0 ? "text-emerald-600" : "text-destructive"
}

function siteLabel(site: string) {
  return SITE_LABEL[site as Site] ?? site
}

function InventoryRow({ item }: { item: TonKhoChiTietKho }) {
  return (
    <TableRow>
      <TableCell>{siteLabel(item.site)}</TableCell>
      <TableCell>
        <div className="grid gap-0.5">
          <span className="font-medium">{item.tenKho}</span>
          <span className="text-xs text-muted-foreground">{item.maKho}</span>
        </div>
      </TableCell>
      <TableCell className="text-right tabular-nums">
        {item.soLuongTon}
      </TableCell>
      <TableCell className="text-right tabular-nums">
        {item.soLuongDatHang}
      </TableCell>
      <TableCell className="text-right font-semibold tabular-nums">
        {item.soLuongKhaDung}
      </TableCell>
    </TableRow>
  )
}

export function TonKhoHeThong({ maSP }: TonKhoHeThongProps) {
  const [open, setOpen] = useState(false)
  const { data, isLoading, isError, error, refetch } = useTonKhoHeThong(maSP)
  const Icon = open ? ChevronUpIcon : ChevronDownIcon

  return (
    <Card className="rounded-xl">
      <button
        type="button"
        aria-expanded={open}
        className="flex w-full items-center justify-between gap-4 p-4 text-left"
        onClick={() => setOpen((current) => !current)}
      >
        <span className="flex min-w-0 items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-lg bg-muted">
            <WarehouseIcon className="size-5 text-muted-foreground" />
          </span>
          <span className="grid gap-0.5">
            <span className="font-semibold">Tồn kho toàn hệ thống</span>
            <span className="text-sm text-muted-foreground">
              Bấm để xem chi tiết từng kho ở Site Bắc và Site Nam.
            </span>
          </span>
        </span>

        <span className="flex shrink-0 items-center gap-2">
          {isLoading && !data ? (
            <Loader2Icon className="size-4 animate-spin text-muted-foreground" />
          ) : null}
          <strong className={cn("text-sm", getStockClassName(data?.tongKhaDung))}>
            {getStockLabel(data?.tongKhaDung)}
          </strong>
          <Icon className="size-4 text-muted-foreground" />
        </span>
      </button>

      {open ? (
        <CardContent className="grid gap-3 border-t p-4">
          {isLoading && !data ? (
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2Icon className="size-4 animate-spin" />
              Đang tải tồn kho từ các site...
            </p>
          ) : null}

          {isError ? (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2">
              <p className="text-sm text-destructive">
                {error instanceof Error
                  ? error.message
                  : "Không tải được tồn kho. Vui lòng thử lại."}
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => refetch()}
              >
                <RotateCcwIcon className="size-4" />
                Thử lại
              </Button>
            </div>
          ) : null}

          {data ? (
            <>
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant={data.tongKhaDung > 0 ? "secondary" : "outline"}>
                  {getStockLabel(data.tongKhaDung)}
                </Badge>
                <Badge variant="outline">{data.soLuongKho} kho có dữ liệu</Badge>
                <Badge variant="outline">Đã giữ chỗ {data.tongDatHang}</Badge>
              </div>

              {data.chiTietKho.length === 0 ? (
                <p className="rounded-lg border border-dashed px-3 py-4 text-sm text-muted-foreground">
                  Sản phẩm chưa có tồn kho ở Site Bắc hoặc Site Nam.
                </p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Khu vực</TableHead>
                      <TableHead>Kho</TableHead>
                      <TableHead className="text-right">Tồn</TableHead>
                      <TableHead className="text-right">Giữ chỗ</TableHead>
                      <TableHead className="text-right">Khả dụng</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.chiTietKho.map((item) => (
                      <InventoryRow
                        key={`${item.site}-${item.maKho}`}
                        item={item}
                      />
                    ))}
                  </TableBody>
                  <TableFooter>
                    <TableRow>
                      <TableCell colSpan={2}>Tổng ({data.soLuongKho} kho)</TableCell>
                      <TableCell className="text-right tabular-nums">
                        {data.tongTonKho}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {data.tongDatHang}
                      </TableCell>
                      <TableCell className="text-right font-semibold tabular-nums">
                        {data.tongKhaDung}
                      </TableCell>
                    </TableRow>
                  </TableFooter>
                </Table>
              )}
            </>
          ) : null}
        </CardContent>
      ) : null}
    </Card>
  )
}
