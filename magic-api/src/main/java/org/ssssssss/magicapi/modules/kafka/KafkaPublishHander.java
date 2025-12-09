package org.ssssssss.magicapi.modules.kafka;

@FunctionalInterface
public interface KafkaPublishHander {
	void callback(String topicName, String errorMessage);
}