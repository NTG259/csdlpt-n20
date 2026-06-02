package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.DanhMuc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, String> {

    List<DanhMuc> findByTrangThai(Boolean trangThai);

    List<DanhMuc> findByDanhMucChaIsNull();

    List<DanhMuc> findByDanhMucCha_MaDanhMuc(String maCha);
}
