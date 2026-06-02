package csdlpt.sitemain.security;

import csdlpt.sitemain.repository.NguoiDungRepository;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final NguoiDungRepository nguoiDungRepository;

    public CustomUserDetailsService(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung voi email: " + username));
    }

    public Optional<CustomUserDetails> findByUsername(String username) {
        return nguoiDungRepository.findByEmailFetchKhuVuc(username)
                .map(CustomUserDetails::new);
    }
}
