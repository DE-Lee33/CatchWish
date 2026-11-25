package controller.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * MyBatis 설정. 조회 전용으로 사용한다 (INSERT/UPDATE는 JDBCUtil 트랜잭션 유지).
 */
@Configuration
@MapperScan("model.mapper")
public class MyBatisConfig {

    // 임시: ConnectionManager에 getDataSource()가 반영되면 그 값을 반환하도록 교체 예정
    @Bean
    public DataSource dataSource() throws IOException {
        Properties props = new Properties();
        try (InputStream in = MyBatisConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties가 없습니다. db.properties.example을 복사해서 만들어주세요.");
            }
            props.load(in);
        }
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(props.getProperty("db.driver"));
        ds.setUrl(props.getProperty("db.url"));
        ds.setUsername(props.getProperty("db.username"));
        ds.setPassword(props.getProperty("db.password"));
        ds.setMaxTotal(5);   // 팀 공용 계정이라 연결 수를 작게 제한
        return ds;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml")); //classpath* 는 폴더가 없어도 빈 배열을 반환
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);   // PERFORMANCE_NAME → performanceName
        bean.setConfiguration(cfg);
        return bean.getObject();
    }
}