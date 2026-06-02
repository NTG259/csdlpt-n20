package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.dto.projection.ProductListItemView;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamCoreRepository extends JpaRepository<SanPhamCore, String> {

    Page<SanPhamCore> findByTrangThai(Boolean trangThai, Pageable pageable);

    Page<SanPhamCore> findByDanhMuc_MaDanhMuc(String maDanhMuc, Pageable pageable);

    Page<SanPhamCore> findByThuongHieu_MaThuongHieu(String maThuongHieu, Pageable pageable);

    @Query("""
            select p from SanPhamCore p
            where (:maDanhMuc is null or p.danhMuc.maDanhMuc = :maDanhMuc)
              and (:maThuongHieu is null or p.thuongHieu.maThuongHieu = :maThuongHieu)
              and (:trangThai is null or p.trangThai = :trangThai)
            """)
    Page<SanPhamCore> search(@Param("maDanhMuc") String maDanhMuc,
                             @Param("maThuongHieu") String maThuongHieu,
                             @Param("trangThai") Boolean trangThai,
                             Pageable pageable);

    @Query("""
            select p.maSP as maSP,
                   p.tenSP as tenSP,
                   p.giaBan as giaBan,
                   p.donViTinh as donViTinh,
                   p.hinhAnh as hinhAnh,
                   p.trangThai as trangThai,
                   d.tenDanhMuc as tenDanhMuc,
                   t.tenThuongHieu as tenThuongHieu
            from SanPhamCore p
            join p.danhMuc d
            join p.thuongHieu t
            where (:maDanhMuc is null or d.maDanhMuc = :maDanhMuc)
              and (:maThuongHieu is null or t.maThuongHieu = :maThuongHieu)
              and (:trangThai is null or p.trangThai = :trangThai)
            """)
    Page<ProductListItemView> searchProjection(@Param("maDanhMuc") String maDanhMuc,
                                               @Param("maThuongHieu") String maThuongHieu,
                                               @Param("trangThai") Boolean trangThai,
                                               Pageable pageable);

    @Query("""
            select p from SanPhamCore p
            join fetch p.danhMuc
            join fetch p.thuongHieu
            where p.maSP = :maSP
            """)
    Optional<SanPhamCore> findDetailById(@Param("maSP") String maSP);
}
