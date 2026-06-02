package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.NguoiDung;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {

    Optional<NguoiDung> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select n from NguoiDung n
            join fetch n.khuVuc
            where n.email = :email
            """)
    Optional<NguoiDung> findByEmailFetchKhuVuc(@Param("email") String email);
}
