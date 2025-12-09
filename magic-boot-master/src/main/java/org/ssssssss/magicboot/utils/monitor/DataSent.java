package org.ssssssss.magicboot.utils.monitor;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DataSent {
	private static final ConcurrentHashMap<String, SseEmitter> SseEmitters = new ConcurrentHashMap<>();
	private static ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);

    public DataSent() {
        // 每30秒检查一次心跳
        heartbeatScheduler.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }
    public void checkHeartbeats() {
    	for (Map.Entry<String, SseEmitter> entry : SseEmitters.entrySet()) {
			SseEmitter emitter = entry.getValue();
			 try {
				emitter.send(SseEmitter.event()
				            .name("heartbeat")
				            .data("{\"type\":\"heartbeat\",\"timestamp\":" + System.currentTimeMillis() + "}"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
    public static SseEmitter get(String clientId, SEErrorCallback seErrorCallback) {
        // 如果已存在发射器，直接返回
        SseEmitter existingEmitter = SseEmitters.get(clientId);
        if (existingEmitter != null) {
            return existingEmitter;
        }
        
        // 创建新的发射器并设置超时时间（0表示无超时）
        SseEmitter emitter = new SseEmitter(0L);
        
        // 客户端断开连接时触发
        emitter.onCompletion(() -> {
        	System.out.println("SSE connection completed for client: " + clientId);
            SseEmitters.remove(clientId, emitter);
            seErrorCallback.error(clientId, "SSE connection completed for client: " + clientId);
        });
        
        // 如果设置了超时时间
        emitter.onTimeout(() -> {
        	System.out.println("SSE connection timed out for client: " + clientId);
            SseEmitters.remove(clientId, emitter);
            seErrorCallback.error(clientId, "SSE connection timed out for client: " + clientId);
        });
        
        // 添加错误处理
        emitter.onError((ex) -> {
        	System.out.println("SSE error for client: " + clientId + ", error: " + ex.getMessage());
            SseEmitters.remove(clientId, emitter);
            seErrorCallback.error(clientId, "SSE error for client: " + clientId + ", error: " + ex.getMessage());
        });
        
        // 将新发射器放入映射
        SseEmitters.put(clientId, emitter);
        return emitter;
    }
    
    public static Set<String>  getLinks(){
        return SseEmitters.keySet();
    }
    
    public static void send(String clientId, String message){
        SseEmitter emitter = SseEmitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(clientId).data(message));
            } catch (IOException | IllegalStateException e) {
                // 发送失败时移除无效的发射器
                SseEmitters.remove(clientId, emitter);
            }
        }
    }
    // 添加获取连接数量的方法
    public static int getConnectionCount() {
        return SseEmitters.size();
    }
}

