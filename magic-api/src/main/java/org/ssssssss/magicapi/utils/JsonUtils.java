package org.ssssssss.magicapi.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.script.annotation.Comment;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * JSON工具包
 *
 * @author peter
 */
public class JsonUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

	static {
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(Logger.class, new JsonSerializer<Logger>() {
			@Override
			public void serialize(Logger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				gen.writeString(value.toString());
			}
		});
		MAPPER.registerModule(simpleModule);
	}
	@Comment("将对象转换为格式化的JSON字符串\n\n"
			+ "@param target 要转换的对象\n\n"
			+ "@return 格式化后的JSON字符串，转换失败返回null\n\n"
			+ "@example String json = JsonUtils.toJsonString(user);")
	public static String toJsonString(Object target) {
		try {
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(target);
		} catch (JsonProcessingException e) {
			logger.error("json序列化失败", e);
			return null;
		}
	}
	@Comment("将对象转换为非格式化的JSON字符串\n\n"
			+ "@param target 要转换的对象\n\n"
			+ "@return 非格式化的JSON字符串，转换失败返回null\n\n"
			+ "@example String json = JsonUtils.toJsonStringWithoutPretty(user);")
	public static String toJsonStringWithoutPretty(Object target) {
		try {
			return MAPPER.writeValueAsString(target);
		} catch (JsonProcessingException e) {
			logger.error("json序列化失败", e);
			return null;
		}
	}
	@Comment("将对象转换为JSON字符串（不记录错误日志）\n\n"
			+ "@param target 要转换的对象\n\n"
			+ "@return JSON字符串，转换失败返回对象的toString()结果\n\n"
			+ "@example String json = JsonUtils.toJsonStringWithoutLog(user);")
	public static String toJsonStringWithoutLog(Object target) {
		try {
			return MAPPER.writeValueAsString(target);
		} catch (Exception e) {
			return target == null ? null : target.toString();
		}
	}
	@Comment("将JSON字符串转换为指定类型的对象\n\n"
			+ "@param json JSON字符串\n\n"
			+ "@param typeReference 目标类型引用\n\n"
			+ "@return 转换后的对象，转换失败返回null\n\n"
			+ "@example User user = JsonUtils.readValue(json, new TypeReference<User>(){});")
	public static <T> T readValue(String json, TypeReference<T> typeReference) {
		try {
			return MAPPER.readValue(json, typeReference);
		} catch (IOException e) {
			logger.error("读取json失败,json:{}", json, e);
			return null;
		}
	}
	@Comment("将JSON字符串转换为指定类的对象\n\n"
			+ "@param json JSON字符串\n\n"
			+ "@param clazz 目标类\n\n"
			+ "@return 转换后的对象，转换失败返回null\n\n"
			+ "@example User user = JsonUtils.readValue(json, User.class);")
	public static <T> T readValue(String json, Class<T> clazz) {
		try {
			return MAPPER.readValue(json, clazz);
		} catch (IOException e) {
	        logger.error("读取json失败,json:{}", json, e);
	        return null;
		}
	}
	@Comment("将字节数组形式的JSON转换为指定类的对象\n\n"
			+ "@param bytes JSON字节数组\n\n"
			+ "@param clazz 目标类\n\n"
			+ "@return 转换后的对象，转换失败返回null\n\n"
			+ "@example User user = JsonUtils.readValue(jsonBytes, User.class);")
	public static <T> T readValue(byte[] bytes, Class<T> clazz) {
		try {
			return MAPPER.readValue(bytes, clazz);
		} catch (IOException e) {
			logger.error("读取json失败,json:{}", new String(bytes), e);
			return null;
		}
	}
	@Comment("将字节数组形式的JSON转换为指定Java类型的对象\n\n"
			+ "@param bytes JSON字节数组\n\n"
			+ "@param javaType 目标Java类型\n\n"
			+ "@return 转换后的对象，转换失败返回null\n\n"
			+ "@example List<User> users = JsonUtils.readValue(jsonBytes, new TypeFactory().constructCollectionType(List.class, User.class));")
	public static <T> T readValue(byte[] bytes, JavaType javaType) {
		try {
			return MAPPER.readValue(bytes, javaType);
		} catch (IOException e) {
			logger.error("读取json失败,json:{}", new String(bytes), e);
			return null;
		}
	}
	@Comment("将对象转换为JSON字节数组\n\n"
			+ "@param target 要转换的对象\n\n"
			+ "@return JSON字节数组，转换失败返回空数组\n\n"
			+ "@example byte[] jsonBytes = JsonUtils.toJsonBytes(user);")
	public static byte[] toJsonBytes(Object target) {
		String json = toJsonString(target);
		return json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
	}

}
