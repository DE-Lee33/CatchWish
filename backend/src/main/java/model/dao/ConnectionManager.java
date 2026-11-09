package model.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;

public class ConnectionManager {
    // 접속 정보는 깃에 올리지 않는 db.properties(src/main/resources)에서 읽는다
    private static final String DB_PROPERTIES = "db.properties";
    private static DataSource ds = null;

    public ConnectionManager() {
    	try (InputStream in = ConnectionManager.class.getClassLoader().getResourceAsStream(DB_PROPERTIES)) {
			if (in == null) {
				throw new IllegalStateException(DB_PROPERTIES + "가 없습니다. db.properties.example을 복사해서 만들어주세요.");
			}
			Properties props = new Properties();
			props.load(in);

			// DataSource 생성 및 설정
			BasicDataSource bds = new BasicDataSource();
	        bds.setDriverClassName(props.getProperty("db.driver"));
	        bds.setUrl(props.getProperty("db.url"));
	        bds.setUsername(props.getProperty("db.username"));
	        bds.setPassword(props.getProperty("db.password"));
			ds = bds;
			
			// 참고: WAS의 DataSource를 이용할 경우: 
			// 		Context init = new InitialContext();
			// 		ds = (DataSource)init.lookup("java:comp/env/jdbc/OracleDS");
		} catch (Exception ex) {
			ex.printStackTrace();
		}    	   
    }

    public Connection getConnection() {
    	Connection conn = null;
    	try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
    }
    
    public void close() {
		BasicDataSource bds = (BasicDataSource) ds;
		try {
			bds.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	// 현재 활성화 상태인 Connection 의 개수와 비활성화 상태인 Connection 개수 출력
	public void printDataSourceStats() {
		try {
			BasicDataSource bds = (BasicDataSource) ds;
			System.out.println("NumActive: " + bds.getNumActive());
			System.out.println("NumIdle: " + bds.getNumIdle());
		} catch (Exception ex) {
			ex.printStackTrace();
		}   
	}
}
