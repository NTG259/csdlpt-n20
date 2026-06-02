package csdlpt.sitemain.domain.entity;

import csdlpt.sitemain.common.converter.BooleanToTinyIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SanPham_Core")
public class SanPhamCore {

    @Id
    @Column(name = "MaSP", length = 20, nullable = false)
    private String maSP;

    @Column(name = "TenSP", length = 255, nullable = false)
    private String tenSP;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaDanhMuc", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaThuongHieu", nullable = false)
    private ThuongHieu thuongHieu;

    @Column(name = "GiaBan", precision = 15, scale = 2, nullable = false)
    private BigDecimal giaBan;

    @Column(name = "DonViTinh", length = 20, nullable = false)
    private String donViTinh;

    @Column(name = "HinhAnh", length = 500)
    private String hinhAnh;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @OneToOne(mappedBy = "sanPhamCore", fetch = FetchType.LAZY)
    private SanPhamDetail chiTiet;
}
