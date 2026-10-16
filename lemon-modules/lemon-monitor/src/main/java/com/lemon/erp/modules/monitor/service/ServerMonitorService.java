package com.lemon.erp.modules.monitor.service;

import com.lemon.erp.modules.monitor.dto.ServerInfo;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 服务器监控：CPU / 内存 / 磁盘 / 系统信息通过 OSHI 采集，JVM 信息通过 JMX 获取
 */
@Service
public class ServerMonitorService {

    /** CPU 使用率需要两次采样的差值 */
    private static final long CPU_SAMPLE_MILLIS = 500;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 不展示的虚拟/系统文件系统 */
    private static final Set<String> IGNORED_FS = Set.of("devfs", "autofs", "nullfs", "tmpfs", "devtmpfs",
            "overlay", "squashfs", "proc", "sysfs", "cgroup", "cgroup2");

    private final SystemInfo systemInfo = new SystemInfo();

    public ServerInfo getServerInfo() {
        return new ServerInfo(cpu(), memory(), jvm(), sys(), disks());
    }

    private ServerInfo.Cpu cpu() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prev = processor.getSystemCpuLoadTicks();
        try {
            Thread.sleep(CPU_SAMPLE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long[] ticks = processor.getSystemCpuLoadTicks();
        long user = diff(ticks, prev, TickType.USER) + diff(ticks, prev, TickType.NICE);
        long sys = diff(ticks, prev, TickType.SYSTEM) + diff(ticks, prev, TickType.IRQ) + diff(ticks, prev, TickType.SOFTIRQ);
        long iowait = diff(ticks, prev, TickType.IOWAIT);
        long idle = diff(ticks, prev, TickType.IDLE);
        long steal = diff(ticks, prev, TickType.STEAL);
        long total = Math.max(1, user + sys + iowait + idle + steal);

        double[] load = processor.getSystemLoadAverage(3);
        List<Double> loadAverage = load[0] < 0 ? Collections.emptyList()
                : Arrays.stream(load).map(ServerMonitorService::round).boxed().toList();
        return new ServerInfo.Cpu(processor.getProcessorIdentifier().getName().trim(),
                processor.getPhysicalProcessorCount(), processor.getLogicalProcessorCount(),
                percent(total - idle, total), percent(user, total), percent(sys, total),
                percent(iowait, total), percent(idle, total), loadAverage);
    }

    private ServerInfo.Memory memory() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long total = memory.getTotal();
        long free = memory.getAvailable();
        long used = total - free;
        return new ServerInfo.Memory(total, used, free, percent(used, total),
                memory.getVirtualMemory().getSwapTotal(), memory.getVirtualMemory().getSwapUsed());
    }

    private ServerInfo.Jvm jvm() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        // 未设置 -Xmx 时 max 可能为 -1，退化为已提交大小
        long heapMax = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        List<ServerInfo.Gc> gcs = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map((GarbageCollectorMXBean gc) -> new ServerInfo.Gc(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()))
                .toList();
        String startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()), ZoneId.systemDefault())
                .format(TIME_FORMAT);
        return new ServerInfo.Jvm(runtime.getVmName(), System.getProperty("java.version"), runtime.getVmVendor(),
                System.getProperty("java.home"), startTime, runtime.getUptime() / 1000,
                heapMax, heap.getCommitted(), heap.getUsed(), percent(heap.getUsed(), heapMax),
                memoryBean.getNonHeapMemoryUsage().getUsed(),
                threads.getThreadCount(), threads.getPeakThreadCount(),
                ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
                gcs, runtime.getInputArguments());
    }

    private ServerInfo.Sys sys() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        return new ServerInfo.Sys(os.getNetworkParams().getHostName(), localIp(), os.getFamily(),
                os.getVersionInfo().toString(), System.getProperty("os.arch"), os.getSystemUptime(),
                System.getProperty("user.dir"), ZoneId.systemDefault().getId(), os.getProcessId());
    }

    private List<ServerInfo.Disk> disks() {
        Set<String> seenVolumes = new HashSet<>();
        return systemInfo.getOperatingSystem().getFileSystem().getFileStores(true).stream()
                .filter(fs -> fs.getTotalSpace() > 0 && !IGNORED_FS.contains(fs.getType()) && !isReadOnly(fs))
                // macOS 的 APFS 系统卷共享同一块空间，只保留根目录
                .filter(fs -> !fs.getMount().startsWith("/System/Volumes/"))
                .filter(fs -> seenVolumes.add(fs.getVolume() + "|" + fs.getMount()))
                .map(this::toDisk)
                .toList();
    }

    /**
     * 只读卷（如 macOS 挂载的模拟器镜像）不是业务关心的磁盘。
     * 根目录除外：macOS 的「/」是只读的系统卷，但其容量代表整块系统盘
     */
    private static boolean isReadOnly(OSFileStore fs) {
        if ("/".equals(fs.getMount())) {
            return false;
        }
        String options = fs.getOptions();
        return options != null && Arrays.stream(options.split(",")).map(String::trim)
                .anyMatch(o -> o.equals("ro") || o.equals("rdonly") || o.equals("read-only"));
    }

    private ServerInfo.Disk toDisk(OSFileStore fs) {
        long total = fs.getTotalSpace();
        long free = fs.getUsableSpace();
        long used = total - free;
        return new ServerInfo.Disk(fs.getMount(), fs.getName(), fs.getType(), total, free, used, percent(used, total));
    }

    /** 第一个启用的非回环 IPv4 地址；不使用 InetAddress.getLocalHost()，避免主机名解析超时 */
    private static String localIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (var addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // 获取不到时返回回环地址
        }
        return "127.0.0.1";
    }

    private static long diff(long[] now, long[] prev, TickType type) {
        return Math.max(0, now[type.getIndex()] - prev[type.getIndex()]);
    }

    private static double percent(long part, long total) {
        return total <= 0 ? 0 : round(part * 100.0 / total);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
