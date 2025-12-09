package org.ssssssss.magicapi.modules.mqtt;

import java.beans.Transient;
import java.io.IOException;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.mqtt.model.MagicDynamicMqttClient;
import org.ssssssss.magicapi.mqtt.util.MqttDataSource;
import org.ssssssss.magicapi.mqtt.util.MqttPublishHandler;
import org.ssssssss.magicapi.mqtt.util.MqttSubscribeHandler;
import org.ssssssss.script.annotation.Comment;
import org.ssssssss.script.functions.DynamicAttribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * mqtt模块
 *
 * @author xuhaiyang
 */
@MagicModule("mqtt")
public class MqttModule implements DynamicAttribute<MqttModule, MqttModule> {
 
	private MagicDynamicMqttClient magicDynamicMqttClient;
	private MqttDataSource mqttDataSource;

	public MqttModule(MagicDynamicMqttClient magicDynamicMqttClient) {
		this.magicDynamicMqttClient = magicDynamicMqttClient;
	}
	
	public MqttModule(MqttDataSource mqttDataSource) {
		this.mqttDataSource = mqttDataSource;
	}
	
	public MqttDataSource getMqttDataSource() {
		return mqttDataSource;
	}

	private void valid() {
		if(mqttDataSource == null) {
			mqttDataSource = magicDynamicMqttClient.getModule("def").getMqttDataSource();
		}
	}
	
	/**
	 * 数据源切换
	 */
	@Override
	@Transient
	public MqttModule getDynamicAttribute(String key) {
		return magicDynamicMqttClient.getModule(key);
	}
	
	@Comment("判断客户端是否在线；")
	public boolean isOn() throws Exception {
		valid();
		return mqttDataSource.isConnected();
	}
	
	@Comment("发布消息；\n\n"
			+ " 返回:\n\n"
			+ " 1:表示发布成功；\n\n"
			+ " 0:表示失败。")
	public int publish(
			@Comment(name = "topicName", value = "主题名称") String topicName,
			@Comment(name = "topicContent", value = "写入内容") String topicContent) {
		try {
			valid();
			// 发布消息
			mqttDataSource.publish(topicName, topicContent.getBytes(), 1, false);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	@Comment("带回调的发布消息")
	public void publish(
			@Comment(name = "topicName", value = "主题名称") String topicName,
			@Comment(name = "topicContent", value = "写入内容") String topicContent,
			@Comment(name = "mqttPublishHandler", value = "写入回调函数(status,errorMessage)->{...}") MqttPublishHandler mqttPublishHandler
	) {
		valid();
		// 发布消息
		mqttDataSource.publish(topicName, topicContent.getBytes(), 1, false, new IMqttActionListener() {
			
			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				mqttPublishHandler.callback(true, null);
			}
			
			@Override
			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
				mqttPublishHandler.callback(false, exception.getMessage());
			}
		});
		 
	}
	
	@Comment("订阅主题；\n\n"
			+ " 返回:\n\n"
			+ " 客户端对象，用于取消订阅操作！")
	public void subscribe(
			@Comment(name = "topicName", value = "主题名称") String topicName,
			@Comment(name = "mqttSubscribeHander", value = "回调函数 ;\n\n 如：(topicName,stringContent,byteArrayContent)->{...}") MqttSubscribeHandler mqttSubscribeHander) {
		valid();
		unSubscribe(topicName);
		mqttDataSource.setHandler(topicName,mqttSubscribeHander);
	}
	
	@Comment("集群模式下订阅主题（同一组内，每次只会有一个订阅端收到数据）\n\n"
			+ " MQTT 版本需要 >=5.0;")
	public void clusterSubscribe(
			@Comment(name = "groupId", value = "分组订阅") String groupId,
			@Comment(name = "topicName", value = "主题名称") String topicName,
			@Comment(name = "mqttSubscribeHander", value = "回调函数 ;\n\n 如：(topicName,stringContent,byteArrayContent)->{...}") MqttSubscribeHandler mqttSubscribeHander) {
		valid();
		String topic = "$share/"+groupId+"/"+topicName;
		unSubscribe(topicName);
		mqttDataSource.setHandler(topic,mqttSubscribeHander);
	}
	
	@Comment("取消指定客户端订阅")
	public void unSubscribe(
			@Comment(name = "topicName", value = "主题名称") String topicName
	) {
		valid();
		mqttDataSource.removeHandler(topicName);
	}
	
	private HttpClient httpClient;
	public Boolean fetchConnectedClientStatus(String ipPort,String API_KEY, String API_SECRET ,String clientId) throws Exception {
		if (clientId == null || clientId.trim().isEmpty()) {
			throw new Exception("请必须执行一个客户端连接的clientId！！");
		} 
		
		try {
			// 获取特定客户端信息
			String apiUrl = "http://"+ipPort+"/api/v5/clients/" + clientId;
			HttpGet request = new HttpGet(apiUrl);
			
			// 创建Basic认证头
			String credentials = API_KEY + ":" + API_SECRET;
			String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
			request.setHeader("Authorization", "Basic " + encodedCredentials);
			this.httpClient = HttpClients.createDefault();
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			
			ObjectMapper objectMapper = new ObjectMapper();
			
			if (entity != null) {
				String jsonString = EntityUtils.toString(entity);
				
				JsonNode rootNode = objectMapper.readTree(jsonString);
				
				// 检查是否有错误
				if (rootNode.has("code") && !rootNode.get("code").asText().equals("0")) {
					return false;
				}
				
				if (clientId != null && !clientId.trim().isEmpty()) {
					// 处理单个客户端响应
					boolean connected = rootNode.get("connected").asBoolean(false);
					return connected;
				}
			}
		} catch (IOException e) {
			System.err.println("获取客户端列表失败: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	
	
}