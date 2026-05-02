package com.banking.account.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // account-service has no users DB
        // JWT signature is already validated by JwtFilter
        // we just need a valid UserDetails principal for the security context
        return User.withUsername(username)
                .password("") // no password needed — JWT already authenticated
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
    }
}