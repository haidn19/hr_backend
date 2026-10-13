package com.example.demo.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.List;
import java.util.Collections;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        @Value("${jwt.secret}")
        private String jwtSecret;

        @Value("${app.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
        private List<String> corsAllowedOriginPatterns;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Login được mở công khai; mọi API còn lại phải mang JWT hợp lệ.
        http
                .csrf((csrf) -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/users/all").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                // Đọc roles từ claim JWT để Spring dùng cho @PreAuthorize.
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(jwt -> Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                        .orElse(Collections.emptyList())
                        .stream()
                        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                        .toList());
                return converter;
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                // Backend dùng cùng secret và thuật toán HS256 để kiểm tra chữ ký token.
                byte[] decodedSecret = Base64.getDecoder().decode(jwtSecret);
                SecretKey secretKey = new SecretKeySpec(decodedSecret, "HmacSHA256");
                return NimbusJwtDecoder.withSecretKey(secretKey)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        }

        @Bean
        public UserDetailsService userDetailsService(UserRepository userRepository) {
                return username -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found: " + username));
        }
        
	@Bean
	public AuthenticationManager authenticationManager(
                        UserDetailsService userDetailsService) {
                // DaoAuthenticationProvider đối chiếu user trong database và mã hóa password.
                DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder());

		return new ProviderManager(authenticationProvider);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}


        @Bean
        CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Pattern cho phép mọi cổng local và các Vercel preview mà không cần đổi code
        // mỗi lần frontend được deploy. Có thể ghi đè bằng CORS_ALLOWED_ORIGIN_PATTERNS.
        configuration.setAllowedOriginPatterns(corsAllowedOriginPatterns);

        configuration.setAllowedMethods(
                List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
        }


        
@Configuration
public class DepartmentDataInitializer {

@Bean
        CommandLineRunner seedDepartments(DepartmentRepository repository, EmployeeRepository employeeRepository) {
        return args -> {
        // Tạo department mặc định và gắn lại department cho dữ liệu đầu.
        List.of("Kế toán", "Nhân sự", "Kỹ thuật").forEach(name ->
                repository.findByNameIgnoreCase(name)
                        .orElseGet(() -> repository.save(new Department(name))));

        employeeRepository.findAll().stream()
                .filter(employee -> employee.getDepartment() == null)
                .filter(employee -> employee.getLegacyDepartment() != null
                && !employee.getLegacyDepartment().isBlank())
                .forEach(employee -> {
                Department department = repository.findByNameIgnoreCase(employee.getLegacyDepartment())
                        .orElseGet(() -> repository.save(new Department(employee.getLegacyDepartment())));
                employee.setDepartment(department);
                employeeRepository.save(employee);
                });
        };
        }
        
}
}
