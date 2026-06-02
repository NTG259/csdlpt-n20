package csdlpt.sitemain.domain.entity;

import csdlpt.sitemain.common.converter.BooleanToTinyIntConverter;
import csdlpt.sitemain.domain.enums.VaiTro;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "NguoiDung")
public class NguoiDung {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaND", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maND;

    @Column(name = "MatKhau", length = 255, nullable = false)
    private String matKhau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKV", nullable = false)
    private KhuVuc khuVuc;

    @Column(name = "MaKhoPhuTrach", length = 20)
    private String maKhoPhuTrach;

    @Column(name = "HoTen", length = 100, nullable = false)
    private String hoTen;

    @Column(name = "Email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "SoDienThoai", length = 15, unique = true)
    private String soDienThoai;

    @Column(name = "DiaChi", length = 300)
    private String diaChi;

    @Column(name = "NgayDangKy", insertable = false, updatable = false)
    private LocalDate ngayDangKy;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai;

    @Column(name = "NgaySinh")
    private LocalDateTime ngaySinh;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh;

    @Column(name = "CCCD", length = 12)
    private String cccd;

    @Enumerated(EnumType.STRING)
    @Column(name = "VaiTro", length = 20, nullable = false)
    private VaiTro vaiTro;
}
