package controller.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 기본 설정.
 * - @EnableWebMvc: JSON 변환(Jackson), @RequestBody/@ResponseBody 등 MVC 기능 활성화
 * - @ComponentScan: controller, model.manager 패키지에서 @RestController, @Component 등을 찾음
 *
 * 이후 PR에서 이 클래스에 추가될 것:
 * - 로그인 확인 인터셉터 등록 (feat/login-check)
 * CORS는 Vite 프록시를 쓰므로 설정하지 않는다 (수정사항 12).
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "controller", "model.manager", "common" })
public class WebConfig implements WebMvcConfigurer {
}