package com.example.os.excelimport;

import com.example.os.config.PracticeProperties;
import com.example.os.domain.ImportRow;
import com.example.os.domain.ImportRow.ImportedOrder;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 07 Excel 导入校验：演示 Optional 逐字段校验 + Stream 聚合错误信息。
 *
 * <p>真实场景：运营后台批量导入订单/用户，需要逐行校验字段格式，汇总错误行号与原因，
 * 只有当空值率低于阈值时才允许落库。Optional 非常适合把“字段可能为空”的校验写成链式表达式。</p>
 */
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final MockDataRepository repository;
    private final PracticeProperties properties;

    /**
     * 校验并转换 Excel 导入数据。
     *
     * @return 校验结果：成功行、失败行、空值率、是否允许落库
     */
    public Map<String, Object> validate() {
        List<ImportRow> rows = repository.getImportRows();

        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();

        // 1. 逐行校验：每行返回 Optional<ImportedOrder>，成功则收集，失败记录错误。
        for (ImportRow row : rows) {
            Optional<ImportedOrder> converted = validateRow(row);
            if (converted.isPresent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rowNum", row.getRowNum());
                m.put("data", converted.get());
                successList.add(m);
            } else {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rowNum", row.getRowNum());
                m.put("errorMsg", row.getErrorMsg());
                m.put("raw", row);
                failList.add(m);
            }
        }

        // 2. 空值率统计：统计所有字段中空值的比例。
        long emptyCount = rows.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getName(), r.getAge(), r.getEmail(), r.getPhone(), r.getAmount()))
                .filter(this::isEmptyValue)
                .count();
        long totalFields = rows.size() * 5L;
        int emptyRate = totalFields == 0 ? 0 : (int) (emptyCount * 100 / totalFields);
        boolean allowPersist = emptyRate <= properties.getExcelMaxEmptyRate();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", rows.size());
        result.put("successRows", successList.size());
        result.put("failRows", failList.size());
        result.put("emptyCount", emptyCount);
        result.put("emptyRatePercent", emptyRate);
        result.put("maxAllowedRatePercent", properties.getExcelMaxEmptyRate());
        result.put("allowPersist", allowPersist);
        result.put("successList", successList);
        result.put("failList", failList);
        result.put("interviewNote", "Optional 把每个字段的‘非空 + 格式 + 范围’校验链式表达；Stream 把结果聚合成成功/失败两份清单。");
        return result;
    }

    /**
     * 单行校验：任何一个字段不通过就返回 Optional.empty()，并在 ImportRow 上记录错误。
     */
    private Optional<ImportedOrder> validateRow(ImportRow row) {
        List<String> errors = new ArrayList<>();

        // 3. name：非空校验。
        String name = Optional.ofNullable(row.getName())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> {
                    errors.add("姓名不能为空");
                    return null;
                });

        // 4. age：正整数校验。
        Integer age = Optional.ofNullable(row.getAge())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(this::parseIntOpt)
                .filter(a -> a > 0 && a < 150)
                .orElseGet(() -> {
                    errors.add("年龄必须是 1~149 的整数");
                    return null;
                });

        // 5. email：格式校验。
        String email = Optional.ofNullable(row.getEmail())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(this::isEmail)
                .orElseGet(() -> {
                    errors.add("邮箱格式不正确");
                    return null;
                });

        // 6. phone：非空校验（简单 11 位手机号）。
        String phone = Optional.ofNullable(row.getPhone())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.matches("\\d{11}"))
                .orElseGet(() -> {
                    errors.add("手机号必须是 11 位数字");
                    return null;
                });

        // 7. amount：非负金额校验。
        BigDecimal amount = Optional.ofNullable(row.getAmount())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(this::parseDecimalOpt)
                .filter(a -> a.compareTo(BigDecimal.ZERO) >= 0)
                .orElseGet(() -> {
                    errors.add("金额必须是非负数字");
                    return null;
                });

        // 8. 有任意错误就记录并返回 empty。
        if (!errors.isEmpty()) {
            row.setErrorMsg(String.join("；", errors));
            return Optional.empty();
        }

        return Optional.of(ImportedOrder.builder()
                .name(name)
                .age(age)
                .email(email)
                .phone(phone)
                .amount(amount)
                .build());
    }

    private boolean isEmptyValue(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private Optional<Integer> parseIntOpt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parseDecimalOpt(String s) {
        try {
            return Optional.of(new BigDecimal(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "Excel 导入校验");
        result.put("pattern", "Optional 链式校验单字段 → 错误聚合 → Stream 输出成功/失败两份清单");
        result.put("keyPoints", new String[]{
                "每个字段用 ofNullable(...).map(trim).filter(规则).orElseGet(记录错误) 表达",
                "orElseGet 里可以写副作用（记录错误），这是少数允许副作用的地方",
                "Stream.flatMap + Stream.of 可以把多行多字段展平后统计空值率",
                "校验失败时返回 Optional.empty()，比抛异常更符合批量导入场景"
        });
        result.put("trap", "不要在校验链里直接抛异常，否则一行报错就中断整个导入；应收集全部错误后统一返回。");
        return result;
    }
}
