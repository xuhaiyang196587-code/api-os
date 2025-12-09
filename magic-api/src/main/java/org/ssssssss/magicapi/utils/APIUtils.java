package org.ssssssss.magicapi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.Header;
import org.ssssssss.magicapi.core.model.Parameter;
import org.ssssssss.magicapi.core.model.TreeNode;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.core.service.MagicResourceService;

public class APIUtils {
	MagicAPIService apiService;
	MagicResourceService service;
	public APIUtils(MagicAPIService apiService, MagicResourceService service) {
		this.apiService = apiService;
		this.service = service;
	}
	
	private Map<String,List<Map<String,Object>>> getAPInfos() {
		List<ApiInfo> files = service.files("api");
		Map<String,List<Map<String,Object>>> apiMap = new HashMap<String,List<Map<String,Object>>>();
		for (ApiInfo apiInfo : files) {
			
			if(apiMap.get(apiInfo.getGroupId()) == null) {
				List<Map<String,Object>> list = new ArrayList<>();
				apiMap.put(apiInfo.getGroupId(), list);
			}
			Map<String,Object> info = new HashMap<String,Object>();
			info.put("id", apiInfo.getId());
			info.put("title", apiInfo.getName());
			info.put("pid", apiInfo.getGroupId());
			info.put("path", apiInfo.getPath());
			info.put("method", apiInfo.getMethod());
			info.put("createBy", apiInfo.getCreateBy());
			info.put("updateBy", apiInfo.getUpdateBy());
			
			Map<String,Object> args = new HashMap<String,Object>();
			
			List<Parameter> parameters = apiInfo.getParameters();
			Map<String,Object> postData = new HashMap<String,Object>();
			List<Map<String,Object>>params = new ArrayList<Map<String,Object>>();
			
			for (Parameter param : parameters) {
				Map<String,Object> paramMap = new HashMap<String,Object>();
				paramMap.put("name", param.getName());
				if(param.getDataType() != null) {
					paramMap.put("type", param.getDataType());
				}
				paramMap.put("value", param.getValue());
				params.add(paramMap);
			}
			
			postData.put("params", params);
			postData.put("allparams", parameters);
			
			List<Header> Headers = apiInfo.getHeaders();
			
			List<Map<String,Object>>headers = new ArrayList<Map<String,Object>>();
			
			for (Header header : Headers) {
				Map<String,Object> headerMap = new HashMap<String,Object>();
				headerMap.put("name", header.getName());
				if(header.getDataType() != null) {
					headerMap.put("type", header.getDataType());
				}
				headerMap.put("value", header.getValue());
				headers.add(headerMap);
			}
			
			args.put("headers", headers);
			args.put("postData", postData);
			info.put("args", args);
			
			apiMap.get(apiInfo.getGroupId()).add(info);
		}
		return apiMap;
	}
	
	private void getChildDir(List<TreeNode<Group>> subNode,String pid, String path, Map<String,Object> _rowObj, Map<String,List<Map<String,Object>>> apinfos) {
		for (TreeNode<Group> treeNode : subNode) {
			String id = "", spath = "";
			
			if(treeNode != null){
				Map<String,Object> rowObj = new HashMap<String,Object>();
				Group group = treeNode.getNode();
				id = group.getId();
				spath = group.getPath();
		        rowObj.put("id", id);
		        rowObj.put("title", group.getName());
		        rowObj.put("path", spath);
		        rowObj.put("pid", pid);
		        @SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) _rowObj.get("children");
		        children.add(rowObj);
		        if(apinfos.get(id) != null){
		        	List<Map<String, Object>> _children = new ArrayList<>();
		        	rowObj.put("children", _children);
		        	for (Map<String, Object> _map : apinfos.get(id)) {
		        		_map.put("path", path+"/"+spath+"/"+_map.get("path").toString());
		        		_children.add(_map);
					}
		        }
		        if(treeNode.getChildren().size() != 0){
		        	if(rowObj.get("children") == null) {
		        		List<Map<String, Object>> _children = new ArrayList<>();
			        	rowObj.put("children", _children);
		        	}
			    	getChildDir(treeNode.getChildren(),id, path+"/"+spath ,rowObj, apinfos);
			    }  
            }
		}
	}
	
	public List<Map<String,Object>> getApis(){
		Map<String,List<Map<String,Object>>> apinfos = getAPInfos();
		List<Map<String,Object>> dirMap = new ArrayList<Map<String,Object>>();
		TreeNode<Group> gs = service.tree("api");
		List<TreeNode<Group>> groups = gs.getChildren();
		for (TreeNode<Group> treeNode : groups) {
			String id = "", path = "";
		    if(treeNode != null){
		    	Group group = treeNode.getNode();
		    	Map<String,Object> rowObj = new HashMap<String,Object>();
		        id = group.getId();
		        path = group.getPath();
		        rowObj.put("id", id);
		        rowObj.put("title", group.getName());
		        rowObj.put("path", path);
		        rowObj.put("pid", "0");
		        dirMap.add(rowObj);
		        
		        if(apinfos.get(id) != null){
		        	List<Map<String, Object>> children = new ArrayList<>();
		        	rowObj.put("children", children);
		        	for (Map<String, Object> _map : apinfos.get(id)) {
		        		_map.put("path", group.getPath()+"/"+_map.get("path").toString());
		        		children.add(_map);
					}
		        }
		        
		        if(treeNode.getChildren().size() != 0){
		        	if(rowObj.get("children") == null) {
		        		List<Map<String, Object>> children = new ArrayList<>();
			        	rowObj.put("children", children);
		        	}
			    	getChildDir(treeNode.getChildren(),id,path,rowObj,apinfos);
			    }  
		    }
		   
		}
		
		return dirMap;
	}
	
	@SuppressWarnings("unchecked")
	public void exeApiLeaf(Map<String, Object> api) throws Exception {
		List<Map<String,Object>> children = (List<Map<String, Object>>) api.get("children");
		if(children != null) {
			for (Map<String, Object> _api : children) {
				exeApiLeaf(_api);
			}
		}else {
			System.out.printf(
					"\n\t服务启动即刻执行的接口: \t\t%s\n"
					, api.get("path")
			);
			Map<String, Object> params = new HashMap<>();
			Object obj = api.get("args");
			if(obj != null) {
				Map<String,Object> argsMap = (Map<String,Object> ) obj;
//				Object headers = argsMap.get("headers");
				Object postDatas = argsMap.get("postData");
//				if(headers != null) {
//					List<Map<String,Object>> headerMap = (List<Map<String,Object>> ) headers;
//				}
				if(postDatas != null) {
					Map<String,Object> postDataMap = (Map<String,Object>) postDatas;
					if(postDataMap.size() > 0) {
						List<Parameter> parameters = (List<Parameter>)postDataMap.get("allparams");
						for (Parameter parameter : parameters) {
							String name = parameter.getName();
							Object value = parameter.getValue();
							String defaultValue = parameter.getDefaultValue();
							if(value == null) {
								value = defaultValue;
							}
							params.put(name, value);
						}
					}
				}
			}
			// 内部调用接口不包含code以及message信息，同时也不走拦截器。
			apiService.execute(api.get("method").toString(), "/"+api.get("path").toString(), params);
		}
    }

}
