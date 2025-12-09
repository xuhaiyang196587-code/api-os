package org.ssssssss.magicboot.interceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.interceptor.BaseAuthorizationInterceptor;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.magicboot.model.UserCacheMap;
import org.ssssssss.script.MagicScriptContext;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;

@Component
public class SystemAuthorizationInterceptor extends BaseAuthorizationInterceptor {// implements AuthorizationInterceptor {

	 @Autowired
	 private JdbcTemplate template;
	/**
     * 配置是否需要登录
	 */
	@Override
	public boolean requireLogin() {
		return true;
	}
	
	@Override
	public MagicUser getUserByToken(String token) throws MagicLoginException {
//		HttpServletRequest request = 
//	            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//	                .getRequest();
//		System.out.println(request.getRequestURL().toString()+"======"+ StpUtil.isLogin() +"--token:--"+token);
			
		
		boolean islogin = true;	
			try {
				islogin = StpUtil.isLogin();
			} catch (Exception e) {
			}
			String userId = token+"_auth";
			if(islogin){
				Map<String, Object> userInfo = UserCacheMap.get(userId);
				if(userInfo == null) {
					//System.out.println("=========动态查询用户信息过于频繁=============");
					userInfo = template.queryForMap("select name from sys_user where id=?", token);
					UserCacheMap.put(userId,userInfo);
				}
		        return new MagicUser(userId,userInfo.get("name").toString(),token);
	        }
			UserCacheMap.remove(userId);
			throw new MagicLoginException("token 无效");
	}

	
	@Override
	public MagicUser login(String username, String password) throws MagicLoginException {
		
		String script = """
				import log
				import 'cn.dev33.satoken.stp.StpUtil';
				import '@/configure/getBykey' as configure;
				import request;
				import org.ssssssss.magicboot.model.CodeCacheMap
				import org.ssssssss.magicboot.model.UserCacheMap
				import cn.hutool.http.useragent.UserAgentUtil
				import cn.hutool.http.useragent.UserAgent
				import cn.dev33.satoken.secure.SaSecureUtil
				import org.ssssssss.magicapi.utils.IpUtils
				
				UserAgent ua = UserAgentUtil.parse(request.getHeaders("User-Agent")[0])
				if(configure('verification-code.enable') == 'true'){
				    if(!body.code){
				        exit 0, '请输入验证码'
				    }else if(body.code != CodeCacheMap.get(body.uuid)){
				        exit 0, '验证码错误'
				    }
				}
				
				var user
				if(SaSecureUtil.sha256(configure('super-password')) == body.password){
				    user = db.table("sys_user").where().eq("username",body.username).eq('is_del', 0).selectOne()
				}else{
				    user = db.table("sys_user").where().eq("username",body.username).eq("password", body.password).eq('is_del', 0).selectOne()
				}
				
				var loginLog = {
				    username: body.username,
				    type: '成功',
				    ip: request.getClientIP(),
				    browser: ua.getBrowser().toString(),
				    os: ua.getOs().toString(),
				    address: IpUtils.getAddress(request.getClientIP())
				}
				 
				if(!user){
				    loginLog.failPassword = body.password
				    loginLog.type = '失败'
				    db.table("sys_login_log").primary("id").save(loginLog);
				    exit 0,'用户名或密码错误'
				}
				
				if(user.isLogin == '1'){
				    exit 0, '此账号禁止登录'
				}
				
				StpUtil.login(user.id)
				var token = StpUtil.getTokenValueByLoginId(user.id)
				loginLog.token = token
				db.table("sys_login_log").primary("id").save(loginLog);
				UserCacheMap.put(user.id,loginLog);
				CodeCacheMap.remove(body.uuid)
				return user.name
				
		""";
		Map<String,Object> body = new HashMap<String,Object>();
		Map<String,Object> parms = new HashMap<String,Object>();
		parms.put("username", username);
		parms.put("password", SaSecureUtil.sha256(password) );
		parms.put("code", "");
		
		body.put("body", parms);
		MagicScriptContext context = new MagicScriptContext();
		context.putMapIntoContext(body);
		Object resultVal = ScriptManager.executeScript(script, context);
		
		if (resultVal instanceof String) {
			// 登录成功后 构造MagicUser对象。
			return new MagicUser(StpUtil.getLoginId().toString(),resultVal.toString(),StpUtil.getLoginId().toString());
	    } else if (resultVal instanceof List) {
	        // 处理List逻辑
	        List<?> list = (List<?>) resultVal;
	        throw new MagicLoginException(list.get(1).toString());
	    } else {
	    	throw new MagicLoginException("用户名或密码错误！");
	    }
	}
}