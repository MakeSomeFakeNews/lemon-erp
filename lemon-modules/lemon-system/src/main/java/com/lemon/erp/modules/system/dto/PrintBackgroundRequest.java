package com.lemon.erp.modules.system.dto;

/**
 * 保存套打底图：dataUrl 为图片的 data URL，为空时删除底图
 */
public record PrintBackgroundRequest(String dataUrl) {
}
