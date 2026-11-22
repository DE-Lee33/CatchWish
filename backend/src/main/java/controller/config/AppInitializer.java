package controller.config;

import jakarta.servlet.Filter;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * web.xml 대신 Tomcat 시작 시 DispatcherServlet을 등록하는 클래스.
 * 모든 요청을 DispatcherServlet이 받고, /api/ 로 시작하는 주소는 @RestController가 처리한다.
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    // 루트 설정은 사용하지 않음 (설정은 WebConfig 하나로 통합)
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    // 요청 본문(JSON, 폼)의 한글이 깨지지 않도록 UTF-8로 고정
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[] { encodingFilter };
    }
}