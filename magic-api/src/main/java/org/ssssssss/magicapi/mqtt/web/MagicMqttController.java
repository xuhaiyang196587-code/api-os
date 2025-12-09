package org.ssssssss.magicapi.mqtt.web;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.mqtt.model.MqttInfo;
import org.ssssssss.magicapi.mqtt.util.MqttDataSource;

public class MagicMqttController extends MagicController implements MagicExceptionHandler {

	public MagicMqttController(MagicConfiguration configuration) {
		super(configuration);
	}

	@RequestMapping("/mqtt/jdbc/test")
	@ResponseBody
	public JsonBean<String> test(@RequestBody MqttInfo properties) {
		try {
			MqttDataSource mqttDataSource = new MqttDataSource(properties,true);
			if(mqttDataSource.test()) {
				return new JsonBean<>("ok");
			}
			return new JsonBean<>("连接超时！");
		} catch (Exception e) {
			return new JsonBean<>(e.getMessage());
		}
	}
	
}
