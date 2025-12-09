package org.ssssssss.magicapi.utils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.script.annotation.Comment;

import cn.hutool.core.net.Ipv4Util;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

public class IpUtils {

	private static final String[] DEFAULT_IP_HEADER = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

	@Comment("获取客户端的真实IP地址，优先检查HTTP头信息，最后回退到远程地址\n\n"
			+ "适用于服务器位于代理或负载均衡器后方的情况，这些中间件可能会修改原始IP\n\n"
			+ "\n\n"
			+ "@param remoteAddr 直接远程地址（通常来自request.getRemoteAddr()）\n\n"
			+ "@param getHeader 用于通过头名称获取头内容的函数（通常用request::getHeader）\n\n"
			+ "@param otherHeaderNames 除默认头之外需要检查的其他HTTP头名称\n\n"
			+ "@return 处理后的真实IP地址，如果头信息中没有有效IP则返回处理后的远程地址\n\n")
	public static String getRealIP(String remoteAddr, Function<String, String> getHeader, String... otherHeaderNames) {
		String ip = null;
		List<String> headers = Stream.concat(Stream.of(DEFAULT_IP_HEADER), Stream.of(otherHeaderNames == null ? new String[0] : otherHeaderNames)).collect(Collectors.toList());
		for (String header : headers) {
			if ((ip = processIp(getHeader.apply(header))) != null) {
				break;
			}
		}
		return ip == null ? processIp(remoteAddr) : ip;
	}
	@Comment("根据IP地址获取地理位置信息\n\n"
			+ "1. 首先判断是否为内网IP，如果是则直接返回\"内网IP\"\n\n"
			+ "2. 对于外网IP，调用第三方API查询地理位置\n\n"
			+ "3. 如果出现异常，返回相应的默认值\n\n"
			+ "\n\n"
			+ "@param ip 需要查询的IP地址(IPv4格式)\n\n"
			+ "@return IP对应的地理位置信息，可能返回以下三种结果：\n\n"
			+ "        - \"内网IP\"：当IP为内网地址或格式非法时\n\n"
			+ "        - \"未知\"：当查询外部API失败时\n\n"
			+ "        - 具体地理位置：例如\"广东省深圳市 电信\"\n\n"
			+ "@throws 本方法已处理所有异常，不会向上抛出\n\n")
	public static String getAddress(String ip) {
		try {
			if (Ipv4Util.isInnerIP(ip)) {
				return "内网IP";
			}
			return JSONUtil.parseObj(HttpUtil.get("https://whois.pconline.com.cn/ipJson.jsp?json=true&ip=" + ip))
					.getStr("addr");
		} catch (IllegalArgumentException e) {
			return "内网IP";
		} catch (Exception e) {
			return "未知";
		}
	}

	private static String processIp(String ip) {
		if (ip != null) {
			ip = ip.trim();
			if (isUnknown(ip)) {
				return null;
			}
			if (ip.contains(",")) {
				String[] ips = ip.split(",");
				for (String subIp : ips) {
					ip = processIp(subIp);
					if (ip != null) {
						return ip;
					}
				}
			}
			return ip;
		}
		return null;
	}

	private static boolean isUnknown(String ip) {
		return StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim());
	}
}
