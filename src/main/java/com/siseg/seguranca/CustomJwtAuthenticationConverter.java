package com.siseg.seguranca;

import com.siseg.model.User;
import com.siseg.model.UserAuthenticated;
import com.siseg.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public CustomJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);
        List<GrantedAuthority> authorities = List.copyOf(userAuthenticated.getAuthorities());
        return new UsernamePasswordAuthenticationToken(userAuthenticated, null, authorities);
    }
}


