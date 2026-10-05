package com.sms.security;

import com.sms.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var roleAuth = Set.of(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    var permAuths = role.getPermissions().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getName()))
                            .collect(Collectors.toSet());
                    var combined = new java.util.HashSet<GrantedAuthority>();
                    combined.addAll(roleAuth);
                    combined.addAll(permAuths);
                    return combined.stream();
                })
                .collect(Collectors.toSet());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getIsAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsEnabled();
    }

    public User getUser() {
        return user;
    }
}
