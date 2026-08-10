package com.example.excel.web;

import com.alibaba.excel.EasyExcel;
import com.example.excel.basic.UserHead;
import com.example.excel.basic.BasicService;
import com.example.excel.common.ExcelBizException;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 09. Web 下载与导入实战：响应头 / 权限 / 上传校验。
 *
 * 真实项目的 Excel 能力几乎都是 Web 接口，这一章把「下载与上传」
 * 从业务里抽出来，讲清楚 HTTP 层面的规范与坑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebService {

    /** 上传文件大小上限（字节）：50MB，与 yml 的 multipart 配置一致 */
    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

    private final BasicService basicService;
    private final ExcelLogStore logStore;

    /**
     * 下载用的 xlsx 字节（复用快速开始的导出）。
     */
    public byte[] downloadBytes() {
        return basicService.exportBytes();
    }

    /**
     * 真实上传导入：校验大小/类型/空文件，再解析计数。
     */
    public Map<String, Object> importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelBizException("上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelBizException("仅支持 .xlsx 格式（EasyExcel 不支持 .xls）");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ExcelBizException("文件超过 50MB 限制");
        }
        List<UserHead> rows;
        try {
            rows = EasyExcel.read(new ByteArrayInputStream(file.getBytes()))
                    .head(UserHead.class)
                    .sheet()
                    .doReadSync();
        } catch (IOException e) {
            throw new ExcelBizException("读取上传文件失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }
        logStore.add("import", "web", "Web 上传导入：" + filename, rows.size(), 20L);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", filename);
        result.put("sizeBytes", file.getSize());
        result.put("rows", rows.size());
        result.put("firstRows", rows.subList(0, Math.min(3, rows.size())));
        result.put("tip", "上传三件套：非空校验 → 扩展名校验（.xlsx）→ 大小校验；解析后再做业务校验。");
        return result;
    }

    /**
     * 下载规范速记（八股）。
     */
    public Map<String, Object> downloadRule() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("headers", new LinkedHashMap<String, Object>() {{
            put("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet（.xlsx 官方 MIME）");
            put("Content-Disposition", "attachment; filename*=UTF-8''<urlencoded> —— RFC 5987 中文文件名标准写法");
            put("Content-Length", "文件字节数，让浏览器显示进度条");
        }});
        result.put("chineseFilename", "老浏览器只认 filename=\"\"，新浏览器认 filename*=UTF-8''，所以两个都写，"
                + "文件名先 URLEncoder（空格转 %20）——本项目 ExcelWebSupport 就是这么干的。");
        result.put("permission", new String[]{
                "下载接口必须鉴权：Session / Token / 接口签名，别裸奔",
                "文件列表权限校验到行/列：按当前用户过滤数据再导出",
                "防刷：导出接口限流，超大导出转异步任务 + 下载链接（带有效期）"
        });
        result.put("antiHotlink", "下载 URL 加一次性签名（如 ?token=&expire=），防止被外站直接引用盗刷。");
        result.put("tip", "前端拿到 blob 后要手动拼文件名：从 Content-Disposition 解析 filename*，或由接口单独返回文件名。");
        return result;
    }

    /**
     * 上传限制说明。
     */
    public Map<String, Object> uploadLimit() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("config", new LinkedHashMap<String, Object>() {{
            put("spring.servlet.multipart.max-file-size", "50MB（本项目 yml 配置，默认只有 1MB，上传 Excel 常超限）");
            put("spring.servlet.multipart.max-request-size", "50MB");
            put("server.tomcat.max-swallow-size", "-1（避免大请求被吞掉）");
        }});
        result.put("tooBig", "超大文件不要直接进 Excel：先落对象存储，再做异步导入，返回任务 ID 让前端轮询进度。");
        result.put("emptyCheck", "前端用 accept=\".xlsx\" 过滤、后端仍要校验扩展名与大小——前端校验只是体验优化，后端才是防线。");
        result.put("tip", "MaxUploadSizeExceededException 已被全局异常处理器转成友好提示（见 GlobalExceptionHandler）。");
        return result;
    }
}
