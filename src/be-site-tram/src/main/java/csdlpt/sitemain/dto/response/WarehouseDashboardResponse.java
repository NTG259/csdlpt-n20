package csdlpt.sitemain.dto.response;

public record WarehouseDashboardResponse(
        String maKho,
        long waitingExportInternal,
        long waitingImportInternal,
        long readyToShipOrders,
        long waitingCustomerExport,
        long lowStockProducts
) {
}
