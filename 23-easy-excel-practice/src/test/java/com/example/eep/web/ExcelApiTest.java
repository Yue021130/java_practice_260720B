package com.example.eep.web;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.example.eep.excel.easypoi.SysUserImport;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel 接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void explain_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/excel/explain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").exists());
    }

    @Test
    void initProducts_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/excel/easyexcel/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(20));
    }

    @Test
    void importMap_shouldReturn200() throws Exception {
        MockMultipartFile file = createExcelFile();

        mockMvc.perform(multipart("/api/excel/easypoi/import/map").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    private MockMultipartFile createExcelFile() throws Exception {
        List<SysUserImport> list = new ArrayList<>();
        SysUserImport u = new SysUserImport();
        u.setRealname("张三");
        u.setDeptOrgCode("D001");
        u.setRoleCode("R001");
        list.add(u);

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
