package org.ssssssss.magicapi.mqtt.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.modules.mqtt.MqttModule;
import org.ssssssss.magicapi.mqtt.util.MqttDataSource;
import org.ssssssss.magicapi.utils.Assert;
/**
 * 动态mqtt客戶端对象
 */
public class MagicDynamicMqttClient {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicMqttClient.class);

	private final Map<String, MqttDataSource> dataSourceMap = new HashMap<>();
	private final Map<String, MqttModule> mqttModuleMap = new HashMap<>();
	 
	/**
	 * 注册数据源（可以运行时注册）
	 *
	 * @param id             数据源ID
	 * @param dataSourceKey  数据源Key
	 * @param datasourceName 数据源名称
	 */
	public void put(String id, String dataSourceKey, String datasourceName, MqttDataSource mqttDataSource) {
		 
		this.dataSourceMap.put(dataSourceKey, mqttDataSource);
		
		this.mqttModuleMap.put(dataSourceKey, new MqttModule(mqttDataSource));
		
		if (id != null) {
			String finalDataSourceKey = dataSourceKey;
			this.dataSourceMap.entrySet().stream()
					.filter(it -> id.equals(it.getValue().getId()) && !finalDataSourceKey.equals(it.getKey()))
					.findFirst()
					.ifPresent(it -> {
						logger.info("移除mqtt旧数据源:{}", it.getKey());
						this.dataSourceMap.remove(it.getKey()).close();
						this.mqttModuleMap.remove(it.getKey());
					});
		}
	}

	/**
	 * 获取全部数据源
	 */
	public List<String> datasources() {
		return new ArrayList<>(this.dataSourceMap.keySet());
	}

	public boolean isEmpty() {
		return this.dataSourceMap.isEmpty();
	}

	/**
	 * 获取全部数据源
	 */
	public Collection<MqttDataSource> datasourceNodes() {
		return this.dataSourceMap.values();
	}

	/**
	 * 删除数据源
	 *
	 * @param datasourceKey 数据源Key
	 * @throws MqttException 
	 */
	public boolean delete(String datasourceKey){
		boolean result = false;
		// 检查参数是否合法
		if (datasourceKey != null && !datasourceKey.isEmpty()) {
			this.dataSourceMap.remove(datasourceKey).close();
			this.mqttModuleMap.remove(datasourceKey);
			result = true;
		}
		logger.info("删除mqtt数据源：{}:{}", datasourceKey, result ? "成功" : "失败");
		return result;
	}

	/**
	 * 获取数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public MqttDataSource getDataSource(String datasourceKey) {
		MqttDataSource mqttDataSource = dataSourceMap.get(datasourceKey);
		Assert.isNotNull(mqttDataSource, String.format("找不到mqtt数据源%s", datasourceKey));
		return mqttDataSource;
	}
	/**
	 * 获取module
	 *
	 * @param datasourceKey 数据源Key
	 */
	public MqttModule getModule(String datasourceKey) {
		MqttModule mqttModule = mqttModuleMap.get(datasourceKey);
		Assert.isNotNull(mqttModule, String.format("找不到mqtt 可用 module %s", datasourceKey));
		return mqttModule;
	}
 
}
