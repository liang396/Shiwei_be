package com.shiwei.seckill.common.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SnowflakeIdGenerator {
    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private static final long EPOCH = 1704067200000L;
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_CLOCK_BACKWARD_MS = 5L;

    private final long machineId;
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public SnowflakeIdGenerator() {
        this.machineId = resolveMachineId();
    }

    public synchronized long nextId() {
        long timestamp = currentTime();
        long last = lastTimestamp.get();

        if (timestamp < last) {
            long offset = last - timestamp;
            if (offset <= MAX_CLOCK_BACKWARD_MS) {
                log.warn("Snowflake clock moved backwards by {} ms, waiting to recover.", offset);
                timestamp = waitNextMillis(last);
            } else {
                throw new IllegalStateException("Clock moved backwards too much: " + offset + " ms");
            }
        }

        if (timestamp == last) {
            long seq = (sequence.incrementAndGet()) & SEQUENCE_MASK;
            if (seq == 0) {
                timestamp = waitNextMillis(last);
                sequence.set(0L);
            }
        } else {
            sequence.set(0L);
        }

        lastTimestamp.set(timestamp);
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (machineId << MACHINE_ID_SHIFT) | sequence.get();
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

    private long resolveMachineId() {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            return Math.abs(host.hashCode()) & MAX_MACHINE_ID;
        } catch (UnknownHostException e) {
            log.warn("Resolve machineId from host failed, fallback to 1.", e);
            return 1L;
        }
    }
}

