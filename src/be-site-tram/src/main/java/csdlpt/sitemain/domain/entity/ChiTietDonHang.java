package csdlpt.sitemain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "ChiTietDonHang")
public class ChiTietDonHang {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaCTDH", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maCTDH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", nullable = false)
    private SanPhamCore sanPham;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "DonGia", precision = 15, scale = 2, nullable = false)
    private BigDecimal donGia;

    @Column(name = "ThanhTien", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal thanhTien;
}
