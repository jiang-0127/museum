package com.museum.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 加速层封装。Redis 关闭或连接失败时自动降级，业务正确性由 MySQL 保证。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.enabled:false}")
    private boolean enabled;

    private volatile boolean warned;
    private final Map<String, MemoryCounter> memoryCounters = new ConcurrentHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public String get(String key) {
        if (!enabled) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            warnOnce(e);
            return null;
        }
    }

    public void set(String key, String value, Duration ttl) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            warnOnce(e);
        }
    }

    public <T> T getJson(String key, Class<T> type) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            return null;
        }
    }

    public void setJson(String key, Object value, Duration ttl) {
        try {
            set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
            // 缓存序列化失败不影响主流程
        }
    }

    public void delete(String key) {
        memoryCounters.remove(key);
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            warnOnce(e);
        }
    }

    /**
     * 仅读取计数，不累加。用于在“校验凭证前”判断是否已被锁定，避免正常登录被计入失败次数。
     */
    public long current(String key) {
        if (enabled) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                return value == null ? 0L : Long.parseLong(value);
            } catch (Exception e) {
                warnOnce(e);
            }
        }
        MemoryCounter counter = memoryCounters.get(key);
        if (counter == null || counter.expireAt() < System.currentTimeMillis()) {
            return 0L;
        }
        return counter.count().get();
    }

    public long increment(String key, Duration ttl) {
        if (enabled) {
            try {
                Long value = redisTemplate.opsForValue().increment(key);
                if (value != null && value == 1L) {
                    redisTemplate.expire(key, ttl);
                }
                return value == null ? 0 : value;
            } catch (Exception e) {
                warnOnce(e);
            }
        }
        long now = System.currentTimeMillis();
        MemoryCounter counter = memoryCounters.compute(key, (k, old) -> {
            if (old == null || old.expireAt < now) {
                return new MemoryCounter(new AtomicLong(1), now + ttl.toMillis());
            }
            old.count.incrementAndGet();
            return old;
        });
        return counter.count.get();
    }

    public boolean setIfAbsent(String key, Duration ttl) {
        if (!enabled) {
            return true;
        }
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            warnOnce(e);
            return true;
        }
    }

    private void warnOnce(Exception e) {
        if (!warned) {
            warned = true;
            log.warn("Redis 不可用，已自动降级为本地模式: {}", e.getMessage());
        }
    }

    private record MemoryCounter(AtomicLong count, long expireAt) {
    }
}
