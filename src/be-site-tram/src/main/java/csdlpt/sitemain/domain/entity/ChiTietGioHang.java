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
@Table(name = "ChiTietGioHang")
public class ChiTietGioHang {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaCTGH", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maCTGH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaGioHang", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", nullable = false)
    private SanPhamCore sanPham;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "NgayThem", nullable = false, updatable = false)
    private LocalDateTime ngayThem;
}
