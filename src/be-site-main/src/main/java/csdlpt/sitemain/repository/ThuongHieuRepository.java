package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.ThuongHieu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, String> {

    List<ThuongHieu> findByTrangThai(Boolean trangThai);

    boolean existsByTenThuongHieu(String tenThuongHieu);

    boolean existsByTenThuongHieuAndMaThuongHieuNot(String tenThuongHieu, String maThuongHieu);
}
