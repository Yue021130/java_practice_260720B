package com.example.tip.service;

import com.example.tip.dto.IpQueryDTO;
import com.example.tip.dto.IpRegionVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IP 归属地查询测试
 */
@SpringBootTest
class IpRegionServiceTest {

    @Autowired
    private IpRegionService ipRegionService;

    @Test
    void testLocalhost() {
        IpQueryDTO dto = new IpQueryDTO();
        dto.setIp("127.0.0.1");
        IpRegionVO vo = ipRegionService.search(dto);
        assertEquals("本地回环", vo.getCity());
    }

    @Test
    void testBaiduDns() {
        IpQueryDTO dto = new IpQueryDTO();
        dto.setIp("114.114.114.114");
        IpRegionVO vo = ipRegionService.search(dto);
        assertEquals("中国", vo.getCountry());
        assertEquals("江苏省", vo.getProvince());
        assertTrue(vo.getFullAddress().contains("电信"));
    }

    @Test
    void testAliDns() {
        IpQueryDTO dto = new IpQueryDTO();
        dto.setIp("223.5.5.5");
        IpRegionVO vo = ipRegionService.search(dto);
        assertEquals("中国", vo.getCountry());
        assertEquals("浙江省", vo.getProvince());
        assertTrue(vo.getFullAddress().contains("阿里巴巴"));
    }
}
