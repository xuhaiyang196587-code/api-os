package org.ssssssss.magicapi.modules.redis;

import org.redisson.api.RMap;

@FunctionalInterface
interface MapHander {
	void hander(RMap<String, Object> map, RedisModule redisModule);
}
