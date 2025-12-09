package org.ssssssss.magicapi.utils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.script.annotation.Comment;
import org.ssssssss.script.reflection.JavaReflection;

public class Mapping {

	private final AbstractHandlerMethodMapping<RequestMappingInfo> methodMapping;

	private final String base;

	private final RequestMappingInfo.BuilderConfiguration config;

	private static final boolean HAS_GET_PATTERN_PARSER = JavaReflection.getMethod(RequestMappingHandlerMapping.class, "getPatternParser") != null;

	private Mapping(AbstractHandlerMethodMapping<RequestMappingInfo> methodMapping, RequestMappingInfo.BuilderConfiguration config, String base) {
		this.methodMapping = methodMapping;
		this.config = config;
		this.base = StringUtils.defaultIfBlank(base, "");
	}

	@Comment("创建Mapping实例的工厂方法（使用默认基础路径）\n\n"
			+ "@param mapping 请求映射处理器\n\n"
			+ "@return 新的Mapping实例\n\n"
			+ "示例: Mapping.create(requestMappingHandlerMapping)")
	public static Mapping create(RequestMappingHandlerMapping mapping) {
		return create(mapping, null);
	}
	@SuppressWarnings("deprecation")
	@Comment("创建Mapping实例的工厂方法（可指定基础路径）\n\n"
			+ "@param mapping 请求映射处理器\n\n"
			+ "@param base 要使用的基础路径\n\n"
			+ "@return 新的Mapping实例\n\n"
			+ "示例: Mapping.create(requestMappingHandlerMapping, \"/api\")")
	public static Mapping create(RequestMappingHandlerMapping mapping, String base) {
		if (HAS_GET_PATTERN_PARSER) {
			RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
			config.setTrailingSlashMatch(mapping.useTrailingSlashMatch());
			config.setContentNegotiationManager(mapping.getContentNegotiationManager());
			if (mapping.getPatternParser() != null) {
				config.setPatternParser(mapping.getPatternParser());
			} else {
				config.setPathMatcher(mapping.getPathMatcher());
			}
			return new Mapping(mapping, config, base);
		}
		return new Mapping(mapping, null, base);
	}
	@Comment("构建请求路径映射信息\n\n"
			+ "@param paths 要映射的路径数组\n\n"
			+ "@return RequestMappingInfo构建器\n\n"
			+ "示例: mapping.paths(\"/user\", \"/profile\")")
	public RequestMappingInfo.Builder paths(String ... paths){
		RequestMappingInfo.Builder builder = RequestMappingInfo.paths(paths);
		if(this.config != null){
			return builder.options(this.config);
		}
		return builder;
	}
	@Comment("注册方法映射\n\n"
			+ "@param requestMappingInfo 请求映射信息\n\n"
			+ "@param handler 处理对象\n\n"
			+ "@param method 处理方法\n\n"
			+ "@return 当前Mapping实例（支持链式调用）\n\n"
			+ "示例: mapping.register(info, controller, controller.getClass().getMethod(\"handler\"))")
	public Mapping register(RequestMappingInfo requestMappingInfo, Object handler, Method method) {
		this.methodMapping.registerMapping(requestMappingInfo, handler, method);
		return this;
	}
	@Comment("便捷方法：注册带有HTTP方法的路径映射\n\n"
			+ "@param requestMethod HTTP请求方法（如GET、POST等）\n\n"
			+ "@param path 请求路径\n\n"
			+ "@param handler 处理对象\n\n"
			+ "@param method 处理方法\n\n"
			+ "@return 创建的RequestMappingInfo\n\n"
			+ "示例: mapping.register(\"GET\", \"/user\", controller, method)")
	public RequestMappingInfo register(String requestMethod, String path, Object handler, Method method) {
		RequestMappingInfo info = paths(path).methods(RequestMethod.valueOf(requestMethod.toUpperCase())).build();
		register(info, handler, method);
		return info;
	}
	@Comment("获取所有已注册的处理器方法\n\n"
			+ "@return 请求映射信息到处理器方法的映射\n\n"
			+ "示例: Map<RequestMappingInfo, HandlerMethod> methods = mapping.getHandlerMethods()")
	public Map<RequestMappingInfo, HandlerMethod> getHandlerMethods() {
		return this.methodMapping.getHandlerMethods();
	}
	@Comment("取消注册指定的请求映射\n\n"
			+ "@param info 要取消的请求映射信息\n\n"
			+ "@return 当前Mapping实例（支持链式调用）\n\n"
			+ "示例: mapping.unregister(info)")
	public Mapping unregister(RequestMappingInfo info) {
		this.methodMapping.unregisterMapping(info);
		return this;
	}
	@Comment("自动注册控制器类中的所有@RequestMapping方法\n\n"
			+ "@param target 控制器对象\n\n"
			+ "@return 当前Mapping实例（支持链式调用）\n\n"
			+ "示例: mapping.registerController(myController)")
	public Mapping registerController(Object target) {
		Method[] methods = target.getClass().getDeclaredMethods();
		for (Method method : methods) {
			RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (requestMapping != null) {
				String[] paths = Stream.of(requestMapping.value()).map(value -> PathUtils.replaceSlash(base + value)).toArray(String[]::new);
				this.register(paths(paths).methods(requestMapping.method()).build(), target, method);
			}
		}
		return this;
	}
	
