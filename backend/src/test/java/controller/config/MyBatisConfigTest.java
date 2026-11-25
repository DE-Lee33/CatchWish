package controller.config;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyBatisConfigTest {

    @Test
    public void sqlSessionFactory가_만들어지고_DB에_연결된다() throws Exception {
        Assume.assumeTrue("db.properties가 없어 건너뜀",
            MyBatisConfigTest.class.getClassLoader().getResource("db.properties") != null);

        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(MyBatisConfig.class)) {
            SqlSessionFactory factory = ctx.getBean(SqlSessionFactory.class);
            try (Connection conn = factory.getConfiguration().getEnvironment()
                                          .getDataSource().getConnection()) {
                assertTrue(conn.isValid(2));
            }
        }
    }
}