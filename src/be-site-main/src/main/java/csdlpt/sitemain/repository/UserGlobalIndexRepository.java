package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.UserGlobalIndex;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGlobalIndexRepository extends JpaRepository<UserGlobalIndex, UUID> {

    boolean existsByEmail(String email);

    boolean existsBySoDienThoai(String soDienThoai);

    Optional<UserGlobalIndex> findByEmail(String email);
}
