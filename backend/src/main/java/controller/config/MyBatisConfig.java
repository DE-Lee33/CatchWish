package controller.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import model.dao.ConnectionManager;

/**
 * MyBatis 설정. 조회 전용으로 사용한다 (INSERT/UPDATE는 JDBCUtil 트랜잭션 유지).
 */
@Configuration
@MapperScan("model.mapper")
public class MyBatisConfig {

    // JDBCUtil과 같은 풀(ConnectionManager)을 공유한다.
    // 공유 풀이므로 Spring 종료 시 자동으로 닫히지 않게 destroyMethod를 비활성화한다.
    @Bean(destroyMethod = "")
    public DataSource dataSource() {
        return ConnectionManager.getDataSource();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);   // PERFORMANCE_NAME → performanceName
        bean.setConfiguration(cfg);
        return bean.getObject();
    }
}