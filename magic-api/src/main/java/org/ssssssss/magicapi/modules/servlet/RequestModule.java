package org.ssssssss.magicapi.modules.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.modules.servlet.utils.RequestGlobal;
import org.ssssssss.magicapi.utils.IpUtils;
import org.ssssssss.magicapi.utils.JsonUtils;
import org.ssssssss.script.annotation.Comment;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;


/**
 * request 模块
 *
 * @author mxd
 */
@MagicModule("request")
public class RequestModule {

	private static MagicRequestContextHolder magicRequestContextHolder;
	public static MagicAPIService service;

	public RequestModule(MagicRequestContextHolder magicRequestContextHolder,MagicAPIService service) {
		RequestModule.magicRequestContextHolder = magicRequestContextHolder;
		RequestModule.service = service;
	}

	public static MagicResourceService service() {
		return MagicConfiguration.getMagicResourceService();
	}
	@Comment("执行函数")
	public static Object invokeFunction(
			@Comment(name = "path", value = "/路径/xxx/函数名称") String path,
			@Comment(name = "params", value = "{Sring:Object}") Map<String, Object> params) {
		return service.invoke(path, params);
	}
	@Comment("执行API接口")
	public static Object invokeApi(
			@Comment(name = "method", value = "GET/POST/PUT/DELETE/...") String method,
			@Comment(name = "path", value = "/路径/xxx/函数名称") String path,
			@Comment(name = "params", value = "{Sring:Object}") Map<String, Object> params) {
		return service.execute(method, path, params);
	}
	
	/**
	 * 获取文件信息
	 *
	 * @param name 参数名
	 */
	@Comment("获取文件")
	public static MultipartFile getFile(@Comment(name = "name", value = "参数名") String name) {
		MultipartRequest request = getMultipartHttpServletRequest();
		if (request == null) {
			return null;
		}
		MultipartFile file = request.getFile(name);
		return file == null || file.isEmpty() ? null : file;
	}

	/**
	 * 获取文件信息
	 *
	 * @param name 参数名
	 */
	@Comment("获取多个文件")
	public static List<MultipartFile> getFiles(@Comment(name = "name", value = "参数名") String name) {
		MultipartRequest request = getMultipartHttpServletRequest();
		if (request == null) {
			return null;
		}
		return request.getFiles(name).stream().filter(it -> !it.isEmpty()).collect(Collectors.toList());
	}
	
	@Comment("自动上传并保存提交的全部附件\n\n"
			+ "@Result 例子数据：{'字段名称':'[{\"original\":\"4文本生成模型.pdf\",\"size\":\"2240513\",\"name\":\"files\",\"state\":\"SUCCESS\",\"type\":\"pdf\",\"url\":\"userfiles/2025-06-02/7810f759ed2b47238347b2cc8988f091/4文本生成模型.pdf\"}]',...}")
	public static Map<String,String> uploadFiles() throws Exception {
		MultipartRequest request = getMultipartHttpServletRequest();
		if(request == null) {
			return new HashMap<String,String>();
		}
		Iterator<String> names = request.getFileNames();
		 Map<String,String> result = new  HashMap<>();
		while (names.hasNext()) {
		    String name = names.next();  // 获取当前文件字段名
        	List<Map<String, String>> infos = new ArrayList<Map<String, String>>();
        	List<MultipartFile> files = getFiles(name);
        	for (MultipartFile file : files) {
        		Map<String, String> info = saveFile(file);
        		infos.add(info);
    		}
        	result.put(name, JsonUtils.toJsonStringWithoutPretty(infos));
		}
        return result;
	}
	
	@Comment("上传并保存提交的指定附件\n\n"
			+ "@Result 例子数据：{\"original\":\"4文本生成模型.pdf\",\"size\":\"2240513\",\"name\":\"files\",\"state\":\"SUCCESS\",\"type\":\"pdf\",\"url\":\"userfiles/2025-06-02/7810f759ed2b47238347b2cc8988f091/4文本生成模型.pdf\"}")
	public static Map<String,String> uploadFile(@Comment(name = "name", value = "参数名") String name) throws Exception {
		return saveFile(getFile(name));
	}
	
	@Comment("获取附件存储的基础目录\n\n")
	public static String getUploadFileBaseDir() throws Exception {
		return RequestGlobal.getUserFilesBaseDir();
	}
	
	
	@Comment("获取MagicHttpServletRequest对象")
	public static MagicHttpServletRequest get() {
		return magicRequestContextHolder.getRequest();
	}
	
	@Comment("获取原生HttpServletRequest对象")
	public static HttpServletRequest getHttpServletRequest() {
		return (HttpServletRequest) magicRequestContextHolder.getRequest().getRequest();
	}
	
	 @Comment("获取当前应用上下文URL\n\n"
	            + "构建完整的应用根URL，格式为：协议://域名:端口/上下文路径/\n\n"
	            + "@return 完整的应用上下文URL，以斜杠结尾\n\n"
	            + "@example\n"
	            + "String contextUrl = request.getContextUrl();\n"
	            + "// 可能返回: \"http://localhost:8080/\"")
	public static String getContextUrl() {
		HttpServletRequest request = getHttpServletRequest();
		StringBuffer url = request.getRequestURL();
		return url.delete(url.length() - request.getRequestURI().length(), url.length())
				.append(request.getServletContext().getContextPath()).append("/").toString();
	}

