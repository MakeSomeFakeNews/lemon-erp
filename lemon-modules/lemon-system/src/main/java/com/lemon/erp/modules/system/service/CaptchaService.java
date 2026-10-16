package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.setting.CaptchaType;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.modules.system.dto.CaptchaImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * 登录验证码：生成图片（算术题或 4 位字母数字），答案按 ID 缓存 2 分钟，校验一次后作废
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    public static final String CACHE_KEY = "login:captcha:";
    private static final Duration TTL = Duration.ofMinutes(2);
    /** 去掉容易混淆的 0/O、1/I/L */
    private static final String CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final CacheService cache;
    private final SettingService settings;
    private final SecureRandom random = new SecureRandom();

    public CaptchaImage create() {
        String text;
        String answer;
        if (settings.getEnum(Setting.CAPTCHA_TYPE, CaptchaType.class) == CaptchaType.MATH) {
            int a = random.nextInt(9) + 1;
            int b = random.nextInt(9) + 1;
            switch (random.nextInt(3)) {
                case 0 -> {
                    text = a + "+" + b + "=?";
                    answer = String.valueOf(a + b);
                }
                case 1 -> {
                    text = Math.max(a, b) + "-" + Math.min(a, b) + "=?";
                    answer = String.valueOf(Math.abs(a - b));
                }
                default -> {
                    text = a + "×" + b + "=?";
                    answer = String.valueOf(a * b);
                }
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            text = sb.toString();
            answer = text;
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        cache.set(CACHE_KEY + id, answer, TTL);
        return new CaptchaImage(id, "data:image/png;base64," + render(text));
    }

    /** 校验并作废验证码（不区分大小写） */
    public void verify(String captchaId, String code) {
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            throw new BizException("请输入验证码");
        }
        String key = CACHE_KEY + captchaId;
        String answer = cache.get(key, String.class);
        cache.delete(key);
        if (answer == null) {
            throw new BizException("验证码已过期，请刷新");
        }
        if (!answer.equalsIgnoreCase(code.trim().toUpperCase(Locale.ROOT))) {
            throw new BizException("验证码错误");
        }
    }

    private String render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(246, 248, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线和噪点
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(150, 220));
                g.setStroke(new BasicStroke(1f + random.nextFloat()));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            for (int i = 0; i < 60; i++) {
                g.setColor(randomColor(120, 230));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            int step = (WIDTH - 16) / text.length();
            for (int i = 0; i < text.length(); i++) {
                AffineTransform saved = g.getTransform();
                int x = 8 + i * step + random.nextInt(4);
                int y = 29 + random.nextInt(5) - 2;
                g.rotate(Math.toRadians(random.nextInt(31) - 15), x + step / 2.0, y - 10);
                g.setColor(randomColor(20, 110));
                g.drawString(String.valueOf(text.charAt(i)), x, y);
                g.setTransform(saved);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BizException("生成验证码失败");
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(min + random.nextInt(max - min), min + random.nextInt(max - min), min + random.nextInt(max - min));
    }
}
