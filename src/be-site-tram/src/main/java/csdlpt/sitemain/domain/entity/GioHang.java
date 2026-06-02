package csdlpt.sitemain.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "GioHang")
public class GioHang {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaGioHang", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maGioHang;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaND", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maND;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    @Column(name = "TrangThai", length = 30, nullable = false)
    private String trangThai = "active";

    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietGioHang> chiTietList = new ArrayList<>();
}
