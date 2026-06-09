package com.saas.libms.analytics.dto;

public record JvmMetricsDTO(
        // Heap memory
        long heapUsedBytes,        // current live heap usage
        long heapMaxBytes,         // maximum heap (-Xmx)
        double heapUsedPercent,    // heapUsed / heapMax * 100

        // Non-heap memory (Metaspace, code cache)
        long nonHeapUsedBytes,

        // Threads
        int liveThreads,           // currently live threads
        int daemonThreads,         // daemon threads (background)
        int peakThreads,           // peak since JVM started

        // CPU
        double cpuUsagePercent,    // process CPU usage 0.0–100.0
        int availableProcessors,   // CPU cores available to JVM

        //  Uptime
        long uptimeSeconds,        // seconds since JVM started
        String uptimeFormatted     // human-readable: "2d 4h 12m"
) {}
