"use client"

import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

import { Button } from "@/components/ui/button"

interface PaginationProps {
  page: number
  totalPages: number
  last?: boolean
  onPageChange: (page: number) => void
}

export function Pagination({
  page,
  totalPages,
  last,
  onPageChange,
}: PaginationProps) {
  const canGoPrevious = page > 0
  const canGoNext = !last && page + 1 < totalPages

  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4">
      <p className="text-sm text-muted-foreground">
        Trang {page + 1} / {totalPages}
      </p>
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!canGoPrevious}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeftIcon className="size-4" />
          Trước
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!canGoNext}
          onClick={() => onPageChange(page + 1)}
        >
          Sau
          <ChevronRightIcon className="size-4" />
        </Button>
      </div>
    </div>
  )
}
