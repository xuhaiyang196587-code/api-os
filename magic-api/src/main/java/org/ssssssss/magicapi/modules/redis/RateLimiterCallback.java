package org.ssssssss.magicapi.modules.redis;
@FunctionalInterface
interface RateLimiterCallback {
	Object exec() throws Exception;
}
