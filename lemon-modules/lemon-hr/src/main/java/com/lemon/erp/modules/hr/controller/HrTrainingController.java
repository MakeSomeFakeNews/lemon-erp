package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.TrainingRecordsRequest;
import com.lemon.erp.modules.hr.entity.HrTraining;
import com.lemon.erp.modules.hr.entity.HrTrainingRecord;
import com.lemon.erp.modules.hr.service.HrTrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 培训管理 /hr/training
 */
@RestController
@RequestMapping("/hr/training")
@RequiredArgsConstructor
public class HrTrainingController {

    private final HrTrainingService trainingService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:training:list')")
    public R<PageResult<HrTraining>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Integer year) {
        return R.ok(trainingService.page(current, size, keyword, category, status, year));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:training:list')")
    public R<HrTraining> get(@PathVariable Long id) {
        return R.ok(trainingService.get(id));
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("@ss.hasPerm('hr:training:list')")
    public R<List<HrTrainingRecord>> records(@PathVariable Long id) {
        return R.ok(trainingService.records(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('hr:training:add')")
    @OperLog(title = "培训管理", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Void> create(@Validated(Create.class) @RequestBody HrTraining training) {
        trainingService.create(training);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:training:edit')")
    @OperLog(title = "培训管理", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody HrTraining training) {
        trainingService.update(id, training);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:training:remove')")
    @OperLog(title = "培训管理", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        trainingService.delete(ids);
        return R.ok();
    }

    /** 开始 ONGOING / 结束 FINISHED / 取消 CANCELED，body：{ status } */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('hr:training:edit')")
    @OperLog(title = "培训管理-状态", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        trainingService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    /** 添加学员，body：员工 ID 列表；返回新增人数 */
    @PostMapping("/{id}/trainees")
    @PreAuthorize("@ss.hasPerm('hr:training:edit')")
    @OperLog(title = "培训管理-添加学员", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Integer> addTrainees(@PathVariable Long id, @RequestBody List<Long> employeeIds) {
        return R.ok(trainingService.addTrainees(id, employeeIds));
    }

    @DeleteMapping("/{id}/trainees/{recordIds}")
    @PreAuthorize("@ss.hasPerm('hr:training:edit')")
    @OperLog(title = "培训管理-移除学员", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> removeTrainees(@PathVariable Long id, @PathVariable List<Long> recordIds) {
        trainingService.removeTrainees(id, recordIds);
        return R.ok();
    }

    @PutMapping("/{id}/records")
    @PreAuthorize("@ss.hasPerm('hr:training:edit')")
    @OperLog(title = "培训管理-出勤与成绩", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> saveRecords(@PathVariable Long id, @Valid @RequestBody TrainingRecordsRequest req) {
        trainingService.saveRecords(id, req.items());
        return R.ok();
    }
}
