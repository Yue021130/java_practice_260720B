package com.example.excel.listener;

import com.alibaba.excel.EasyExcel;
import com.example.excel.basic.UserHead;
import com.example.excel.common.ExcelBizException;
import com.example.excel.config.ExcelPracticeProperties;
import com.example.excel.support.DemoData;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 07. 监听器与增量读取：流式逐行解析 + 按批落库。
 *
 * 适合大文件导入：内存占用与文件大小无关（只跟 batchSize 有关）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListenerService {

    private final ExcelLogStore logStore;
    private final ExcelPracticeProperties props;

    /**
     * 生成一份 N 行用户导入文件（供演示与测试）。
     */
    public byte[] generateBytes(int rows) {
        int safeRows = Math.max(1, Math.min(rows, 100_000));
        List<UserHead> heads = new ArrayList<>(safeRows);
        for (int i = 1; i <= safeRows; i++) {
            heads.add(toHead(DemoData.user(i)));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, UserHead.class)
                .sheet("用户导入")
                .doWrite(heads);
        return out.toByteArray();
    }

    /**
     * 监听器导入演示：生成 N 行 → AnalysisEventListener 流式读 → 按批落库。
     */
    public Map<String, Object> importDemo(int rows) {
        int safeRows = Math.max(1, Math.min(rows, 100_000));
        int batchSize = props.getBatchSize();
        long start = System.currentTimeMillis();
        UserBatchListener listener = new UserBatchListener(batchSize);
        try {
            EasyExcel.read(new ByteArrayInputStream(generateBytes(safeRows)))
                    .head(UserHead.class)
                    .registerReadListener(listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }
        long costMs = System.currentTimeMillis() - start;
        logStore.add("import", "listener", "监听器批量导入 " + safeRows + " 行", listener.getTotal(), costMs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", listener.getTotal());
        result.put("batchSize", batchSize);
        result.put("batchCount", listener.getBatchStats().size());
        result.put("batches", listener.getBatchStats());
        result.put("costMs", costMs);
        result.put("peakMemoryHint", "内存峰值 ≈ batchSize(" + batchSize + ") 行对象，与文件总行数无关");
        result.put("tip", "invoke() 逐行回调，攒满 " + batchSize + " 行批量 insert 一次，全部读完把最后一批补齐。");
        return result;
    }

    /**
     * 监听器机制速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", "doReadSync() 把整个文件读成 List，几十万行会 OOM；监听器是 SAX 式流式逐行回调，内存恒定。");
        result.put("callbacks", new LinkedHashMap<String, Object>() {{
            put("invoke(T, ctx)", "每解析一行回调一次，在这里做校验/转换/攒批");
            put("doAfterAllAnalysed(ctx)", "全部读完回调，把最后一批补齐、发通知");
            put("invokeHeadMap(map, ctx)", "表头回调，可校验表头是否与模板一致");
            put("onException(e, ctx)", "单行解析异常回调，可跳过坏行继续读");
            put("hasNext(ctx)", "控制是否继续读（可提前终止，实现「只读前 N 行」）");
        }});
        result.put("batch", new String[]{
                "攒批：每 batchSize 行批量 insert 一次（JDBC 批量 / MP saveBatch）",
                "提交：小文件一把提交，大文件建议每批提交并记录「已处理到第几行」",
                "断点：进程挂了从记录的行号续传，避免整批重来"
        });
        result.put("tip", "行号溯源用 ctx.readRowHolder().getRowIndex()（物理行号 0 起，用户可见行号 +1）。");
        return result;
    }

    private UserHead toHead(com.example.excel.support.UserRow row) {
        UserHead head = new UserHead();
        head.setId(row.getId());
        head.setName(row.getName());
        head.setDepartment(row.getDepartment());
        head.setSalary(row.getSalary());
        head.setHireDate(row.getHireDate());
        head.setActive(row.getActive());
        return head;
    }
}
