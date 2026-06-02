package csdlpt.sitemain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "KhuVuc")
public class KhuVuc {

    @Id
    @Column(name = "MaKhuVuc", length = 10, nullable = false)
    private String maKhuVuc;

    @Column(name = "TenKhuVuc", length = 100, nullable = false)
    private String tenKhuVuc;
}
