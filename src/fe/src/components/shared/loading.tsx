import { Loader2Icon } from "lucide-react"

import { cn } from "@/lib/utils"

interface LoadingProps {
  label?: string
  className?: string
}

export function Loading({ label = "Đang tải dữ liệu", className }: LoadingProps) {
  return (
    <div
      className={cn(
        "flex min-h-40 items-center justify-center gap-2 text-sm text-muted-foreground",
        className
      )}
      role="status"
      aria-live="polite"
    >
      <Loader2Icon className="size-4 animate-spin" />
      <span>{label}</span>
    </div>
  )
}
