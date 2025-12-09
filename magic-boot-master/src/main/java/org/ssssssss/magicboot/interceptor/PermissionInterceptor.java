package org.ssssssss.magicboot.interceptor;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.utils.PathUtils;
import org.ssssssss.magicboot.model.StatusCode;
import org.ssssssss.script.MagicScriptContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;

@Component
@Order(1)
public class PermissionInterceptor implements RequestInterceptor, HandlerInterceptor {

    @Autowired
    MagicAPIService magicAPIService;

    @Autowired
    MagicResourceService magicResourceService;

    @Autowired
    private JdbcTemplate template;

    /*
     * 当返回对象时，直接将此对象返回到页面，返回null时，继续执行后续操作
     */
    @Override
    public Object preHandle(ApiInfo info, MagicScriptContext context, MagicHttpServletRequest request, MagicHttpServletResponse response) {
        String requireLogin = Objects.toString(info.getOptionValue(Options.REQUIRE_LOGIN), "");
        if(requireLogin.equals("false")){
            return null;
        }
        if(!StpUtil.isLogin()){
            return StatusCode.CERTIFICATE_EXPIRED.json();
        } else {
            // TODO
            @SuppressWarnings("unchecked")
			List<String> permissions = (List<String>) magicAPIService.execute("post", "/system/security/permissions", new HashMap<String, Object>());
            String permission = Objects.toString(info.getOptionValue(Options.PERMISSION), "");
            if (StringUtils.isNotBlank(permission) && !permissions.contains(permission)) {
                return StatusCode.FORBIDDEN.json();
            }
        }
        return null;
    }

    @Override
    public Object postHandle(RequestEntity requestEntity, Object returnValue) throws Exception {
        if(StpUtil.isLogin()){
            try {
                MagicHttpServletRequest request = requestEntity.getRequest();
                Map<String,String> queryData = toMap((HttpServletRequest) request.getRequest());
                ObjectMapper objectMapper = new ObjectMapper();
//        		objectMapper.setSerializationInclusion(Include.NON_NULL);
                String queryDataStr = objectMapper.writeValueAsString(queryData);
                ApiInfo info = requestEntity.getApiInfo();
                template.update("insert into sys_oper_log(api_name, api_path, api_method, cost_time, create_by, create_date, user_agent, user_ip, data) values(?,?,?,?,?,?,?,?,?)",
//                    PathUtils.replaceSlash(groupServiceProvider.getFullName(info.getGroupId()) + "/" + info.getName()).replace("/","-"),
                        PathUtils.replaceSlash(String.format("/%s/%s", magicResourceService.getGroupName(info.getGroupId()), info.getName())),
                        request.getRequestURI(),
                        request.getMethod(),
                        System.currentTimeMillis() - requestEntity.getRequestTime(),
                        StpUtil.getLoginId(),
                        new Date(requestEntity.getRequestTime()),
                        request.getHeader("User-Agent"),
                        request.getRemoteAddr(),
                        queryDataStr);
            } catch (Exception ignored){
                ignored.printStackTrace();
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
	public Map<String, String> toMap(HttpServletRequest request) throws Exception {
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

}
