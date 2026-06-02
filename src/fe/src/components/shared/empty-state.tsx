import type { ReactNode } from "react"
import { InboxIcon } from "lucide-react"

import { cn } from "@/lib/utils"

interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

export function EmptyState({
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex min-h-56 flex-col items-center justify-center gap-3 rounded-md border border-dashed px-6 py-10 text-center",
        className
      )}
    >
      <InboxIcon className="size-8 text-muted-foreground" />
      <div className="space-y-1">
        <h2 className="text-base font-semibold">{title}</h2>
        {description ? (
          <p className="max-w-md text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  )
}
