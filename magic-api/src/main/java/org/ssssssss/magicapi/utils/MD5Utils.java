package org.ssssssss.magicapi.utils;

import java.security.MessageDigest;

import org.ssssssss.magicapi.core.exception.MagicAPIException;
import org.ssssssss.script.annotation.Comment;

/**
 * MD5加密工具类
 * 
 * MD5是一种广泛使用的哈希算法，可生成128位（16字节）哈希值，通常表示为32位十六进制数
 * 注意：MD5是单向哈希算法，无法通过数学方法逆向解密
 * 
 * @author peter
 */
public class MD5Utils {

	// 十六进制字符表用于转换字节为字符串
	private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	@Comment("MD5加密字符串\n\n"
			+ "使用MD5算法对输入字符串进行加密处理，生成32位小写十六进制哈希值\n\n"
			+ "注意：MD5是单向哈希算法，不可逆且存在安全性问题，不推荐用于密码存储等安全场景\n\n"
			+ "@param value 要加密的原始字符串\n\n"
			+ "@return 32位小写MD5哈希值\n\n"
			+ "@throws MagicAPIException 当加密过程出现异常时抛出\n\n"
			+ "示例:\n"
			+ "String encrypted = MD5Utils.encrypt(\"password123\");\n"
			+ "结果类似: \"482c811da5d5b4bc6d497ffa98491e38\"")
	public static String encrypt(String value) {
		return encrypt(value.getBytes());
	}

	@Comment("MD5加密字节数组\n\n"
			+ "使用MD5算法对字节数组进行加密处理，生成32位小写十六进制哈希值\n\n"
			+ "@param value 要加密的原始字节数组\n\n"
			+ "@return 32位小写MD5哈希值\n\n"
			+ "@throws MagicAPIException 当加密过程出现异常时抛出\n\n"
			+ "示例:\n"
			+ "byte[] data = \"hello\".getBytes();\n"
			+ "String encrypted = MD5Utils.encrypt(data);\n"
			+ "结果: \"5d41402abc4b2a76b9719d911017c592\"")
	public static String encrypt(byte[] value) {
		try {
			byte[] bytes = MessageDigest.getInstance("MD5").digest(value);
			char[] chars = new char[32];
			for (int i = 0; i < chars.length; i = i + 2) {
				byte b = bytes[i / 2];
				chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
				chars[i + 1] = HEX_CHARS[b & 0xf];
			}
			return new String(chars);
		} catch (Exception e) {
			throw new MagicAPIException("md5 encrypt error", e);
		}
	}
}
