package br.com.alevh.sistema_adocao_pets.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable()) // possibilita redirecionamento de dados em cyberataques de um site logado
                                              // para outro

                // gerencia as rotas e os acessos com token e sem
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**")
                        .permitAll()

                        // Rotas específicas com restrição

                        // Rotas de administrador
                        .requestMatchers("/api/v1/administradores/**").hasRole("ADMIN")

                        // Rotas de usuário
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/{nomeUsuario}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/id/{id}").hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/{id}/adocoes").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/{nomeUsuario}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/{nomeUsuario}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/{nomeUsuario}").hasAnyRole("USER", "ADMIN")

                        // Rotas de ong
                        .requestMatchers(HttpMethod.GET, "/api/v1/ongs").hasAnyRole("ADMIN", "USER", "ONG")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ongs/{nomeUsuario}").hasAnyRole("ADMIN", "ONG", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ongs/id/{id}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ongs/{id}/adocoes").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ongs/{nomeUsuario}/animais")
                        .hasAnyRole("ADMIN", "ONG", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ongs/{nomeUsuario}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ongs/{nomeUsuario}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ongs/{nomeUsuario}").hasAnyRole("ADMIN", "ONG")

                        // Rotas de animal
                        .requestMatchers(HttpMethod.GET, "/api/v1/animais").hasAnyRole("ADMIN", "ONG", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/animais/id/{id}").hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/animais/{nome}").hasAnyRole("ADMIN", "ONG", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/animais/registro").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/animais/{nome}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/animais/{nome}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/animais/{nome}").hasAnyRole("ADMIN", "ONG")

                        // Rotas de adoção
                        .requestMatchers(HttpMethod.GET, "/api/v1/adocoes").hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/adocoes/{id}").hasAnyRole("ADMIN", "ONG", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/adocoes/registro").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/adocoes/{id}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/adocoes/{id}").hasAnyRole("ADMIN", "ONG")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/adocoes/{id}").hasAnyRole("ADMIN", "ONG")

                        // Qualquer outra rota com permissão não especificada fica liberada
                        .anyRequest().permitAll())

                // antes de verificar as roles, vai validar o token do usuário
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // trata as exceções lançadas dentro do filtro
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateful -> guarda no site as infos de user/senha,
                // Stateless -> tokenização n armazena nada no servidor antes de verificar as roles,
                // vai validar o token do usuário
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class) // ordem dos filtros,
                                                                                             // primeiro parâmetro e dps
                                                                                             // o segundo, q é do spring
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
