package org.ssssssss.magicapi.modules.servlet.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.ssssssss.magicapi.modules.servlet.RequestModule;

import cn.hutool.core.util.StrUtil;

public class RequestGlobal {

	private static Environment environment; // 注入 Environment

	@Autowired
	public void setEnvironment(Environment env) {
		RequestGlobal.environment = env;
	}

	/**
	 * 文件上的根目录
	 */
	public static String getDir() {
		return environment.getProperty("upload.dir", "");
	}

	public final static String USER_FILES_BASE_URL = "/userfiles/";
	 

	/**
	 * 获取上传文件的根目录
	 *
	 * @return
	 */
	public static String getUserFilesBaseDir() {
		String dir = getDir();
		if (StrUtil.isBlank(dir)) {
			try {
				dir = RequestModule.getHttpServletRequest().getSession().getServletContext().getRealPath("/");
			} catch (Exception e) {
				return "";
			}
		}
		if (!dir.endsWith("/")) {
			dir += "/";
		}
		return dir;
	}

}
