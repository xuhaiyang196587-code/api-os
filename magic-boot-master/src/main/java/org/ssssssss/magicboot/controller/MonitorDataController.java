package org.ssssssss.magicboot.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("monitor")
public class MonitorDataController {
	private static ConcurrentHashMap<String, SseEmitter> SseEmitters = new ConcurrentHashMap<>();
	private ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);

    public MonitorDataController() {
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
	@GetMapping("/get")
	public SseEmitter get(@RequestParam("clientId") String clientId) {
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
            SseEmitters.remove(clientId,emitter);
        });
        
        // 添加错误处理
        emitter.onError((ex) -> {
        	System.out.println("SSE error for client: " + clientId + ", error: " + ex.getMessage());
            SseEmitters.remove(clientId,emitter);
        });
        
        // 将新发射器放入映射
        SseEmitters.put(clientId, emitter);

		return emitter;
	}
	// 添加测试接口查看连接状态
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connectionCount", SseEmitters.size());
        status.put("connections", SseEmitters.keySet());
        return status;
    }
    @GetMapping("/getLinks")
    public static List<String>  getLinks(){
    	List<String> result = new ArrayList<>();
    	for (Map.Entry<String, SseEmitter> entry : SseEmitters.entrySet()) {
    		result.add(entry.getKey());	  
		}
    	
        return result;
    }
    
    @GetMapping("/send")
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
}
