package csdlpt.sitemain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "DonHang")
public class DonHang {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaDonHang", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maDonHang;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaND", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maND;

    @Column(name = "NgayDat", nullable = false)
    private LocalDateTime ngayDat;

    @Column(name = "HoTenNguoiNhan", length = 100, nullable = false)
    private String hoTenNguoiNhan;

    @Column(name = "SoDienThoaiNhan", length = 15, nullable = false)
    private String soDienThoaiNhan;

    @Column(name = "DiaChiGiao", length = 300, nullable = false)
    private String diaChiGiao;

    @Column(name = "MaKhuVucXuLi", length = 10, nullable = false)
    private String maKhuVucXuLi;

    @Column(name = "TongTien", precision = 15, scale = 2, nullable = false)
    private BigDecimal tongTien;

    @Column(name = "PhuongThucTT", length = 20, nullable = false)
    private String phuongThucTT;

    @Column(name = "TrangThaiTT", length = 30, nullable = false)
    private String trangThaiTT;

    @Column(name = "TrangThaiDH", length = 30, nullable = false)
    private String trangThaiDH;

    @Column(name = "GhiChu", length = 500)
    private String ghiChu;

    @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY)
    private List<ChiTietDonHang> chiTietList = new ArrayList<>();
}
