package csdlpt.sitemain.domain.entity;

import csdlpt.sitemain.common.converter.BooleanToTinyIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DanhMuc")
public class DanhMuc {

    @Id
    @Column(name = "MaDanhMuc", length = 20, nullable = false)
    private String maDanhMuc;

    @Column(name = "TenDanhMuc", length = 100, nullable = false)
    private String tenDanhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDanhMucCha")
    private DanhMuc danhMucCha;

    @Column(name = "MoTa", length = 500)
    private String moTa;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "DanhMuc_ThuongHieu",
            joinColumns = @JoinColumn(name = "MaDanhMuc"),
            inverseJoinColumns = @JoinColumn(name = "MaThuongHieu")
    )
    private Set<ThuongHieu> thuongHieus = new LinkedHashSet<>();
}
