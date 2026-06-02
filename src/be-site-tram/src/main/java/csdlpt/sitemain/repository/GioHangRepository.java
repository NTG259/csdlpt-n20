package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.GioHang;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GioHangRepository extends JpaRepository<GioHang, UUID> {

    @Query("SELECT g FROM GioHang g LEFT JOIN FETCH g.chiTietList c LEFT JOIN FETCH c.sanPham WHERE g.maND = :maND AND g.trangThai = 'active'")
    Optional<GioHang> findActiveByMaNDWithItems(@Param("maND") UUID maND);

    Optional<GioHang> findByMaNDAndTrangThai(UUID maND, String trangThai);
}
