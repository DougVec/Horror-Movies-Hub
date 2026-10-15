package com.douglas.horrorhub.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Redis fora do ar não derruba a API: o erro é logado e a chamada segue direto para a TMDB. */
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            public void handleCacheGetError(RuntimeException e, Cache c, Object k) { avisar("get", c, e); }
            public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) { avisar("put", c, e); }
            public void handleCacheEvictError(RuntimeException e, Cache c, Object k) { avisar("evict", c, e); }
            public void handleCacheClearError(RuntimeException e, Cache c) { avisar("clear", c, e); }
        };
    }

    private static void avisar(String op, Cache cache, RuntimeException e) {
        log.warn("cache {} falhou em {}: {}", op, cache.getName(), e.getMessage());
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
