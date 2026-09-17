package com.study.study_community_platform.config;

import com.study.study_community_platform.config.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // URL 접근 권한 설정
        http.authorizeHttpRequests(authorize -> authorize

                        // 비로그인 사용자도 접근할 수 있는 GET 요청
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/members/join",
                                "/members/login",
                                "/studies"
                        ).permitAll()

                        // 비로그인 사용자도 접근할 수 있는 POST 요청
                        .requestMatchers(
                                HttpMethod.POST,
                                "/members/join",
                                "/members/login"
                        ).permitAll()

                        // 정적 파일과 오류 화면
                        .requestMatchers(
                                "/css/**",
                                "/vendor/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        // 숫자 ID로 된 스터디 상세 화면만 공개
                        // /studies/new는 이 조건과 일치하지 않으므로 보호
                        .requestMatchers(
                                regexMatcher(
                                        HttpMethod.GET,
                                        "^/studies/[0-9]+(?:\\?.*)?$"
                                )
                        ).permitAll()

                        // 위에서 허용하지 않은 요청은 로그인 필요
                        .anyRequest().authenticated()
                )

                // 로그인 설정
                .formLogin(form -> form

                        // GET 로그인 화면 주소
                        .loginPage("/members/login")

                        // POST 로그인 처리 주소
                        // 해당 주소는 Controller가 아니라 Security Filter가 처리
                        .loginProcessingUrl("/members/login")

                        // 로그인 폼의 아이디 input 이름
                        .usernameParameter("loginId")

                        // 로그인 폼의 비밀번호 input 이름
                        .passwordParameter("password")

                        // 로그인 성공 후 실행할 클래스
                        .successHandler(loginSuccessHandler)

                        // 로그인 실패 후 이동할 주소
                        .failureUrl("/members/login?error")
                        .permitAll()
                )

                // 로그아웃 설정
                .logout(logout -> logout

                        // POST 로그아웃 처리 주소
                        .logoutUrl("/members/logout")

                        // 성공 시 이동할 주소
                        .logoutSuccessUrl("/")

                        // 서버의 로그인 세션 제거
                        .invalidateHttpSession(true)

                        // SecurityContext의 Authentication 제거
                        .clearAuthentication(true)

                        // 브라우저의 세션 쿠키 제거
                        .deleteCookies("JSESSIONID")
                )

                // CSRF 방어 활성화 -> 기본 값: 활성화
                .csrf(Customizer.withDefaults())

                // 로그인 성공 시 기존 세션 ID를 새로운 값으로 변경
                // 로그인 전 세션 데이터는 유지하면서 세션 ID만 새롭게 발급
                .sessionManagement(session -> session
                        .sessionFixation(fixation ->
                                fixation.changeSessionId()
                        )
                );

        return http.build();
    }
}