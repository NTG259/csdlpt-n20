"use client"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface ConfirmWarehouseActionProps {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  isPending?: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
}

export function ConfirmWarehouseAction({
  open,
  title,
  description,
  confirmLabel,
  isPending,
  onOpenChange,
  onConfirm,
}: ConfirmWarehouseActionProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={isPending}
            onClick={() => onOpenChange(false)}
          >
            Hủy
          </Button>
          <Button type="button" disabled={isPending} onClick={onConfirm}>
            {isPending ? "Đang xử lý..." : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
