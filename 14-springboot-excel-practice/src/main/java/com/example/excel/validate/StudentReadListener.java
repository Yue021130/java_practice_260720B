package com.example.excel.validate;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生导入监听器：逐行校验，合法行 / 错误行分开收集。
 *
 * 相比 doReadSync() 一次性读回所有行，用 AnalysisEventListener 可以
 * 拿到每一行的行号（context.readRowHolder().getRowIndex()），
 * 也方便「边读边处理、出错不中断」。
 *
 * 行号换算：getRowIndex() 是 sheet 里的物理行号（0 起，表头占第 0 行），
 * 展示给用户要 +1（Excel 里表头就是第 1 行，第一行数据是第 2 行）。
 */
public class StudentReadListener extends AnalysisEventListener<StudentHead> {

    /** 合法行 */
    private final List<StudentHead> validRows = new ArrayList<>();

    /** 错误行：{row, studentNo, name, error} */
    private final List<Map<String, Object>> errorRows = new ArrayList<>();

    /** 读取到的总数据行数 */
    private int total = 0;

    @Override
    public void invoke(StudentHead data, AnalysisContext context) {
        total++;
        int sheetRow = context.readRowHolder().getRowIndex(); // 0 起物理行号
        String error = validate(data);
        if (error == null) {
            validRows.add(data);
        } else {
            data.setErrorMsg(error);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("row", sheetRow + 1);          // 转成用户可见的行号
            err.put("studentNo", data.getStudentNo());
            err.put("name", data.getName());
            err.put("error", error);
            errorRows.add(err);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 全部解析完成：这里可以做一次「整体提交/告警」，本演示只统计
    }

    /**
     * 单行业务校验：返回 null 表示通过，否则返回原因。
     */
    private String validate(StudentHead data) {
        if (isBlank(data.getStudentNo())) {
            return "学号不能为空";
        }
        if (!data.getStudentNo().matches("S\\d{4}")) {
            return "学号格式应为 S + 4 位数字（如 S0001）";
        }
        if (isBlank(data.getName())) {
            return "姓名不能为空";
        }
        if (data.getAge() == null || data.getAge() < 18 || data.getAge() > 60) {
            return "年龄必须在 18~60 之间";
        }
        if (isBlank(data.getPhone()) || !data.getPhone().matches("1\\d{10}")) {
            return "手机号应为 1 开头的 11 位数字";
        }
        if (isBlank(data.getMajor())) {
            return "专业不能为空";
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public List<StudentHead> getValidRows() {
        return validRows;
    }

    public List<Map<String, Object>> getErrorRows() {
        return errorRows;
    }

    public int getTotal() {
        return total;
    }
}
