package com.gameexpert.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatRateLimitService {

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> script;
    private static final String MESSAGE_LIMIT = "5";
    private static final String TTL = "10";

    public ChatRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();

        setScript();
    }

    private void setScript(){
        script.setScriptText("""
                         --매개변수 List.of(key) 에 해당
                        local key = KEYS[1]
                        --매개변수 "5"에 해당
                        local limit = tonumber(ARGV[1])
                        --매개변수 "10" 해당
                        local ttlSeconds = tonumber(ARGV[2])
                
                        --redisTemplate.opsForValue().get(key) 에 해당
                        local current = tonumber(redis.call('GET', key) or '0') 
                        if current >= limit then     
                            return 0
                        end
                
                
                        --redisTemplate.opsForValue().increment(key); 에 해당
                        local updated = redis.call('INCR', key) 
                        if updated == 1 then   
                            redis.call('EXPIRE', key, ttlSeconds)
                        end
                
                        return 1
                """);
        script.setResultType(Long.class);
    }

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;

        // TODO Lv 19: 횟수 확인부터 최초 만료 설정까지 원자적으로 실행합니다.
        Long result = redisTemplate.execute(script, List.of(key), MESSAGE_LIMIT, TTL);
        return result == 1;

    }
}
