package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.DonHang;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonHangRepository extends JpaRepository<DonHang, UUID> {

    Page<DonHang> findByMaND(UUID maND, Pageable pageable);

    @Query("""
            SELECT DISTINCT dh
            FROM DonHang dh
            LEFT JOIN FETCH dh.chiTietList ct
            LEFT JOIN FETCH ct.sanPham
            WHERE dh.maDonHang = :maDonHang
            """)
    Optional<DonHang> findByIdWithItems(@Param("maDonHang") UUID maDonHang);

    @Query("""
            SELECT DISTINCT dh
            FROM DonHang dh
            LEFT JOIN FETCH dh.chiTietList ct
            LEFT JOIN FETCH ct.sanPham
            WHERE dh.maDonHang = :maDonHang
              AND dh.maND = :maND
            """)
    Optional<DonHang> findByIdAndMaNDWithItems(
            @Param("maDonHang") UUID maDonHang,
            @Param("maND") UUID maND
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE DonHang dh
               SET dh.trangThaiDH = 'completed',
                   dh.trangThaiTT = CASE
                       WHEN dh.phuongThucTT = 'COD' AND dh.trangThaiTT = 'waiting_cod'
                       THEN 'paid' ELSE dh.trangThaiTT END
             WHERE dh.maDonHang = :maDonHang
               AND dh.maND = :maND
               AND dh.trangThaiDH = 'shipping'
            """)
    int hoanTatDonDangGiao(
            @Param("maDonHang") UUID maDonHang,
            @Param("maND") UUID maND
    );
}
