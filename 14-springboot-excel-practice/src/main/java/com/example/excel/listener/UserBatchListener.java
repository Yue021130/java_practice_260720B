package com.example.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.excel.basic.UserHead;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户批量导入监听器：每攒够 batchSize 行就「批量落库」一次。
 *
 * 这是导入大数据量的标准姿势，面试必考：
 * - 流式逐行回调 invoke()，内存里永远只有一批（batchSize 行）
 * - 每批一次批量 insert（而不是逐行 insert），吞吐量高一个数量级
 * - 全部读完在 doAfterAllAnalysed() 里把最后一批补齐
 *
 * 对比：doReadSync() 会把整个文件读成一个大 List，几十万行直接撑爆内存。
 */
public class UserBatchListener extends AnalysisEventListener<UserHead> {

    /** 每批行数 */
    private final int batchSize;

    /** 当前攒的一批 */
    private final List<UserHead> batch = new ArrayList<>();

    /** 每批「落库」的统计 */
    private final List<Map<String, Object>> batchStats = new ArrayList<>();

    /** 读到的总行数 */
    private int total = 0;

    /** 批次序号 */
    private int batchNo = 0;

    public UserBatchListener(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void invoke(UserHead data, AnalysisContext context) {
        total++;
        batch.add(data);
        if (batch.size() >= batchSize) {
            persistBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 收尾：最后不满一批的剩余行也落库
        if (!batch.isEmpty()) {
            persistBatch();
        }
    }

    /**
     * 模拟一次批量 insert：真实工程这里是一句 batchInsert SQL 或 saveBatch。
     */
    private void persistBatch() {
        batchNo++;
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("batchNo", batchNo);
        stat.put("rows", batch.size());
        stat.put("action", "INSERT INTO user (id,name,department,salary,hire_date,active) VALUES (...x" + batch.size() + ")");
        batchStats.add(stat);
        batch.clear();
    }

    public List<Map<String, Object>> getBatchStats() {
        return batchStats;
    }

    public int getTotal() {
        return total;
    }
}
