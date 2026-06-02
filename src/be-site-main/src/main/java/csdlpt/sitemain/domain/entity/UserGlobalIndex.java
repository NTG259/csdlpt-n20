package csdlpt.sitemain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "User_Global_Index")
public class UserGlobalIndex {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaND", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maND;

    @Column(name = "Email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "SoDienThoai", length = 15, unique = true)
    private String soDienThoai;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKhuVuc", nullable = false)
    private KhuVuc khuVuc;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;
}
