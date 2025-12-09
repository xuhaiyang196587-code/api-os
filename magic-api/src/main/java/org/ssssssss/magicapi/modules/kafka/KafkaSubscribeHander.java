package org.ssssssss.magicapi.modules.kafka;

@FunctionalInterface
public interface KafkaSubscribeHander {
	void callback(String topicName, String key, String stringMessage, byte[] byteArrayMessage);
}