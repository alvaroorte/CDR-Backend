package bo.cdr.app.appsecurity.auth.service;

import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {

        UserApp user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return CustomUserDetails.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .password(user.getPassword())
                .userId(user.getId())
                .enabled(Boolean.TRUE.equals(user.getActive()))
                .authorities(
                        userRoleRepository.findByUser(user).stream()
                                .map(r -> new SimpleGrantedAuthority(r.getRole().getCode()))
                                .toList()
                )
                .build();
    }
}
