package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;

import org.junit.Test;

public class ConnectionTest {

	@Test
	public void connectToDatabase() throws Exception {
		// 계정 정보가 없는 환경(새로 clone한 PC 등)에서는 실패 대신 건너뛴다
		assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
				getClass().getClassLoader().getResource("db.properties") != null);

		JDBCUtil jdbcUtil = new JDBCUtil();
		try {
			jdbcUtil.setSqlAndParameters("SELECT 1 FROM DUAL", null);
			ResultSet rs = jdbcUtil.executeQuery();
			assertNotNull("쿼리 실행 실패 — db.properties의 접속 정보를 확인하세요", rs);
			rs.next();
			assertEquals(1, rs.getInt(1));
		} finally {
			jdbcUtil.close();
		}
	}
}
