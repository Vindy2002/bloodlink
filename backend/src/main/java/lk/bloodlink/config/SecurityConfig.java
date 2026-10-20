package lk.bloodlink.config;

import lk.bloodlink.repo.UserRepository;
import lk.bloodlink.service.AsyncSender.SmsGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtService jwt, UserRepository users) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                .requestMatchers("/api/donor/**").hasRole("DONOR")
                .requestMatchers("/api/hospital/**").hasRole("HOSPITAL")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Please log in to continue.\"}");
            }))
            .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthFilter(jwt, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsSource(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.corsOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/api/**", c);
        return s;
    }

    /** Logs SMS by default. Set SMS_ENABLED=true + SMS_URL/SMS_TOKEN to call a real HTTP SMS provider. */
    @Bean
    SmsGateway smsGateway(AppProperties props) {
        var log = LoggerFactory.getLogger("SMS");
        var sms = props.sms();
        if (!sms.enabled() || sms.url() == null || sms.url().isBlank()) {
            return (to, text) -> log.info("[SMS disabled] to={} text={}", to, text);
        }
        RestClient client = RestClient.create();
        return (to, text) -> client.post().uri(sms.url())
                .header("Authorization", "Bearer " + sms.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("to", to, "message", text, "from", sms.sender()))
                .retrieve().toBodilessEntity();
    }
}
