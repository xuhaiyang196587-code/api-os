package org.ssssssss.magicapi.mqtt.model;

import java.util.Map;

import org.ssssssss.magicapi.core.model.MagicEntity;

public class MqttInfo extends MagicEntity {
 
	private String key;
	private Map<String, Object> properties;
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public MagicEntity simple() {
		MqttInfo mqttInfo = new MqttInfo();
		mqttInfo.setKey(this.key);
		super.simple(mqttInfo);
		return mqttInfo;
	}

	@Override
	public MagicEntity copy() {
		MqttInfo mqttInfo = new MqttInfo();
		super.copyTo(mqttInfo);
		mqttInfo.setKey(key);
		mqttInfo.setProperties(properties);
		return mqttInfo;
	}
}
