package com.example.tip.controller;

import com.example.tip.common.Result;
import com.example.tip.dto.TransactionSceneVO;
import com.example.tip.dto.TransferDTO;
import com.example.tip.service.TransactionDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 事务失效场景演示接口
 */
@RestController
@RequestMapping("/api/tx")
@Tag(name = "事务失效演示", description = "5 种典型 @Transactional 失效场景")
public class TransactionDemoController {

    private final TransactionDemoService transactionDemoService;

    public TransactionDemoController(TransactionDemoService transactionDemoService) {
        this.transactionDemoService = transactionDemoService;
    }

    @GetMapping("/scenes")
    @Operation(summary = "运行全部事务失效场景")
    public Result<List<TransactionSceneVO>> scenes() throws Exception {
        return Result.ok(Arrays.asList(
                transactionDemoService.scene1SelfInvoke(),
                transactionDemoService.scene1Fix(),
                transactionDemoService.scene2SwallowException(),
                transactionDemoService.scene3WrongRollbackFor(),
                transactionDemoService.scene4NonPublic(),
                transactionDemoService.scene5Async()
        ));
    }

    @PostMapping("/transfer")
    @Operation(summary = "正常转账（事务生效）")
    public Result<BigDecimal> transfer(@Validated @RequestBody TransferDTO dto) {
        return Result.ok(transactionDemoService.getBalance(dto.getFromId()));
    }
}
