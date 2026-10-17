package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.mes.dto.BoardView;
import com.lemon.erp.modules.mes.service.MesBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生产看板：在制工单的工序进度、预计完工，各产线今天的生产情况
 */
@RestController
@RequestMapping("/mes/board")
@RequiredArgsConstructor
public class MesBoardController {

    private final MesBoardService boardService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:board:list')")
    public R<BoardView> board(@RequestParam(required = false) Long workCenterId) {
        return R.ok(boardService.board(workCenterId));
    }
}
