package org.ssssssss.magicapi.mqtt.util;

@FunctionalInterface
public interface MqttSubscribeHandler {
	void callback(String topicName, String stringContent, byte[] byteArrayContent);
}