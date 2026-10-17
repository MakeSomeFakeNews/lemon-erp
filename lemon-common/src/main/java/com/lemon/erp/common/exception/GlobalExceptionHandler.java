package com.lemon.erp.common.exception;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.InvalidFormatException;

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
        return R.fail(R.FORBIDDEN, Messages.get("common.text.accessDeniedContactAdministrator"));
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return R.fail(Messages.get("common.text.uploadedFileTooLarge"));
    }

    /** 请求体不是合法的 JSON，或字段类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        // 枚举、数字等字段取值不合法：提示字段和取值
        DatabindException withPath = null;
        for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
            if (t instanceof DatabindException d && !d.getPath().isEmpty()) {
                withPath = d;
            }
            Object value;
            if (t instanceof InvalidFormatException f) {
                value = f.getValue();
            } else if (t instanceof InvalidEnumValueException v) {
                // 枚举的 @JsonCreator 抛出（如 EnableStatus 收到 1 / 0 以外的值），Jackson 包装后带字段路径
                value = v.getValue();
            } else {
                continue;
            }
            String field = withPath == null ? null : withPath.getPath().get(withPath.getPath().size() - 1).getPropertyName();
            if (field != null) {
                return R.fail(Messages.get("common.text.invalidFieldValue", field, value));
            }
        }
        return R.fail(Messages.get("common.text.invalidRequestFormat"));
    }

    /** 路径或查询参数类型不匹配，例如 /system/user/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(Messages.get("common.text.parameterHasInvalidFormat", e.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(Messages.get("common.text.missingParameter", e.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(Messages.get("common.text.requestsNotSupported", e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNotFound(NoResourceFoundException e) {
        return R.fail(R.NOT_FOUND, Messages.get("common.text.apiNotFound"));
    }

    /** 并发写入时唯一索引冲突（校验通过后被其他请求抢先插入） */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突: {}", e.getMostSpecificCause().getMessage());
        return R.fail(Messages.get("common.text.dataAlreadyExistsRefreshTryAgain"));
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
        return R.fail(Messages.get("common.text.systemBusyTryAgainLater"));
    }
}
