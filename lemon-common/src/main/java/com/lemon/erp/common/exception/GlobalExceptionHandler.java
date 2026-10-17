package com.lemon.erp.common.exception;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getLocalizedMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public R<Void> handleAccessDenied(Exception e) {
        return R.fail(R.FORBIDDEN, I18n.t("没有权限，请联系管理员"));
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> I18n.t(f.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return R.fail(msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return R.fail(I18n.t("上传文件过大"));
    }

    /** 请求体不是合法的 JSON，或字段类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(I18n.t("请求参数格式不正确"));
    }

    /** 路径或查询参数类型不匹配，例如 /system/user/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(I18n.t("参数「{0}」格式不正确", e.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(I18n.t("缺少参数「{0}」", e.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(I18n.t("不支持 {0} 请求", e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNotFound(NoResourceFoundException e) {
        return R.fail(R.NOT_FOUND, I18n.t("接口不存在"));
    }

    /** 并发写入时唯一索引冲突（校验通过后被其他请求抢先插入） */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突: {}", e.getMostSpecificCause().getMessage());
        return R.fail(I18n.t("数据已存在，请刷新后重试"));
    }

    /**
     * 未知异常：只记录日志，不把异常信息返回给前端（可能包含 SQL、表结构等内部信息）。
     * 业务异常可能被 MyBatis 等框架包装（例如在拦截器中抛出），这里解开后按业务异常处理。
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handle(Exception e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
            if (t instanceof BizException biz) {
                return handleBiz(biz);
            }
        }
        log.error("系统异常", e);
        return R.fail(I18n.t("系统繁忙，请稍后重试"));
    }
}
