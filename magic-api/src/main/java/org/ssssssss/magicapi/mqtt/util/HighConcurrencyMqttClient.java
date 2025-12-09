package org.ssssssss.magicapi.mqtt.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class HighConcurrencyMqttClient implements MqttCallbackExtended {

    // 连接状态常量
    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;
    private static final int DISCONNECTING = 3;
    
    private static final long RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 999999999;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    
    private MqttAsyncClient mqttClient;
    private final String brokerUrl;
    private final String clientId;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final String userName;
    private final String password;
    private final int qos;
    private final int queueSize;
    private final int connectionTimeout;
    private final int keepAliveInterval;
    private final int waitForCompletion;
    private final boolean automaticReconnect;
    private final boolean cleanSession;
    
    // 使用线程安全的ConcurrentHashMap存储处理器
    private final ConcurrentMap<String, MqttSubscribeHandler> handles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Pattern> topicPatterns = new ConcurrentHashMap<>();
    
    // 使用读写锁处理订阅/取消订阅操作
    private final ReadWriteLock subscriptionLock = new ReentrantReadWriteLock();
    
    // 消息处理线程池
    private final ExecutorService messageProcessor;
    
    // 连接状态监控
    private final AtomicInteger connectionStatus = new AtomicInteger(DISCONNECTED);
    
    // 重连计数器
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    // 重连任务控制
    private volatile ScheduledFuture<?> reconnectFuture;
    private final Object reconnectLock = new Object();
    
    // 客户端锁，防止并发连接/断开操作
    private final Object clientLock = new Object();
    
    // 心跳任务
    private ScheduledFuture<?> heartbeatFuture;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 连接尝试标志，防止重复连接
    private volatile boolean connectAttempted = false;
    // 调试模式
    private final boolean debugMode = true;

    public HighConcurrencyMqttClient(String clientId, String brokerUrl, String userName, String password,
            int qos, int queueSize, int connectionTimeout, int keepAliveInterval, int waitForCompletion,
            boolean automaticReconnect, boolean cleanSession) {
        this(clientId, brokerUrl, userName, password, qos, queueSize, connectionTimeout, 
             keepAliveInterval, waitForCompletion, automaticReconnect, cleanSession,
             Runtime.getRuntime().availableProcessors() * 2);
    }

    public HighConcurrencyMqttClient(String clientId, String brokerUrl, String userName, 
                                    String password, int qos, int queueSize, int connectionTimeout,
                                    int keepAliveInterval, int waitForCompletion, 
                                    boolean automaticReconnect, boolean cleanSession, 
                                    int threadPoolSize) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.userName = userName;
        this.password = password;
        this.qos = qos;
        this.queueSize = queueSize;
        this.connectionTimeout = connectionTimeout;
        this.keepAliveInterval = keepAliveInterval;
        this.waitForCompletion = waitForCompletion;
        this.automaticReconnect = automaticReconnect;
        this.cleanSession = cleanSession;
        
        // 创建有界线程池处理消息
        this.messageProcessor = new ThreadPoolExecutor(
            threadPoolSize, 
            threadPoolSize,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(queueSize),
            new MqttThreadFactory("mqtt-processor"),
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程处理
        );
        
        if (debugMode) {
            System.out.println("MQTT客户端初始化完成，客户端ID: " + clientId);
        }
        
        if (!this.clientId.startsWith("linktest_")) {
            connect();
        }
    }
    
    public void setHandler(String topicFilter, MqttSubscribeHandler handler) {
        handles.put(topicFilter, handler);
        // 预编译主题匹配模式以提高性能
        topicPatterns.put(topicFilter, compileTopicPattern(topicFilter));
        
        // 如果已连接，立即订阅
        if (isConnected()) {
            subscribeTopic(topicFilter);
        }
    }

    public void removeHandler(String topicFilter) {
        handles.remove(topicFilter);
        topicPatterns.remove(topicFilter);
        
        // 如果已连接，取消订阅
        if (isConnected()) {
            unsubscribeTopic(topicFilter);
        }
    }
    
    private void subscribeTopic(String topicFilter) {
        try {
            subscriptionLock.writeLock().lock();
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topicFilter, qos);
                if (debugMode) {
                    System.out.println("MQTT客户端,成功订阅主题: " + topicFilter);
                }
            }
        } catch (MqttException e) {
            System.err.println("MQTT客户端,订阅主题失败: " + topicFilter + ", 错误: " + e.getMessage());
        } finally {
            subscriptionLock.writeLock().unlock();
        }
    }
    
    private void unsubscribeTopic(String topicFilter) {
        try {
            subscriptionLock.writeLock().lock();
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(topicFilter);
                if (debugMode) {
                    System.out.println("MQTT客户端,成功取消订阅主题: " + topicFilter);
                }
            }
        } catch (MqttException e) {
            System.err.println("MQTT客户端,取消订阅主题失败: " + topicFilter + ", 错误: " + e.getMessage());
        } finally {
            subscriptionLock.writeLock().unlock();
        }
    }
    
    public MqttAsyncClient getClient() {
        return mqttClient;
    }
    
    public boolean isConnected() {
        return connectionStatus.get() == CONNECTED && mqttClient != null && mqttClient.isConnected();
    }

    public void connect() {
        // 检查是否已经尝试过连接
        if (connectAttempted) {
            if (debugMode) {
                System.out.println("MQTT客户端,连接已经尝试过，不再重复连接。客户端ID: " + clientId);
            }
            return;
        }
        
        // 使用CAS确保只有一个连接线程在执行
        if (!connectionStatus.compareAndSet(DISCONNECTED, CONNECTING)) {
            // 已经在连接或已连接状态
            if (debugMode) {
                System.out.println("MQTT客户端,连接已在进行中或已完成，当前状态: " + connectionStatus.get() + ", 客户端ID: " + clientId);
            }
            return;
        }
        
        // 设置连接尝试标志
        connectAttempted = true;
    
        try {
            MqttConnectOptions options = createMqttConnectOptions();

            synchronized (clientLock) {
                // 确保旧的客户端被正确关闭
                cleanupOldClient();
                
                mqttClient = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
                mqttClient.setCallback(this);
            }
            
            System.out.println("MQTT客户端,正在连接到 MQTT Broker: " + brokerUrl + ", 客户端ID: " + clientId);
            IMqttToken token = mqttClient.connect(options);
            token.waitForCompletion(waitForCompletion * 1000);
            
            if (token.isComplete() && token.getException() == null) {
                System.out.println("MQTT客户端,成功连接到 MQTT Broker: " + brokerUrl + ", 客户端ID: " + clientId);
                connectionStatus.set(CONNECTED);
                reconnectAttempts.set(0);
                
                // 取消任何计划中的重连任务
                cancelReconnect();
                
                // 启动心跳检测
                startHeartbeat();
                
                // 连接成功后重新订阅所有主题
                resubscribeAllTopics();
            } else {
                if (token.getException() != null) {
                    throw new MqttException(token.getException());
                } else {
                    throw new MqttException(MqttException.REASON_CODE_CONNECTION_LOST);
                }
            }
            
        } catch (MqttException e) {
            System.err.println("MQTT客户端,连接MQTT服务器失败: " + e.getMessage() + ", 客户端ID: " + clientId);
            handleConnectionFailure();
        } catch (Exception e) {
            System.err.println("MQTT客户端,连接过程中发生未知错误: " + e.getMessage() + ", 客户端ID: " + clientId);
            handleConnectionFailure();
        }
    }
    
    private MqttConnectOptions createMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(automaticReconnect);
        options.setCleanSession(cleanSession);
        
        if (userName != null && !userName.trim().isEmpty()) {
            options.setUserName(userName);
        }
        
        if (password != null && !password.trim().isEmpty()) {
            options.setPassword(password.toCharArray());
        }
        
        // 设置连接超时和会话保持时间
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        // 设置最大飞行消息数，提高并发性能
        options.setMaxInflight(queueSize);
        
        return options;
    }
    
    private void cleanupOldClient() {
        if (mqttClient != null) {
            try {
                // 停止心跳检测
                stopHeartbeat();
                
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
            } catch (MqttException e) {
                // 忽略"已在进行连接"的错误，这通常发生在客户端正在尝试连接时关闭
                if (!e.getMessage().contains("已在进行连接")) {
                    System.err.println("MQTT客户端,关闭旧客户端时发生错误: " + e.getMessage());
                }
            } finally {
                mqttClient = null;
            }
        }
    }
    
    private void handleConnectionFailure() {
        connectionStatus.set(DISCONNECTED);
        
        int attempts = reconnectAttempts.incrementAndGet();
        
        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
            System.out.println("MQTT客户端,计划在 " + RECONNECT_DELAY_SECONDS + " 秒后重连... 尝试次数: " + attempts + ", 客户端ID: " + clientId);
            scheduleReconnect();
        } else {
            System.err.println("MQTT客户端,已达到最大重连尝试次数，停止重连, 客户端ID: " + clientId);
            cancelReconnect();
        }
    }

    public boolean test() {
        try {
            MqttConnectOptions options = createMqttConnectOptions();
            // 测试连接使用较短的超时时间
            options.setConnectionTimeout(3);
            options.setKeepAliveInterval(3);
            
            MqttAsyncClient testClient = new MqttAsyncClient(brokerUrl, "test_" + System.currentTimeMillis(), new MemoryPersistence());
            
            IMqttToken token = testClient.connect(options);
            token.waitForCompletion(waitForCompletion * 1000);
            
            boolean connected = testClient.isConnected();
            
            if (connected) {
                testClient.disconnect();
            }
            testClient.close();
            
            System.out.println("MQTT客户端,测试连接" + (connected ? "成功" : "失败") + ": " + brokerUrl);
            return connected;
            
        } catch (MqttException e) {
            System.err.println("MQTT客户端,测试连接MQTT服务器失败: " + e.getMessage());
            return false;
        }
    }

    // 重新订阅所有已注册的主题
    private void resubscribeAllTopics() {
        if (handles.isEmpty()) {
            return;
        }
        
        try {
            subscriptionLock.writeLock().lock();
            String[] topics = handles.keySet().toArray(new String[0]);
            int[] QoS = new int[topics.length];
            for (int i = 0; i < QoS.length; i++) {
                QoS[i] = qos;
            }
            
            if (topics.length > 0 && mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topics, QoS);
                if (debugMode) {
                    System.out.println("MQTT客户端,重新订阅了 " + topics.length + " 个主题, 客户端ID: " + clientId);
                }
            }
        } catch (MqttException e) {
            System.err.println("MQTT客户端,重新订阅主题失败: " + e.getMessage() + ", 客户端ID: " + clientId);
        } finally {
            subscriptionLock.writeLock().unlock();
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        System.out.println("MQTT客户端,已成功" + (reconnect ? "重连" : "连接") + "到: " + serverURI + ", 客户端ID: " + clientId);
        connectionStatus.set(CONNECTED);
        reconnectAttempts.set(0);
        
        // 取消任何计划中的重连任务
        cancelReconnect();
        
        // 启动心跳检测
        startHeartbeat();
        
        if (reconnect) {
            // 重连成功后重新订阅所有主题
            resubscribeAllTopics();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("MQTT 连接丢失! 原因: " + cause.getMessage() + ", 客户端ID: " + clientId);
        
        // 停止心跳检测
        stopHeartbeat();
        
        // 只有当前状态是已连接时才触发重连
        if (connectionStatus.compareAndSet(CONNECTED, DISCONNECTED)) {
            System.out.println("MQTT客户端,连接丢失，计划重连, 客户端ID: " + clientId);
            scheduleReconnect();
        } else {
            System.out.println("MQTT客户端,连接丢失但当前状态不是已连接，忽略重连。当前状态: " + connectionStatus.get() + ", 客户端ID: " + clientId);
        }
    }

    private void scheduleReconnect() {
        synchronized (reconnectLock) {
            // 取消现有的重连任务
            cancelReconnect();
            
            reconnectFuture = reconnectScheduler.schedule(() -> {
                System.out.println("MQTT客户端,正在执行重连尝试 " + reconnectAttempts.get() + "/" + MAX_RECONNECT_ATTEMPTS + ", 客户端ID: " + clientId);
                connect();
            }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void cancelReconnect() {
        synchronized (reconnectLock) {
            if (reconnectFuture != null && !reconnectFuture.isDone()) {
                reconnectFuture.cancel(false);
                reconnectFuture = null;
            }
        }
    }
    
    private void startHeartbeat() {
        // 取消现有心跳任务
        stopHeartbeat();
        
        heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (isConnected()) {
                try {
                    // 发送心跳消息或空消息保持连接
                    publish("$SYS/heartbeat/" + clientId, new byte[0], 0, false);
                } catch (Exception e) {
                    System.err.println("MQTT客户端,发送心跳失败: " + e.getMessage() + ", 客户端ID: " + clientId);
                }
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    private void stopHeartbeat() {
        if (heartbeatFuture != null && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // 异步处理消息，避免阻塞网络线程
        messageProcessor.submit(() -> {
            try {
                // 查找匹配的处理程序（支持通配符主题）
                for (Map.Entry<String, MqttSubscribeHandler> entry : handles.entrySet()) {
                    String topicFilter = entry.getKey();
                    Pattern pattern = topicPatterns.get(topicFilter);
                    
                    if (pattern != null && pattern.matcher(topic).matches()) {
                        try {
                            entry.getValue().callback(topic, new String(message.getPayload()), message.getPayload());
                        } catch (Exception e) {
                            System.err.println("MQTT客户端,处理MQTT消息时发生错误: " + e.getMessage() + ", 主题: " + topic);
                        }
                        break; // 找到第一个匹配的处理程序即可
                    }
                }
            } catch (Exception e) {
                System.err.println("MQTT客户端,处理MQTT消息时发生未知错误: " + e.getMessage() + ", 主题: " + topic);
            }
        });
    }
    
    // 编译主题匹配模式
    private Pattern compileTopicPattern(String topicFilter) {
        // 处理共享订阅主题
        String actualTopic = topicFilter;
        if (topicFilter.startsWith("$share/")) {
            // 提取实际主题部分（去掉 $share/group/ 前缀）
            int thirdSlashIndex = topicFilter.indexOf('/', 7); // 7 是 "$share/".length
            if (thirdSlashIndex != -1) {
                actualTopic = topicFilter.substring(thirdSlashIndex + 1);
                if (debugMode) {
                    System.out.println("MQTT客户端,共享订阅主题: " + topicFilter + " -> 实际主题: " + actualTopic);
                }
            }
        }
        
        // 转义正则表达式特殊字符
        String regex = actualTopic
            .replace("\\", "\\\\")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace(".", "\\.")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("|", "\\|")
            .replace("*", "\\*")
            .replace("+", "\\+")
            .replace("?", "\\?");
        
        // 处理MQTT通配符
        regex = regex
            .replace("/#", "(/.*)?")  // 多级通配符
            .replace("+/", "([^/]+/)?") // 单级通配符
            .replace("+", "[^/]+");     // 单级通配符
        
        return Pattern.compile("^" + regex + "$");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 可以在这里添加消息发送成功后的处理逻辑
        // 如更新发送状态、记录日志等
    }

    // 异步发布消息
    public void publish(String topic, byte[] payload, int qos, boolean retained) {
        if (!isConnected()) {
            throw new IllegalStateException("MQTT客户端未连接, 客户端ID: " + clientId);
        }
        
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        
        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            System.err.println("MQTT客户端,发布消息失败: " + e.getMessage() + ", 主题: " + topic);
            // 可以根据业务需求添加重试逻辑
        }
    }
    
    // 带回调的异步发布
    public void publish(String topic, byte[] payload, int qos, boolean retained, 
                       IMqttActionListener listener) {
        if (!isConnected()) {
            throw new IllegalStateException("MQTT客户端未连接, 客户端ID: " + clientId);
        }
        
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        
        try {
            mqttClient.publish(topic, message, null, listener);
        } catch (MqttException e) {
            System.err.println("MQTT客户端,发布消息失败: " + e.getMessage() + ", 主题: " + topic);
            if (listener != null) {
                listener.onFailure(null, e);
            }
        }
    }

    public void disconnect() {
        try {
            // 设置状态为断开中
            if (connectionStatus.compareAndSet(CONNECTED, DISCONNECTING) || 
                connectionStatus.compareAndSet(CONNECTING, DISCONNECTING)) {
                
                cancelReconnect();
                stopHeartbeat();
                
                synchronized (clientLock) {
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.disconnect();
                    }
                }
                
                connectionStatus.set(DISCONNECTED);
                System.out.println("MQTT客户端已断开连接, 客户端ID: " + clientId);
            }
        } catch (MqttException e) {
            System.err.println("MQTT客户端,断开连接时发生错误: " + e.getMessage() + ", 客户端ID: " + clientId);
            connectionStatus.set(DISCONNECTED);
        }
    }
    
    // 优雅关闭
    public void shutdown() {
        disconnect();
        
        try {
            // 关闭重连调度器
            reconnectScheduler.shutdown();
            if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectScheduler.shutdownNow();
            }
            
            // 关闭心跳调度器
            heartbeatScheduler.shutdown();
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
            
            // 关闭消息处理器
            messageProcessor.shutdown();
            if (!messageProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            reconnectScheduler.shutdownNow();
            heartbeatScheduler.shutdownNow();
            messageProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // 自定义线程工厂
    private static class MqttThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        MqttThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}