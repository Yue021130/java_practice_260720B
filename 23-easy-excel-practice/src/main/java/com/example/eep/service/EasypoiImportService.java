package com.example.eep.service;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.handler.inter.IExcelVerifyHandler;
import com.example.eep.entity.SysUser;
import com.example.eep.excel.easypoi.SysUserImport;
import com.example.eep.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Easypoi 导入导出服务。
 *
 * <p>覆盖：</p>
 * <ul>
 *     <li>基础导入</li>
 *     <li>带业务校验导入 + 错误日志回写</li>
 *     <li>无注解 Map 方式导入</li>
 *     <li>ThreadLocal 组内重复校验</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EasypoiImportService {

    private final SysUserRepository sysUserRepository;
    private final IExcelVerifyHandler<SysUserImport> sysUserVerifyHandler;

    /**
     * 基础导入：将 Excel 解析为 SysUserImport 列表。
     */
    public List<SysUserImport> importBasic(MultipartFile file) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        try (InputStream in = file.getInputStream()) {
            return ExcelImportUtil.importExcel(in, SysUserImport.class, params);
        }
    }

    /**
     * 带校验导入：正确数据落库，错误数据生成错误日志 Excel。
     *
     * @return result.failList 中的错误信息 + 生成的错误日志下载字节
     */
    public Map<String, Object> importWithVerify(MultipartFile file) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        params.setNeedVerify(true);
        params.setVerifyHandler(sysUserVerifyHandler);

        ExcelImportResult<SysUserImport> result;
        try (InputStream in = file.getInputStream()) {
            result = ExcelImportUtil.importExcelMore(in, SysUserImport.class, params);
        }

        // 校验通过的数据落库
        List<SysUserImport> successList = result.getList();
        List<SysUser> saved = successList.stream().map(this::toEntity).collect(Collectors.toList());
        sysUserRepository.saveAll(saved);

        // 失败数据
        List<SysUserImport> failList = result.getFailList();
        byte[] errorExcelBytes = null;
        if (!failList.isEmpty()) {
            // 传入副本，避免 Easypoi 导出时修改原始 failList（实测会清空列表）
            errorExcelBytes = exportErrorLog(new ArrayList<>(failList));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("successCount", successList.size());
        map.put("failCount", failList.size());
        map.put("failDetails", failList.stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowNum", u.getRowNum());
            item.put("realname", u.getRealname());
            item.put("errorMsg", u.getErrorMsg());
            return item;
        }).collect(Collectors.toList()));
        map.put("errorExcelBase64", errorExcelBytes == null ? null : Base64.getEncoder().encodeToString(errorExcelBytes));
        map.put("tip", "errorExcelBase64 为错误日志 Excel 的 Base64，可下载查看");
        return map;
    }

    /**
     * Map 方式导入：不定义实体类，直接读取为 Map。
     */
    public Map<String, Object> importByMap(MultipartFile file) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        try (InputStream in = file.getInputStream()) {
            List<Map<String, Object>> list = ExcelImportUtil.importExcel(in, Map.class, params);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", list.size());
            result.put("firstRow", list.isEmpty() ? null : list.get(0));
            result.put("rows", list);
            return result;
        }
    }

    /**
     * 带组内重复校验的导入（ThreadLocal 版）。
     *
     * <p>演示 Easypoi verifyHandler 中 ThreadLocal 的使用。</p>
     */
    public Map<String, Object> importWithDuplicateCheck(MultipartFile file) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        params.setNeedVerify(true);
        params.setVerifyHandler(new GroupDuplicateVerifyHandler());

        ExcelImportResult<SysUserImport> result;
        try (InputStream in = file.getInputStream()) {
            result = ExcelImportUtil.importExcelMore(in, SysUserImport.class, params);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("successCount", result.getList().size());
        map.put("failCount", result.getFailList().size());
        map.put("failDetails", result.getFailList().stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowNum", u.getRowNum());
            item.put("realname", u.getRealname());
            item.put("errorMsg", u.getErrorMsg());
            return item;
        }).collect(Collectors.toList()));
        return map;
    }

    /**
     * 将导入失败的列表导出为错误日志 Excel。
     */
    private byte[] exportErrorLog(List<SysUserImport> failList) throws IOException {
        ExportParams params = new ExportParams("导入错误日志", "错误记录");
        Workbook workbook = ExcelExportUtil.exportExcel(params, SysUserImport.class, failList);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    /**
     * 下载一个示例导入模板。
     */
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        setExcelResponse(response, "sys-user-template.xlsx");
        List<SysUserImport> list = new ArrayList<>();
        SysUserImport demo = new SysUserImport();
        demo.setRealname("张三");
        demo.setDeptOrgCode("D001");
        demo.setRoleCode("R001");
        demo.setPhone("13800138000");
        demo.setEmail("zhangsan@example.com");
        demo.setSexName("男");
        demo.setWorkNo("W001");
        list.add(demo);

        ExportParams params = new ExportParams("系统用户导入模板", "模板");
        Workbook workbook = ExcelExportUtil.exportExcel(params, SysUserImport.class, list);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private SysUser toEntity(SysUserImport user) {
        SysUser entity = new SysUser();
        entity.setRealname(user.getRealname());
        entity.setDeptOrgCode(user.getDeptOrgCode());
        entity.setRoleCode(user.getRoleCode());
        entity.setPhone(user.getPhone());
        entity.setEmail(user.getEmail());
        entity.setSexName(user.getSexName());
        entity.setWorkNo(user.getWorkNo());
        return entity;
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encoded);
    }
}
