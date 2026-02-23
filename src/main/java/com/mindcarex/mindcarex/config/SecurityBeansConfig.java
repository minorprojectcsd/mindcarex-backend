package com.mindcarex.mindcarex.config;

import com.mindcarex.mindcarex.entity.User;
import com.mindcarex.mindcarex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;

@Configuration
@RequiredArgsConstructor
public class SecurityBeansConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .disabled(!user.isEnabled())
                    .build();
        };
    }
}
