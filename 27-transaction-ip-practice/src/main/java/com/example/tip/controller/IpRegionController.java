package com.example.tip.controller;

import com.example.tip.common.Result;
import com.example.tip.dto.IpQueryDTO;
import com.example.tip.dto.IpRegionVO;
import com.example.tip.service.IpRegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * IP 归属地查询接口
 */
@RestController
@RequestMapping("/api/ip")
@Tag(name = "IP 归属地", description = "基于 ip2region 离线库解析 IP")
public class IpRegionController {

    private final IpRegionService ipRegionService;

    public IpRegionController(IpRegionService ipRegionService) {
        this.ipRegionService = ipRegionService;
    }

    @GetMapping("/search")
    @Operation(summary = "查询 IP 归属地")
    public Result<IpRegionVO> search(@RequestParam String ip) {
        IpQueryDTO dto = new IpQueryDTO();
        dto.setIp(ip);
        return Result.ok(ipRegionService.search(dto));
    }
}
