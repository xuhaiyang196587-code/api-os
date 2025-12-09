package org.ssssssss.magicapi.mqtt.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.ssssssss.magicapi.mqtt.model.MqttInfo;

import cn.hutool.core.lang.UUID;

public class MqttDataSource{
	
	private String id = "";
	private String clientId = "";
	private HighConcurrencyMqttClient highConcurrencyMqttClient = null;
	
	public MqttDataSource(MqttInfo info, boolean isTest) {
		this.id = info.getId();
		Map<String, Object> mqtt = new HashMap<>(info.getProperties());
		
		String broker = mqtt.get("broker").toString();
		String username = mqtt.get("username").toString();
		String password = mqtt.get("password").toString();
		int queueSize = Integer.parseInt(mqtt.get("queueSize").toString());
		int qos = Integer.parseInt(mqtt.get("qos").toString());
		int connectionTimeout = Integer.parseInt(mqtt.get("connection-timeout").toString());
		int keepAliveInterval = Integer.parseInt(mqtt.get("keep-alive-interval").toString());
		int waitForCompletion = Integer.parseInt(mqtt.get("waitForCompletion").toString());
		boolean automaticReconnect = Boolean.parseBoolean(mqtt.get("automatic-reconnect").toString());
		boolean cleanSession = Boolean.parseBoolean(mqtt.get("clean-session").toString());
		String client = info.getKey()+"_"+UUID.randomUUID().toString().replaceAll("-", "");
		if(!isTest) {
			// 创建高并发MQTT客户端
			this.highConcurrencyMqttClient = new HighConcurrencyMqttClient(
					client, broker, username, password
					,qos
					,queueSize
					,connectionTimeout
					,keepAliveInterval
					,waitForCompletion
					,automaticReconnect
					,cleanSession
					);
			
		}else {
			client = "linktest_"+UUID.randomUUID().toString().replaceAll("-", "");
			this.highConcurrencyMqttClient = new HighConcurrencyMqttClient(
					client, broker, username, password
					,qos
					,queueSize
					,connectionTimeout
					,keepAliveInterval
					,waitForCompletion
					,automaticReconnect
					,cleanSession
			);
		}
		
		this.clientId = client;
	}

	public MqttAsyncClient getClient() {
		return highConcurrencyMqttClient.getClient();
	}
	
	public boolean test() {
		
		
		return highConcurrencyMqttClient.test();
	}
	
	public boolean isConnected() {
		return getClient().isConnected();
	}
	
	public String getId() {
		return id;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void close(){
		highConcurrencyMqttClient.shutdown();
	}

	public void publish(String topic, byte[] payload, int qos, boolean retained) {
		highConcurrencyMqttClient.publish(topic, payload, qos, retained);
	}

	public void publish(String topic, byte[] payload, int qos, boolean retained, IMqttActionListener iMqttActionListener) {
		highConcurrencyMqttClient.publish(topic, payload, qos, retained, iMqttActionListener);
	}

	public void setHandler(String topicName, MqttSubscribeHandler mqttSubscribeHander) {
		highConcurrencyMqttClient.setHandler(topicName, mqttSubscribeHander);
	}

	public void removeHandler(String topicName) {
		highConcurrencyMqttClient.removeHandler(topicName);
	}
}
