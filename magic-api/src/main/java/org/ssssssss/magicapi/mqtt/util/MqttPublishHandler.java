package org.ssssssss.magicapi.mqtt.util;

@FunctionalInterface
public interface MqttPublishHandler {
	void callback(boolean status, String errorMessage);
}