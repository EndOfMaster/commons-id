package com.endofmaster.commons.id;

/**
 * @author ZM.Wang
 */
public class ClusterIdGenerator {
    private long workerId;
    private long sequence = 0L;
    private static long twepoch = 1288834974657L;
    private static long workerIdBits = 10L;
    private static long maxWorkerId;
    private static long sequenceBits;
    private static long workerIdShift;
    private static long timestampLeftShift;
    private static long sequenceMask;
    private long lastTimestamp = -1L;

    public ClusterIdGenerator(long workerId) {
        if (workerId <= maxWorkerId && workerId >= 0L) {
            this.workerId = workerId;
        } else {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
    }

    public synchronized long nextId() {
        long timestamp = this.timeGen();
        if (timestamp < this.lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", this.lastTimestamp - timestamp));
        } else {
            if (this.lastTimestamp == timestamp) {
                this.sequence = this.sequence + 1L & sequenceMask;
                if (this.sequence == 0L) {
                    timestamp = this.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }

            this.lastTimestamp = timestamp;
            return timestamp - twepoch << (int)timestampLeftShift | this.workerId << (int)workerIdShift | this.sequence;
        }
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp;
        for(timestamp = this.timeGen(); timestamp <= lastTimestamp; timestamp = this.timeGen()) {
            ;
        }

        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public static long timestamp(long id) {
        return (id >> (int)timestampLeftShift) + twepoch;
    }

    static {
        maxWorkerId = ~(-1L << (int)workerIdBits);
        sequenceBits = 12L;
        workerIdShift = sequenceBits;
        timestampLeftShift = sequenceBits + workerIdBits;
        sequenceMask = ~(-1L << (int)sequenceBits);
    }
}

