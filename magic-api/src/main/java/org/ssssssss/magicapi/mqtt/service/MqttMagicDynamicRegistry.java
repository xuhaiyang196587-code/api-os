package org.ssssssss.magicapi.mqtt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.mqtt.model.MagicDynamicMqttClient;
import org.ssssssss.magicapi.mqtt.model.MqttInfo;
import org.ssssssss.magicapi.mqtt.util.MqttDataSource;

public class MqttMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MqttInfo> {

	private final MagicDynamicMqttClient magicDynamicMqttClient;

	private static final Logger logger = LoggerFactory.getLogger(MqttMagicDynamicRegistry.class);

	public MqttMagicDynamicRegistry(MagicResourceStorage<MqttInfo> magicResourceStorage,
			MagicDynamicMqttClient magicDynamicMqttClient) {
		super(magicResourceStorage);
		this.magicDynamicMqttClient = magicDynamicMqttClient;
	}

	@EventListener(condition = "#event.type == 'mqtt'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception e) {
			logger.error("注册mqtt数据源失败", e);
		}
	}

	@Override
	protected boolean register(MappingNode<MqttInfo> mappingNode) {
		MqttInfo info = mappingNode.getEntity();
		try {
			MqttDataSource mqttDataSource = new MqttDataSource(info, false);
			if(mqttDataSource.getClient() == null) {
				return false;
			}
			magicDynamicMqttClient.put(info.getId(), info.getKey(), info.getName(), mqttDataSource);
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	@Override
	protected void unregister(MappingNode<MqttInfo> mappingNode) {
		magicDynamicMqttClient.delete(mappingNode.getEntity().getKey());
	}

	
}
