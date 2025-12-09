package org.ssssssss.magicboot.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.ssssssss.magicapi.modules.servlet.RequestModule;
import org.ssssssss.magicapi.modules.servlet.utils.RequestGlobal;

import cn.hutool.core.util.StrUtil;

@Component
public class Global {

    /**
     * 文件上的根目录
     */
    public static String dir;
    
    public static ResourceHandlerRegistry registry;
    
    public static void addResourceHandlers() {
    	if(!RequestGlobal.getDir().equals("")) {
			 registry.addResourceHandler(Global.USER_FILES_BASE_URL + "**")
		        .addResourceLocations("file:"+ RequestGlobal.getDir() + Global.USER_FILES_BASE_URL);
		}
    }

    public final static String USER_FILES_BASE_URL = "/userfiles/";

    public static String getDir() {
    	return dir;
    }

    @Value("${upload.dir:D:/tb/}")
    public void setDir(String dir) {
        Global.dir = dir; // 注意：静态变量需要用类名访问
    }

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
        // 改用 String.concat() 或 StringBuilder 避免 Java 9+ 优化
        if (!dir.endsWith("/")) {
            dir = dir.concat("/");  // 或者 dir = dir + "/";
        }
        return dir;
    }
}