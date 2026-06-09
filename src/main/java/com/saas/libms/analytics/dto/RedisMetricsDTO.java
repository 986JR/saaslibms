package com.saas.libms.analytics.dto;

public record RedisMetricsDTO(
        //Memory
        long usedMemoryBytes,          // current memory used by Redis
        long maxMemoryBytes,           // maxmemory setting (0 = no limit)
        String usedMemoryFormatted,    // human-readable: "24 MB"
        double memoryUsagePercent,     // usedMemory / maxMemory * 100 (0 if no limit)

        // Hit / Miss rate
        long keyspaceHits,             // total cache hits since Redis started
        long keyspaceMisses,           // total cache misses since Redis started
        double hitRatePercent,         // hits / (hits + misses) * 100

        // Keys
        long totalKeys,                // total keys in Redis right now
        long cacheKeys,                // keys that are DTO cache entries (books::*, etc.)
        long rateLimitKeys,            // keys that are rate limit entries (rl:*)
        long evictedKeys,              // keys evicted due to maxmemory policy

        // Connections
        int connectedClients,          // currently connected clients

        // Server info
        String redisVersion,           // Redis server version
        long uptimeSeconds             // Redis server uptime in seconds
) {}
