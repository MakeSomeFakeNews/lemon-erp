package com.lemon.erp.modules.monitor.dto;

import java.util.List;

/**
 * 服务器信息。容量单位均为字节，百分比为 0-100（保留两位小数），时长单位为秒
 */
public record ServerInfo(Cpu cpu, Memory memory, Jvm jvm, Sys sys, List<Disk> disks) {

    public record Cpu(String name, int physicalCores, int logicalCores,
                      double usage, double user, double system, double iowait, double idle,
                      List<Double> loadAverage) {
    }

    public record Memory(long total, long used, long free, double usage, long swapTotal, long swapUsed) {
    }

    public record Jvm(String name, String version, String vendor, String home, String startTime, long uptime,
                      long heapMax, long heapCommitted, long heapUsed, double heapUsage, long nonHeapUsed,
                      int threadCount, int peakThreadCount, long loadedClassCount,
                      List<Gc> gcs, List<String> inputArgs) {
    }

    public record Gc(String name, long count, long time) {
    }

    public record Sys(String hostname, String ip, String osName, String osVersion, String osArch, long uptime,
                      String userDir, String timezone, int pid) {
    }

    public record Disk(String mount, String name, String type, long total, long free, long used, double usage) {
    }
}
