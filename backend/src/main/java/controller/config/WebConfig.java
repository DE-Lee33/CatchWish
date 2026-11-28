package controller.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import common.auth.LoginCheckInterceptor;

/**
 * Spring MVC 기본 설정.
 * - @EnableWebMvc: JSON 변환(Jackson), @RequestBody/@ResponseBody 등 MVC 기능 활성화
 * - @ComponentScan: controller, model.manager, model.dao, common 패키지에서
 *   @RestController, @Service, @Repository, @RestControllerAdvice 등을 찾음
 *
 * - 로그인 확인 인터셉터: /api/** 중 @LoginRequired가 붙은 메서드만 검사
 * CORS는 Vite 프록시를 쓰므로 설정하지 않는다 (수정사항 12).
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "controller", "model.manager", "model.dao", "model.storage", "common" })
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor()).addPathPatterns("/api/**");
    }
    
}