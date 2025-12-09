package org.ssssssss.magicapi.core.interceptor;

import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

public class BaseAuthorizationInterceptor implements AuthorizationInterceptor {
	
	/**
	 * 是否拥有页面按钮的权限
	 */
	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {

		// Authorization.DOWNLOAD 导出
		// Authorization.UPLOAD 上传
		// Authorization.PUSH 推送
		if(magicUser == null) {
			return false;
		}
		
		if(Authorization.DOWNLOAD.equals(authorization) ||
			Authorization.UPLOAD.equals(authorization) ||
			Authorization.PUSH.equals(authorization)
		) {
			if(magicUser.getUsername().equals("徐海洋")){
				return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * 是否拥有对接口的增删改查权限
	 */
	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization, MagicEntity entity) {
		// Authorization.SAVE 保存
		// Authorization.DELETE 删除
		// Authorization.VIEW 查询
		// Authorization.LOCK 锁定
		// Authorization.UNLOCK 解锁
		if(magicUser == null) {
			return false;
		}
		
		if(Authorization.VIEW.equals(authorization)) {
			return true;
		}
		
		
		if(Authorization.SAVE.equals(authorization) ||
			Authorization.DELETE.equals(authorization) ||
			Authorization.LOCK.equals(authorization) ||
			Authorization.UNLOCK.equals(authorization)
		) {
			if(
					(entity.getCreateBy() != null && entity.getCreateBy().equals(magicUser.getUsername())) ||
					(entity.getUpdateBy() != null && entity.getUpdateBy().equals(magicUser.getUsername()))
			){
				return true;
			}
			return false;
		}
		 
		return false;
	}
	
	/**
	 * 是否拥有对分组的增删改查权限
	 */
	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization, Group group) {
		// Authorization.SAVE 保存
		// Authorization.DELETE 删除
		// Authorization.VIEW 查询

		if(magicUser == null) {
			return false;
		}
		
		if(Authorization.VIEW.equals(authorization)) {
			return true;
		}
		 
		 
		if(Authorization.SAVE.equals(authorization) ||
				Authorization.DELETE.equals(authorization)
			) {
				if(
						(group.getCreateBy() != null && group.getCreateBy().equals(magicUser.getUsername())) ||
						(group.getUpdateBy() != null && group.getUpdateBy().equals(magicUser.getUsername()))
				){
					return true;
				}
				return false;
			}
		return false;
	}
 
}