	/**
	 * 根据参数名获取参数值集合
	 *
	 * @param name 参数名
	 */
	@Comment("根据请求参数名获取值")
	public List<String> getValues(@Comment(name = "name", value = "参数名") String name) {
		MagicHttpServletRequest request = get();
		if (request != null) {
			String[] values = request.getParameterValues(name);
			return values == null ? null : Arrays.asList(values);
		}
		return null;
	}

	/**
	 * 根据header名获取header集合
	 *
	 * @param name 参数名
	 */
	@Comment("根据header名获取值")
	public List<String> getHeaders(@Comment(name = "name", value = "header名") String name) {
		MagicHttpServletRequest request = get();
		if (request != null) {
			Enumeration<String> headers = request.getHeaders(name);
			return headers == null ? null : Collections.list(headers);
		}
		return null;
	}

	@Comment("获取客户端IP")
	public String getClientIP(String... otherHeaderNames) {
		MagicHttpServletRequest request = get();
		if (request == null) {
			return null;
		}
		return IpUtils.getRealIP(request.getRemoteAddr(), request::getHeader, otherHeaderNames);
	}
	
	@SuppressWarnings("unchecked")
	@Comment("获取请求的全部参数\n\n" + "@param request 可以通过(HttpServletRequest) RequestModule.get().getRquest() 获得\n\n"
			+ "@return {aa:xxx,bb:xxx,...}\n\n")
	public Map<String, String> toMap() throws Exception {
		HttpServletRequest request = (HttpServletRequest) get().getRequest();
		Map<String, String> params = null;
		String contentType = request.getContentType();
		if ((contentType == null || contentType.contains("form-data") || contentType.contains("application/json")
				|| contentType.contains("x-www-form-urlencoded"))) {

			params = new HashMap<String, String>();
			Enumeration<?> paramNames = request.getParameterNames();
			while (paramNames != null && paramNames.hasMoreElements()) {
				String paramName = (String) paramNames.nextElement();
				String value = request.getParameter(paramName).replaceAll("'", "′");
				if (request.getParameterValues(paramName).length > 1) {
					value = array2String(request.getParameterValues(paramName), ",");
				}
				params.put(paramName, value);
			}
		} else {
			int contentLength = request.getContentLength();
			if (contentLength < 0) {
				return null;
			}
			byte buffer[] = new byte[contentLength];
			for (int i = 0; i < contentLength;) {
				int readlen = request.getInputStream().read(buffer, i, contentLength - i);
				if (readlen == -1) {
					break;
				}
				i += readlen;
			}

			String charEncoding = request.getCharacterEncoding();
			if (charEncoding == null) {
				charEncoding = "UTF-8";
			}
			try {
				params = (Map<String, String>) string2Map(new String(buffer, charEncoding).replaceAll("'", "′"));
			} catch (Exception e) {
				String ps[] = new String(buffer, charEncoding).split("&");
				params = new HashMap<String, String>();
				for (String param : ps) {
					String arg[] = param.split("=");
					params.put(arg[0], arg[1]);
				}
			}
		}
		return params;
	}
	
	private String array2String(String[] array, String flag) {
		StringBuilder sb = new StringBuilder();
		if (array != null && array.length > 0) {
			for (int i = 0; i < array.length; i++) {
				if (i < array.length - 1) {
					sb.append(array[i].replaceAll("'", "′") + flag);
				} else {
					sb.append(array[i].replaceAll("'", "′"));
				}
			}
		}
		return sb.toString();
	}

	/***
	 * 将json字符串转换成Map对象
	 * 
	 * @param str {"name":"peter","sex":"男"}
	 * @return Map<?,?> 集合对象
	 * @throws Exception
	 */
	private Map<?, ?> string2Map(String str) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper.readValue(str.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", ""), Map.class);
	}
	
	private static MultipartRequest getMultipartHttpServletRequest() {
		MagicHttpServletRequest request = get();
		if (request != null && request.isMultipart()) {
			return request.resolveMultipart();
		}
		return null;
	}
	
	private static Map<String, String> createFileAttr(String originalFilename){
        String ret = RequestGlobal.USER_FILES_BASE_URL + DateUtil.today() + "/" + IdUtil.simpleUUID() + "/";
        String suffix = FileUtil.getSuffix(originalFilename);
        Map<String, String> map = new HashMap<>();
        map.put("ret", ret);
        map.put("filePath", ret.substring(1) + originalFilename);
        map.put("fileNames", originalFilename);
        map.put("suffix", suffix);
        return map;
    }

    private static Map<String, String> saveFile(MultipartFile file) {
    	if(file == null) {
			return new HashMap<String,String>();
		}
        Map<String, String> fileAttr = createFileAttr(file.getOriginalFilename());
        String fileNames = fileAttr.get("fileNames");
        String ret = fileAttr.get("ret");
        String suffix = fileAttr.get("suffix");
        String realPath = RequestGlobal.getUserFilesBaseDir() + ret;
        FileUtil.mkdir(FileUtil.normalize(realPath));
        File tempFile = new File(realPath + fileNames);
        if (!tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdir();
        }
        Map<String, String> params = new HashMap<>();
		params.put("state", "SUCCESS");
		params.put("original", fileNames);
		params.put("name", file.getName());
		params.put("size", file.getSize() + "");
      	params.put("type", suffix);
      	params.put("url", fileAttr.get("filePath"));
        try {
            if (!tempFile.exists()) {
                file.transferTo(tempFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            params.put("state", "ERROR");
            FileUtil.del(FileUtil.normalize(realPath));
        }
      	return params;
	}
}
