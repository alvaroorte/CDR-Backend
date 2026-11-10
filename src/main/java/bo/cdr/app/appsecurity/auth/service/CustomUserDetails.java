package bo.cdr.app.appsecurity.auth.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Principal autenticado. Igual que la referencia, pero {@link #getUsername()} devuelve el
 * username (login por nombre de usuario) en lugar del email.
 */
@Getter
@Builder
public class CustomUserDetails implements UserDetails {

    private String username;
    private String fullName;
    private Integer userId;
    private String password;
    @Builder.Default
    private boolean enabled = true;
    private Collection<? extends SimpleGrantedAuthority> authorities;

    @Override
    public Collection<? extends SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
