package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.enums.VaiTro;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {

    Optional<NguoiDung> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsBySoDienThoai(String soDienThoai);

    @Query("""
            select n from NguoiDung n
            join fetch n.khuVuc
            where n.email = :email
            """)
    Optional<NguoiDung> findByEmailFetchKhuVuc(@Param("email") String email);

    @Query("""
            select n from NguoiDung n
            join fetch n.khuVuc
            where n.maND = :maND
            """)
    Optional<NguoiDung> findByIdFetchKhuVuc(@Param("maND") UUID maND);

    @Query(
            value = """
                    select n from NguoiDung n
                    join fetch n.khuVuc
                    where (:tuKhoa is null
                           or lower(n.hoTen) like lower(concat('%', :tuKhoa, '%'))
                           or lower(n.email) like lower(concat('%', :tuKhoa, '%'))
                           or n.soDienThoai like concat('%', :tuKhoa, '%'))
                      and (:vaiTro is null or n.vaiTro = :vaiTro)
                      and (:trangThai is null or n.trangThai = :trangThai)
                    """,
            countQuery = """
                    select count(n) from NguoiDung n
                    where (:tuKhoa is null
                           or lower(n.hoTen) like lower(concat('%', :tuKhoa, '%'))
                           or lower(n.email) like lower(concat('%', :tuKhoa, '%'))
                           or n.soDienThoai like concat('%', :tuKhoa, '%'))
                      and (:vaiTro is null or n.vaiTro = :vaiTro)
                      and (:trangThai is null or n.trangThai = :trangThai)
                    """
    )
    Page<NguoiDung> timKiem(
            @Param("tuKhoa") String tuKhoa,
            @Param("vaiTro") VaiTro vaiTro,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );
}
