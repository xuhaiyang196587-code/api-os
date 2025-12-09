package org.ssssssss.magicapi.mqtt.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.mqtt.model.MqttInfo;
import org.ssssssss.magicapi.utils.JsonUtils;

public class MqttMagicResourceStorage implements MagicResourceStorage<MqttInfo>, JsonCodeConstants {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "mqtt";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<MqttInfo> magicClass() {
		return MqttInfo.class;
	}

	@Override
	public boolean requirePath() {
		return false;
	}

	@Override
	public boolean requiredScript() {
		return false;
	}

	@Override
	public boolean allowRoot() {
		return true;
	}

	@Override
	public String buildMappingKey(MqttInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(MqttInfo entity) {
		notBlank(entity.getKey(), DS_KEY_REQUIRED);
		notNull(entity.getProperties(), new JsonCode(1020, "mqtt propertie 不能为空"));
		
		Map<String, Object> mqtt = new HashMap<>(entity.getProperties());
	 
		try {
			mqtt.get("broker").toString();
			mqtt.get("username").toString();
			mqtt.get("password").toString();
			Integer.parseInt(mqtt.get("queueSize").toString());
			Integer.parseInt(mqtt.get("qos").toString());
			Integer.parseInt(mqtt.get("connection-timeout").toString());
			Integer.parseInt(mqtt.get("keep-alive-interval").toString());
			Integer.parseInt(mqtt.get("waitForCompletion").toString());
			Boolean.parseBoolean(mqtt.get("automatic-reconnect").toString());
			Boolean.parseBoolean(mqtt.get("clean-session").toString());
		}catch(Exception e){
			e.printStackTrace();
			notNull(null, new JsonCode(6020, "Mqtt 配置参数 异常："+e.getMessage()));
		}

		boolean noneMatchKey = magicResourceService.listFiles("mqtt:0").stream()
				.map(it -> (MqttInfo)it)
				.filter(it -> !it.getId().equals(entity.getId()))
				.noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
		isTrue(noneMatchKey, DS_KEY_CONFLICT);
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public MqttInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, MqttInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}
}
