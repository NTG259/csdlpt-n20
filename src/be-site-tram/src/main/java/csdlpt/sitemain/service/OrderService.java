package csdlpt.sitemain.service;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.TaoDonHangRequest;
import csdlpt.sitemain.dto.response.DonHangResponse;
import csdlpt.sitemain.dto.response.DonHangSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    DonHangResponse taoDonHang(UUID maND, String maKhuVuc, TaoDonHangRequest request);

    PageResponse<DonHangSummaryResponse> getMyOrders(UUID maND, Pageable pageable);

    DonHangResponse getMyOrderDetail(UUID maND, UUID maDonHang);

    DonHangResponse xacNhanNhanHang(UUID maND, UUID maDonHang);
}
