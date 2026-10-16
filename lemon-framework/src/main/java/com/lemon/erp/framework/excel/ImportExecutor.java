package com.lemon.erp.framework.excel;

import com.lemon.erp.common.exception.BizException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Excel 导入执行器：逐行做 Bean Validation 并调用业务的新增方法（与手工新增走同一套校验），
 * 收集每行的错误；全部成功才提交，任何一行出错整批回滚，避免导入一半
 */
@Component
@RequiredArgsConstructor
public class ImportExecutor {

    public static final int MAX_ROWS = 2000;

    /** 返回的错误行数上限 */
    private static final int MAX_ERRORS = 200;

    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    public <T> ImportResult run(ImportRequest<T> req, Consumer<T> handler, Class<?>... groups) {
        List<ImportRow<T>> rows = req.rows();
        List<ImportResult.RowError> errors = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            for (ImportRow<T> row : rows) {
                if (errors.size() >= MAX_ERRORS) {
                    break;
                }
                if (row == null || row.data() == null) {
                    continue;
                }
                String error = validate(row.data(), groups);
                if (error == null) {
                    try {
                        handler.accept(row.data());
                    } catch (BizException e) {
                        error = e.getMessage();
                    }
                }
                if (error != null) {
                    errors.add(new ImportResult.RowError(row.rowNum(), error));
                }
            }
            if (!errors.isEmpty()) {
                status.setRollbackOnly();
            }
        });
        return new ImportResult(rows.size(), errors.isEmpty() ? rows.size() : 0, errors);
    }

    private <T> String validate(T data, Class<?>... groups) {
        var violations = validator.validate(data, groups);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("；"));
    }
}
