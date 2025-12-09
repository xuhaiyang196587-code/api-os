package org.ssssssss.magicapi.utils;

import java.security.Principal;
import java.util.Optional;

import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.script.annotation.Comment;

/**
 * Web相关工具类
 *
 * @author peter
 */
public class WebUtils {

	public static MagicRequestContextHolder magicRequestContextHolder;

	@Comment("获取当前登录用户名\n\n"
            + "从请求上下文中获取当前登录用户信息，支持两种获取方式：\n\n"
            + "1. 优先从MagicUser属性中获取\n\n"
            + "2. 回退到从Principal对象中获取\n\n"
            + "@return 当前用户名，未登录时返回null\n\n"
            + "@example\n"
            + "String username = WebUtils.currentUserName();\n"
            + "// 可能返回: \"admin\" 或 null")
	public static String currentUserName() {
		Optional<MagicHttpServletRequest> request = Optional.ofNullable(magicRequestContextHolder.getRequest());
		return request.map(r -> (MagicUser) r.getAttribute(Constants.ATTRIBUTE_MAGIC_USER))
				.map(MagicUser::getUsername)
				.orElseGet(() -> request.map(MagicHttpServletRequest::getUserPrincipal)
						.map(Principal::getName)
						.orElse(null)
				);
	}
}
