package csdlpt.sitemain.domain.entity;

import csdlpt.sitemain.common.converter.BooleanToTinyIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "ThuongHieu")
public class ThuongHieu {

    @Id
    @Column(name = "MaThuongHieu", length = 20, nullable = false)
    private String maThuongHieu;

    @Column(name = "TenThuongHieu", length = 100, nullable = false, unique = true)
    private String tenThuongHieu;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai;
}
