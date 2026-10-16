package com.lemon.erp.framework.ip;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * IP 归属地查询，基于 ip2region 离线库（resources/ip2region/ip2region_v4.xdb，约 11MB），
 * 启动时整体加载到内存，单次查询为微秒级，且不依赖任何外部服务。
 * <p>
 * 数据库更新：从 https://github.com/lionsoul2014/ip2region/tree/master/data 下载最新 ip2region_v4.xdb 覆盖即可。
 * 目前只内置 IPv4 库，公网 IPv6 返回「未知」。
 */
@Slf4j
@Service
public class IpLocationService {

    public static final String INTRANET = "内网IP";
    public static final String UNKNOWN = "未知";
    private static final String DB_PATH = "ip2region/ip2region_v4.xdb";

    private final Searcher searcher;

    public IpLocationService() {
        Searcher s = null;
        try (InputStream in = new ClassPathResource(DB_PATH).getInputStream()) {
            // 基于内存缓冲的 Searcher 是线程安全的，可全局共享
            s = Searcher.newWithBuffer(Version.IPv4, Searcher.loadContentFromInputStream(in));
        } catch (IOException e) {
            log.warn("加载 IP 归属地库 {} 失败，归属地将显示为「{}」", DB_PATH, UNKNOWN, e);
        }
        this.searcher = s;
    }

    /**
     * 查询归属地，如「广东省深圳市 电信」「美国 加利福尼亚」「内网IP」
     */
    public String getLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        if (isIntranet(ip)) {
            return INTRANET;
        }
        if (searcher == null || ip.contains(":")) {
            return UNKNOWN;
        }
        try {
            return format(searcher.search(ip));
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    /**
     * ip2region 返回「国家|省份|城市|运营商|...」，缺失项为 0
     */
    static String format(String region) {
        if (region == null || region.isBlank()) {
            return UNKNOWN;
        }
        String[] parts = region.split("\\|", -1);
        String country = part(parts, 0);
        String province = part(parts, 1);
        String city = part(parts, 2);
        String isp = part(parts, 3);

        List<String> area = new ArrayList<>();
        // 国内地址省略「中国」，直辖市避免「北京市北京市」
        if (!"中国".equals(country)) area.add(country);
        if (!province.isEmpty()) area.add(province);
        if (!city.isEmpty() && !city.equals(province)) area.add(city);
        area.removeIf(String::isEmpty);

        String location = "中国".equals(country) ? String.join("", area) : String.join(" ", area);
        if (location.isEmpty()) {
            location = country.isEmpty() ? UNKNOWN : country;
        }
        return isp.isEmpty() ? location : location + " " + isp;
    }

    private static String part(String[] parts, int index) {
        if (index >= parts.length) return "";
        String v = parts[index].trim();
        return "0".equals(v) ? "" : v;
    }

    /** 回环、内网、链路本地地址 */
    public static boolean isIntranet(String ip) {
        try {
            // 只对字面量 IP 解析，不会发起 DNS 查询
            if (!ip.matches("[0-9a-fA-F.:]+")) return false;
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress() || isUniqueLocalV6(addr);
        } catch (Exception e) {
            return false;
        }
    }

    /** IPv6 唯一本地地址 fc00::/7 */
    private static boolean isUniqueLocalV6(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xfe) == 0xfc;
    }
}