	 public static void main(String[] args) throws Exception {
	        // 1. 创建 RequestMappingHandlerMapping (Spring MVC的核心组件)
	        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
	        handlerMapping.afterPropertiesSet(); // 初始化

	        // 2. 测试 create() 方法
	        //    创建带和不带基础路径的 Mapping 实例
	        //    /api 基础路径会影响后续注册的控制器路径
	        System.out.println("=== 测试 create() 方法 ===");
	        Mapping mapping1 = Mapping.create(handlerMapping);
	        Mapping mapping2 = Mapping.create(handlerMapping, "/api");
	        System.out.println("创建无base路径的Mapping实例: " + mapping1);
	        System.out.println("创建带base路径的Mapping实例: " + mapping2);

	        // 3. 测试 paths() 方法
	        //    演示如何构建多路径的请求映射
	        System.out.println("\n=== 测试 paths() 方法 ===");
	        RequestMappingInfo.Builder builder = mapping1.paths("/test", "/demo");
	        System.out.println("构建的路径: " + builder.build().getPatternsCondition());

	        // 4. 测试 register() 方法
	        //    手动注册一个控制器方法
	        //    需要构建完整的 RequestMappingInfo
	        System.out.println("\n=== 测试 register() 方法 ===");
	        TestController controller = new TestController();
	        Method helloMethod = TestController.class.getMethod("hello");
	        RequestMappingInfo info = RequestMappingInfo.paths("/hello").methods(RequestMethod.GET).build();
	        
	        mapping1.register(info, controller, helloMethod);
	        System.out.println("注册了 /hello GET 方法");

	        // 5. 测试 register(String, String, Object, Method) 方法
	        //    更简便的注册方式，直接指定HTTP方法和路径
	        System.out.println("\n=== 测试 register(String, String, Object, Method) 方法 ===");
	        Method worldMethod = TestController.class.getMethod("world");
	        RequestMappingInfo worldInfo = mapping1.register("POST", "/world", controller, worldMethod);
	        System.out.println("注册了 /world POST 方法: " + worldInfo);

	        // 6. 测试 getHandlerMethods() 方法
	        //    获取所有已注册的处理器方法
	        //    输出格式: 映射路径 -> 处理方法
	        System.out.println("\n=== 测试 getHandlerMethods() 方法 ===");
	        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping1.getHandlerMethods();
	        handlerMethods.forEach((key, value) -> {
	            System.out.println("映射路径: " + key.getPatternsCondition() + 
	                             ", 方法: " + value.getMethod().getName());
	        });

	        // 7. 测试 unregister() 方法
	        //    演示如何取消已注册的映射 取消后检查剩余映射
	        System.out.println("\n=== 测试 unregister() 方法 ===");
	        mapping1.unregister(info);
	        System.out.println("取消注册 /hello 路径后的处理器方法:");
	        mapping1.getHandlerMethods().forEach((k, v) -> 
	            System.out.println("剩余路径: " + k.getPatternsCondition())
	        );

	        // 8. 测试 registerController() 方法
	        //    自动扫描控制器类中的所有 @RequestMapping 方法
	        //    特别注意基础路径 /api 会自动附加到所有路径前
	        System.out.println("\n=== 测试 registerController() 方法 ===");
	        mapping2.registerController(controller);
	        System.out.println("自动注册控制器后的映射:");
	        mapping2.getHandlerMethods().forEach((k, v) -> {
	            System.out.println("路径: " + k.getPatternsCondition() + 
	                             ", 方法: " + v.getMethod().getName() + 
	                             ", HTTP方法: " + k.getMethodsCondition());
	        });
	    }

	    // 测试控制器类
	    public static class TestController {
	        @org.springframework.web.bind.annotation.RequestMapping(value = "/hello", method = RequestMethod.GET)
	        public String hello() {
	            return "Hello";
	        }

	        @org.springframework.web.bind.annotation.RequestMapping(value = "/world", method = RequestMethod.POST)
	        public String world() {
	            return "World";
	        }

	        @org.springframework.web.bind.annotation.RequestMapping("/auto")
	        public String autoRegistered() {
	            return "Auto Registered";
	        }
	    }
}
