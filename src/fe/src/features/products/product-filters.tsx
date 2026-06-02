"use client"

import { useState } from "react"
import { RotateCcwIcon, SearchIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

import { useBrands, useCategories } from "./use-reference"

export const DEFAULT_PRODUCT_SIZE = 12
export const DEFAULT_PRODUCT_SORT = "ngayTao,desc"

export type ProductStatusFilter = "all" | "true" | "false"

export interface ProductFilterState {
  q: string
  maDanhMuc: string
  maThuongHieu: string
  trangThai: ProductStatusFilter
  sort: string
  size: number
}

interface ProductFiltersProps {
  filters: ProductFilterState
  resultLabel?: string
  onChange: (filters: ProductFilterState) => void
  onReset: () => void
}

const sortOptions = [
  { value: DEFAULT_PRODUCT_SORT, label: "Mới nhất" },
  { value: "tenSP,asc", label: "Tên A-Z" },
  { value: "tenSP,desc", label: "Tên Z-A" },
  { value: "giaBan,asc", label: "Giá thấp đến cao" },
  { value: "giaBan,desc", label: "Giá cao đến thấp" },
]

const sizeOptions = [12, 24, 48]

function selectValue(value: string) {
  return value.trim() ? value : "all"
}

export function ProductFilters({
  filters,
  resultLabel,
  onChange,
  onReset,
}: ProductFiltersProps) {
  const [query, setQuery] = useState(filters.q)
  const { data: categories = [], isLoading: isLoadingCategories } =
    useCategories()
  const { data: brands = [], isLoading: isLoadingBrands } = useBrands()

  function updateFilter(next: Partial<ProductFilterState>) {
    onChange({ ...filters, ...next })
  }

  return (
    <Card className="rounded-xl">
      <CardContent className="grid gap-4 p-4">
        <form
          className="grid gap-2 sm:grid-cols-[1fr_auto]"
          onSubmit={(event) => {
            event.preventDefault()
            updateFilter({ q: query.trim() })
          }}
        >
          <div className="relative">
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo tên, mã, danh mục, thương hiệu..."
              className="h-10 pl-9"
            />
          </div>
          <Button type="submit" className="h-10">
            Tìm kiếm
          </Button>
        </form>

        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <Select
            value={selectValue(filters.maDanhMuc)}
            onValueChange={(value) =>
              updateFilter({
                maDanhMuc: value === "all" ? "" : String(value),
              })
            }
            disabled={isLoadingCategories}
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Tất cả danh mục" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả danh mục</SelectItem>
              {categories.map((category) => (
                <SelectItem
                  key={category.maDanhMuc}
                  value={category.maDanhMuc}
                  disabled={!category.trangThai}
                >
                  {category.tenDanhMuc}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select
            value={selectValue(filters.maThuongHieu)}
            onValueChange={(value) =>
              updateFilter({
                maThuongHieu: value === "all" ? "" : String(value),
              })
            }
            disabled={isLoadingBrands}
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Tất cả thương hiệu" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả thương hiệu</SelectItem>
              {brands.map((brand) => (
                <SelectItem
                  key={brand.maThuongHieu}
                  value={brand.maThuongHieu}
                  disabled={!brand.trangThai}
                >
                  {brand.tenThuongHieu}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select
            value={filters.trangThai}
            onValueChange={(value) =>
              updateFilter({
                trangThai: String(value) as ProductStatusFilter,
              })
            }
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Trạng thái" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả trạng thái</SelectItem>
              <SelectItem value="true">Đang bán</SelectItem>
              <SelectItem value="false">Ngừng bán</SelectItem>
            </SelectContent>
          </Select>

          <Select
            value={filters.sort}
            onValueChange={(value) => updateFilter({ sort: String(value) })}
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Sắp xếp" />
            </SelectTrigger>
            <SelectContent>
              {sortOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select
            value={String(filters.size)}
            onValueChange={(value) => updateFilter({ size: Number(value) })}
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Số sản phẩm" />
            </SelectTrigger>
            <SelectContent>
              {sizeOptions.map((size) => (
                <SelectItem key={size} value={String(size)}>
                  {size} / trang
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            {resultLabel ?? "Lọc theo danh mục, thương hiệu, trạng thái và giá."}
          </p>
          <Button type="button" variant="outline" size="sm" onClick={onReset}>
            <RotateCcwIcon className="size-4" />
            Xóa lọc
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
