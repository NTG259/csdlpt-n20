package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.TaoDonHangRequest;
import csdlpt.sitemain.dto.response.DonHangResponse;
import csdlpt.sitemain.dto.response.DonHangSummaryResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Don hang", description = "Dat hang tu gio hang phia server")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Dat hang tu gio hang",
            description = "Tao don hang tu gio hang dang active cua nguoi dung. Request khong nhan danh sach san pham.")
    @PostMapping
    public ResponseEntity<ApiResponse<DonHangResponse>> taoDonHang(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TaoDonHangRequest request
    ) {
        DonHangResponse response = orderService.taoDonHang(
                userDetails.getUserId(),
                userDetails.getMaKhuVuc(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dat hang thanh cong", response));
    }

    @Operation(summary = "Danh sach don hang cua toi")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DonHangSummaryResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 10, sort = "ngayDat", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrders(userDetails.getUserId(), pageable)));
    }

    @Operation(summary = "Chi tiet don hang cua toi")
    @GetMapping("/{maDonHang}")
    public ResponseEntity<ApiResponse<DonHangResponse>> getMyOrderDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maDonHang") UUID maDonHang
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrderDetail(userDetails.getUserId(), maDonHang)));
    }
}
