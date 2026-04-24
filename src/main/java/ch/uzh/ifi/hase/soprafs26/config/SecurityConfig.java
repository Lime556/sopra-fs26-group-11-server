package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Komplett deaktivieren, da Token manuell validiert werden
            .cors(Customizer.withDefaults()) // Übernimmt die CORS-Einstellungen aus Application.java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll() // Erlaubt den WebSocket-Handshake ohne Token
                .anyRequest().permitAll() // Alle Anfragen erlauben (Logik liegt in deinen Services)
            );
        
        return http.build();
    }
}