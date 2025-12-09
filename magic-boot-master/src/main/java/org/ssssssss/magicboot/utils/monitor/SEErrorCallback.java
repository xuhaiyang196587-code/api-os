package org.ssssssss.magicboot.utils.monitor;


@FunctionalInterface
public interface SEErrorCallback {
	void error(String clientId, String errorMessage);
}