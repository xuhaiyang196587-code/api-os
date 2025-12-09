package org.ssssssss.magicapi.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.ssssssss.magicapi.core.exception.MagicAPIException;
import org.ssssssss.script.annotation.Comment;

public class JdbcUtils {

	private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);
	@Comment("获取数据库连接\n\n"
			+ "@param driver JDBC驱动类名（可为空，将从url自动推断）\n\n"
			+ "@param url 数据库连接URL\n\n"
			+ "@param username 数据库用户名\n\n"
			+ "@param password 数据库密码\n\n"
			+ "@return 数据库连接对象\n\n"
			+ "@throws MagicAPIException 当驱动类找不到或连接失败时抛出\n\n"
			+ "@example Connection conn = JdbcUtils.getConnection(\"com.mysql.jdbc.Driver\", \"jdbc:mysql://localhost:3306/test\", \"root\", \"password\");")
	public static Connection getConnection(String driver, String url, String username, String password) {
		try {
			if (StringUtils.isBlank(driver)) {
				driver = DatabaseDriver.fromJdbcUrl(url).getDriverClassName();
				if (StringUtils.isBlank(driver)) {
					throw new MagicAPIException("无法从url中获得驱动类");
				}
			}
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			throw new MagicAPIException("找不到驱动：" + driver);
		}
		try {
			return DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			logger.error("获取Jdbc链接失败", e);
			throw new MagicAPIException("获取Jdbc链接失败：" + e.getMessage());
		}
	}
	@Comment("关闭数据库连接\n\n"
			+ "@param connection 要关闭的数据库连接\n\n"
			+ "@example JdbcUtils.close(connection);")
	public static void close(Connection connection) {
		try {
			connection.close();
		} catch (Exception ignored) {

		}
	}
}
