package com.example.eep.service;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.example.eep.entity.SysUser;
import com.example.eep.excel.easypoi.SysUserImport;
import com.example.eep.repository.SysUserRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Easypoi 导入服务测试。
 */
@SpringBootTest
class EasypoiImportServiceTest {

    @Autowired
    private EasypoiImportService easypoiImportService;

    @Autowired
    private SysUserRepository sysUserRepository;

    @BeforeEach
    void clean() {
        sysUserRepository.deleteAll();
    }

    @Test
    void importWithVerify_shouldSaveValidRows() throws Exception {
        List<SysUserImport> list = new ArrayList<>();
        SysUserImport u1 = new SysUserImport();
        u1.setRealname("张三");
        u1.setDeptOrgCode("D001");
        u1.setRoleCode("R001");
        u1.setPhone("13800138000");
        list.add(u1);

        SysUserImport u2 = new SysUserImport();
        u2.setRealname("李四");
        u2.setDeptOrgCode("D002");
        u2.setRoleCode("R002");
        u2.setPhone("13900139000");
        list.add(u2);

        MockMultipartFile file = createExcelFile(list);
        Map<String, Object> result = easypoiImportService.importWithVerify(file);

        assertThat(result.get("successCount")).isEqualTo(2);
        assertThat(result.get("failCount")).isEqualTo(0);
        assertThat(sysUserRepository.count()).isEqualTo(2L);
    }

    @Test
    void importWithVerify_shouldReturnErrorsForDuplicateRows() throws Exception {
        // 模拟数据库已存在同名用户，触发校验处理器失败
        SysUser existing = new SysUser();
        existing.setRealname("张三");
        existing.setDeptOrgCode("D001");
        existing.setRoleCode("R001");
        sysUserRepository.save(existing);

        List<SysUserImport> list = new ArrayList<>();
        SysUserImport u1 = new SysUserImport();
        u1.setRealname("张三"); // 数据库已存在，会失败
        u1.setDeptOrgCode("D001");
        u1.setRoleCode("R001");
        list.add(u1);

        SysUserImport u2 = new SysUserImport();
        u2.setRealname("李四");
        u2.setDeptOrgCode("D002");
        u2.setRoleCode("R002");
        list.add(u2);

        MockMultipartFile file = createExcelFile(list);
        Map<String, Object> result = easypoiImportService.importWithVerify(file);

        assertThat(result.get("successCount")).isEqualTo(1);
        assertThat(result.get("failCount")).isEqualTo(1);
        assertThat(result.get("errorExcelBase64")).isNotNull();
    }

    @Test
    void importWithDuplicateCheck_shouldDetectDuplicateNames() throws Exception {
        List<SysUserImport> list = new ArrayList<>();
        SysUserImport u1 = new SysUserImport();
        u1.setRealname("张三");
        u1.setDeptOrgCode("D001");
        u1.setRoleCode("R001");
        list.add(u1);

        SysUserImport u2 = new SysUserImport();
        u2.setRealname("张三"); // 重复
        u2.setDeptOrgCode("D002");
        u2.setRoleCode("R002");
        list.add(u2);

        MockMultipartFile file = createExcelFile(list);
        Map<String, Object> result = easypoiImportService.importWithDuplicateCheck(file);

        assertThat(result.get("failCount")).isEqualTo(1);
    }

    private MockMultipartFile createExcelFile(List<SysUserImport> list) throws Exception {
        ExportParams params = new ExportParams(null, "用户");
        Workbook workbook = ExcelExportUtil.exportExcel(params, SysUserImport.class, list);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new MockMultipartFile("file", "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(out.toByteArray()));
    }
}
