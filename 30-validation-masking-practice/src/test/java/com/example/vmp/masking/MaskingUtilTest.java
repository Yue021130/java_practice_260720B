package com.example.vmp.masking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 脱敏策略单元测试。
 */
class MaskingUtilTest {

    @Test
    void maskPhone() {
        assertEquals("138****8000", MaskingUtil.maskPhone("13800138000"));
        assertEquals("139****5678", MaskingUtil.maskPhone("13912345678"));
        assertNull(MaskingUtil.maskPhone(null));
        assertEquals("123", MaskingUtil.maskPhone("123"));
    }

    @Test
    void maskIdCard() {
        assertEquals("1101**********7758", MaskingUtil.maskIdCard("110101199003077758"));
        assertNull(MaskingUtil.maskIdCard(null));
    }

    @Test
    void maskEmail() {
        assertEquals("z****@163.com", MaskingUtil.maskEmail("zhangsan@163.com"));
        assertEquals("a****@gmail.com", MaskingUtil.maskEmail("abc@gmail.com"));
        assertNull(MaskingUtil.maskEmail(null));
    }

    @Test
    void maskName() {
        assertEquals("张*", MaskingUtil.maskName("张三"));
        assertEquals("张**", MaskingUtil.maskName("张三丰"));
        assertEquals("张", MaskingUtil.maskName("张"));
        assertNull(MaskingUtil.maskName(null));
    }

    @Test
    void maskAddress() {
        assertEquals("北京市朝阳区****", MaskingUtil.maskAddress("北京市朝阳区建国路 88 号"));
        assertEquals("上海浦东新区****", MaskingUtil.maskAddress("上海浦东新区张江高科技园区"));
        assertNull(MaskingUtil.maskAddress(null));
        assertEquals("北京市", MaskingUtil.maskAddress("北京市"));
    }
}
