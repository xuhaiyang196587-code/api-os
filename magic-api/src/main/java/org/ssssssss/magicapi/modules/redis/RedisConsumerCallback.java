package org.ssssssss.magicapi.modules.redis;

@FunctionalInterface
interface RedisConsumerCallback {
	void processMessage(String topicName, String message) throws Exception;
}
