package csdlpt.sitemain.repository;

import csdlpt.sitemain.domain.entity.KhuVuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhuVucRepository extends JpaRepository<KhuVuc, String> {
}
