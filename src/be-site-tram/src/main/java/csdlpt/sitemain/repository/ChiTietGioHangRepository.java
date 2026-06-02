package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.ChiTietGioHang;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, UUID> {
}
