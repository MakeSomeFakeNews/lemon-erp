package com.lemon.erp.modules.ai.client;

import java.util.Base64;

/**
 * 发送给识图模型的图片，按 OpenAI 协议以 base64 data URL 放在 image_url 中
 *
 * @param mimeType 如 image/png、image/jpeg
 * @param data     图片内容
 */
public record LlmImage(String mimeType, byte[] data) {

    /** data:image/png;base64,... */
    public String dataUrl() {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(data);
    }

    @Override
    public String toString() {
        return "LlmImage[" + mimeType + ", " + data.length + " bytes]";
    }
}
