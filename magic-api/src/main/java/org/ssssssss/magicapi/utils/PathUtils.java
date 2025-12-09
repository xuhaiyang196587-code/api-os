package org.ssssssss.magicapi.utils;

import java.util.regex.Pattern;

import org.ssssssss.script.annotation.Comment;

/**
 * 路径处理工具包
 *
 * @author peter
 */
public class PathUtils {

	private static final Pattern REPLACE_SLASH_REGX = Pattern.compile("/+");

	@Comment("将多个/替换为一个/ \n\n"
			+ "@param path \n\n"
			+ "       url路径，/aa//bb （会转化成/a/b）\n\n")
	public static String replaceSlash(String path) {
		return REPLACE_SLASH_REGX.matcher(path).replaceAll("/");
	}
}
