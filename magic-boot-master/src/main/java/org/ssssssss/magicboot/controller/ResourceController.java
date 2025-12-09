package org.ssssssss.magicboot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

@RestController
@RequestMapping("resources")
@Validated
public class ResourceController {
	@PostMapping("/dispatchApiDevUser")
	public Object dispatchApiDevUser(String type, String rootPath, String paths, String username) throws Exception {
		String script = """
import org.ssssssss.magicapi.task.model.TaskInfo
import org.ssssssss.magicapi.core.model.Option
import org.ssssssss.magicapi.core.model.ApiInfo
import org.ssssssss.magicapi.core.model.Group
import org.ssssssss.magicapi.core.service.MagicResourceService
import java.util.Arrays
import log 


var dispatchApiDevUser = (type, rootPath, paths, username) => {
    var sources = MagicResourceService.tree(type).children.filter(it => {
        return it.node.path == rootPath
    })

    sources.each(item => {
        var builder = (_item, path) => {
            var nodeId = _item.node.id;
            var nodePath = (path + _item.node.path).replaceAll("//", "/");
            var apiFiles = MagicResourceService.listFiles(nodeId)
            apiFiles.forEach(it => {
                if (paths.length == 1 && paths[0].equals("*")) {
                    // 更新当前接口的所有人
                    ApiInfo apiInfo = MagicResourceService.file(it.id);

                    apiInfo.setUpdateBy(username)
                    MagicResourceService.saveFile(apiInfo)
                } else if (paths.indexOf((nodePath + "/" + it.path).replaceAll("//", "/")) != -1) {
                    // 更新当前接口的所有人
                    ApiInfo apiInfo = MagicResourceService.file(it.id);
                    apiInfo.setUpdateBy(username)
                    MagicResourceService.saveFile(apiInfo)

                }

            })
            if (_item.children.size() > 0) {
				 Group group = MagicResourceService.getGroup(nodeId);
                 group.setUpdateBy(username)
                 MagicResourceService.saveGroup(group)
                _item.children.each(subItem => {
                    builder(subItem, nodePath + '/')
                })
            }
        }
        builder(item, '')
    })
}


dispatchApiDevUser(type, rootPath, Arrays.asList(paths.split(',')), username);
return 200			
		""";
		
		Map<String,Object> parms = new HashMap<String,Object>();
		parms.put("type", type);
		parms.put("rootPath", rootPath);
		parms.put("paths", paths);
		parms.put("username", username);
		MagicScriptContext context = new MagicScriptContext();
		context.putMapIntoContext(parms);
		return ScriptManager.executeScript(script, context);
	}
}
