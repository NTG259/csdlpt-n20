package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.SanPhamDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamDetailRepository extends JpaRepository<SanPhamDetail, String> {

    Optional<SanPhamDetail> findBySanPhamCore_MaSP(String maSP);
}
