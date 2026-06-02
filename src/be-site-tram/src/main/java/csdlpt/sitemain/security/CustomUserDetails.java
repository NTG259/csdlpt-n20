package csdlpt.sitemain.security;

import csdlpt.sitemain.domain.entity.KhuVuc;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.enums.VaiTro;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final NguoiDung nguoiDung;

    public CustomUserDetails(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public static CustomUserDetails fromTokenClaims(
            String email,
            UUID maND,
            String maKhuVuc,
            String vaiTro,
            String maKhoPhuTrach,
            String hoTen,
            boolean enabled
    ) {
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaND(maND);
        nguoiDung.setMatKhau("");
        nguoiDung.setMaKhoPhuTrach(maKhoPhuTrach);
        nguoiDung.setHoTen(hoTen == null || hoTen.isBlank() ? email : hoTen);
        nguoiDung.setEmail(email);
        nguoiDung.setTrangThai(enabled);
        nguoiDung.setVaiTro(VaiTro.valueOf(vaiTro));

        if (maKhuVuc != null && !maKhuVuc.isBlank()) {
            KhuVuc khuVuc = new KhuVuc();
            khuVuc.setMaKhuVuc(maKhuVuc);
            nguoiDung.setKhuVuc(khuVuc);
        }

        return new CustomUserDetails(nguoiDung);
    }

    public UUID getUserId() {
        return nguoiDung.getMaND();
    }

    public String getMaKhuVuc() {
        return nguoiDung.getKhuVuc() == null ? null : nguoiDung.getKhuVuc().getMaKhuVuc();
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name()));
    }

    @Override
    public String getPassword() {
        return nguoiDung.getMatKhau();
    }

    @Override
    public String getUsername() {
        return nguoiDung.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(nguoiDung.getTrangThai());
    }
}
