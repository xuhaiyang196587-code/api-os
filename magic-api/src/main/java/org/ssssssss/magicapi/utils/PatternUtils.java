package org.ssssssss.magicapi.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.ssssssss.script.annotation.Comment;

/**
 * 正则相关工具包
 *
 * @author mxd
 */
public class PatternUtils {

	private static final Map<String, Pattern> CACHED_PATTERNS = new ConcurrentHashMap<>();

	@Comment("使用正则表达式匹配字符串内容\n\n"
            + "该方法会缓存已编译的正则表达式模式，避免重复编译带来的性能开销\n\n"
            + "匹配规则说明:\n"
            + "1. 使用Pattern.matcher().find()进行部分匹配\n"
            + "2. 只要字符串包含匹配正则的子串即返回true\n"
            + "3. 如需全匹配，请在正则中使用^和$\n\n"
            + "@param content 要匹配的字符串内容\n\n"
            + "@param regex 正则表达式\n\n"
            + "@return 如果内容匹配正则表达式返回true，否则返回false\n\n"
            + "示例:\n"
            + "boolean result1 = PatternUtils.match(\"hello123\", \"\\\\d+\"); // true\n"
            + "boolean result2 = PatternUtils.match(\"hello\", \"^\\\\d+$\"); // false")
	public static boolean match(String content, String regex) {
		// 从缓存获取已编译的Pattern对象
		Pattern pattern = CACHED_PATTERNS.get(regex);
		if (pattern == null) {
			// 如果缓存中没有，则编译并存入缓存
			pattern = Pattern.compile(regex);
			CACHED_PATTERNS.put(regex, pattern);
		}
		// 执行匹配操作
		return pattern.matcher(content).find();
	}
}
