import type { ComponentPropsWithoutRef } from "react"

import { cn } from "@/lib/utils"

export function PageContainer({
  className,
  ...props
}: ComponentPropsWithoutRef<"main">) {
  return (
    <main
      className={cn("mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6", className)}
      {...props}
    />
  )
}
