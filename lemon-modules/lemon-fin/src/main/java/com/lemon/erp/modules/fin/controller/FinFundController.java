package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.FundPlan;
import com.lemon.erp.modules.fin.dto.FundPlanEntry;
import com.lemon.erp.modules.fin.dto.FundTransferRequest;
import com.lemon.erp.modules.fin.dto.NoteRequests;
import com.lemon.erp.modules.fin.entity.FinFundPlanItem;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import com.lemon.erp.modules.fin.entity.FinNote;
import com.lemon.erp.modules.fin.model.NoteKind;
import com.lemon.erp.modules.fin.model.NoteStatus;
import com.lemon.erp.modules.fin.model.PlanUnit;
import com.lemon.erp.modules.fin.service.FinFundService;
import com.lemon.erp.modules.fin.service.FinNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 资金管理：票据、账户调拨、资金计划
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinFundController {

    private final FinNoteService noteService;
    private final FinFundService fundService;

    // ---------------------------------------------------------------- 票据

    /** @param dueWithin 只看 N 天内到期（含已到期）的 */
    @GetMapping("/note")
    @PreAuthorize("@ss.hasPerm('fin:note:list')")
    public R<PageResult<FinNote>> pageNotes(@RequestParam NoteKind kind,
                                            @RequestParam(required = false) NoteStatus status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer dueWithin,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(noteService.page(current, size, kind, status, keyword, dueWithin));
    }

    @GetMapping("/note/{id}")
    @PreAuthorize("@ss.hasPerm('fin:note:list')")
    public R<FinNote> note(@PathVariable Long id) {
        return R.ok(noteService.detail(id));
    }

    @PostMapping("/note/receive")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-收票", type = OperType.INSERT) // i18n-ignore
    public R<Long> receive(@Valid @RequestBody NoteRequests.Register req) {
        return R.ok(noteService.receive(req));
    }

    @PostMapping("/note/issue")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-开具应付票据", type = OperType.INSERT) // i18n-ignore
    public R<Long> issue(@Valid @RequestBody NoteRequests.Register req) {
        return R.ok(noteService.issue(req));
    }

    @PostMapping("/note/{id}/endorse")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-背书转让", type = OperType.UPDATE) // i18n-ignore
    public R<Void> endorse(@PathVariable Long id, @Valid @RequestBody NoteRequests.Endorse req) {
        noteService.endorse(id, req);
        return R.ok();
    }

    @PostMapping("/note/{id}/discount")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-贴现", type = OperType.UPDATE) // i18n-ignore
    public R<Void> discount(@PathVariable Long id, @Valid @RequestBody NoteRequests.Settle req) {
        noteService.discount(id, req);
        return R.ok();
    }

    @PostMapping("/note/{id}/collect")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-托收", type = OperType.UPDATE) // i18n-ignore
    public R<Void> collect(@PathVariable Long id, @Valid @RequestBody NoteRequests.Settle req) {
        noteService.collect(id, req);
        return R.ok();
    }

    @PostMapping("/note/{id}/dishonor")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-拒付", type = OperType.UPDATE) // i18n-ignore
    public R<Void> dishonor(@PathVariable Long id, @Valid @RequestBody NoteRequests.Settle req) {
        noteService.dishonor(id, req);
        return R.ok();
    }

    @PostMapping("/note/{id}/pay")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-兑付", type = OperType.UPDATE) // i18n-ignore
    public R<Void> pay(@PathVariable Long id, @Valid @RequestBody NoteRequests.Settle req) {
        noteService.pay(id, req);
        return R.ok();
    }

    /** 撤销最近一次贴现、托收、拒付、兑付 */
    @PostMapping("/note/{id}/undo")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据-撤销", type = OperType.UPDATE) // i18n-ignore
    public R<Void> undo(@PathVariable Long id) {
        noteService.undo(id);
        return R.ok();
    }

    @DeleteMapping("/note/{id}")
    @PreAuthorize("@ss.hasPerm('fin:note:edit')")
    @OperLog(title = "票据", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteNote(@PathVariable Long id) {
        noteService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 账户调拨

    @GetMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:list')")
    public R<PageResult<FinFundTransfer>> pageTransfers(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(fundService.pageTransfers(current, size, keyword, begin, end));
    }

    @PostMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.INSERT) // i18n-ignore
    public R<Long> createTransfer(@Valid @RequestBody FundTransferRequest req) {
        return R.ok(fundService.createTransfer(req));
    }

    @DeleteMapping("/fund-transfer/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteTransfers(@PathVariable List<Long> ids) {
        fundService.deleteTransfers(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 资金计划

    @GetMapping("/fund-plan")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<FundPlan> plan(@RequestParam(defaultValue = "WEEK") PlanUnit unit, @RequestParam(defaultValue = "12") int periods) {
        return R.ok(fundService.plan(unit, periods));
    }

    /** 一期的收支明细；from 为空表示已逾期 */
    @GetMapping("/fund-plan/entries")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<List<FundPlanEntry>> planEntries(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(fundService.planEntries(from, to));
    }

    @GetMapping("/fund-plan/items")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<PageResult<FinFundPlanItem>> pageItems(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                    @RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(fundService.pageItems(current, size, begin, end));
    }

    @PostMapping("/fund-plan/items")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.INSERT) // i18n-ignore
    public R<Void> createItem(@Validated(Create.class) @RequestBody FinFundPlanItem item) {
        item.setId(null);
        fundService.saveItem(item);
        return R.ok();
    }

    @PutMapping("/fund-plan/items/{id}")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateItem(@PathVariable Long id, @Valid @RequestBody FinFundPlanItem item) {
        item.setId(id);
        fundService.saveItem(item);
        return R.ok();
    }

    @DeleteMapping("/fund-plan/items/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteItems(@PathVariable List<Long> ids) {
        fundService.deleteItems(ids);
        return R.ok();
    }
}
