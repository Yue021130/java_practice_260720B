package com.example.tip.service;

import com.example.tip.dto.IpQueryDTO;
import com.example.tip.dto.IpRegionVO;
import com.example.tip.util.Ip2RegionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * IP 归属地查询服务
 */
@Service
public class IpRegionService {

    @Resource
    private Ip2RegionUtil ip2RegionUtil;

    public IpRegionVO search(IpQueryDTO dto) {
        return ip2RegionUtil.search(dto.getIp());
    }
}
