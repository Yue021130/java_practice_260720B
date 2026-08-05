package com.example.exception.commonex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 常见异常场景服务。
 *
 * 演示 Java 开发中高频出现的运行时异常及其正确防御方式。
 */
@Slf4j
@Service
public class CommonExceptionScenarioService {

    /**
     * NPE 防御：装箱空值、链式调用、Map.get。
     */
    public Map<String, Object> npeDefense() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        // 坑 1：自动拆箱 null
        Integer a = null;
        try {
            int b = a; // NullPointerException
        } catch (NullPointerException e) {
            tips.add("自动拆箱 null 会 NPE：Integer a = null; int b = a;");
        }

        // 坑 2：链式调用
        User user = null;
        String city = Optional.ofNullable(user)
                .map(User::getAddress)
                .map(Address::getCity)
                .orElse("未知");
        tips.add("链式调用用 Optional: " + city);

        // 坑 3：Map.get 后强转/操作
        Map<String, String> map = new HashMap<>();
        String value = map.get("key");
        tips.add("Map.get 不存在返回 null，判断后再使用：" + Objects.isNull(value));

        // 坑 4：equals 反写
        String s = "hello";
        if ("hello".equals(s)) {
            tips.add("常量放 equals 前面，避免变量为 null 时 NPE");
        }

        result.put("tips", tips);
        return result;
    }

    /**
     * ClassCastException：泛型擦除与强转。
     */
    public Map<String, Object> classCast() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        // 泛型擦除：编译后 List<Integer> 与 List<String> 都是 List
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        // 通过反射绕过泛型检查
        List rawList = intList;
        rawList.add("abc");

        try {
            for (Integer i : intList) {
                log.info("i = {}", i);
            }
        } catch (ClassCastException e) {
            tips.add("泛型只在编译期有效，运行时擦除，混类型会 ClassCastException");
        }

        Object obj = "hello";
        if (obj instanceof Integer) {
            int i = (Integer) obj;
            tips.add("强转前用 instanceof: " + i);
        } else {
            tips.add("obj 不是 Integer，没有强转");
        }

        result.put("tips", tips);
        return result;
    }

    /**
     * NumberFormatException 与 BigDecimal 构造坑。
     */
    public Map<String, Object> numberFormat() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        try {
            int i = Integer.parseInt("abc");
        } catch (NumberFormatException e) {
            tips.add("Integer.parseInt 非法字符串抛 NumberFormatException");
        }

        try {
            // double 构造会导致精度问题，且无法用字符串构造来避免
            BigDecimal d = new BigDecimal(0.1);
            tips.add("new BigDecimal(0.1) 实际值: " + d);
        } catch (Exception e) {
            tips.add("异常：" + e.getMessage());
        }

        BigDecimal correct = new BigDecimal("0.1");
        tips.add("推荐用字符串构造：new BigDecimal(\"0.1\") = " + correct);

        result.put("tips", tips);
        return result;
    }

    /**
     * IndexOutOfBoundsException：数组、List、String 越界。
     */
    public Map<String, Object> indexOutOfBounds() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        int[] arr = {1, 2, 3};
        try {
            int x = arr[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            tips.add("数组越界：arr[3]，长度只有 3");
        }

        List<String> list = Arrays.asList("a", "b");
        try {
            String s = list.get(2);
        } catch (IndexOutOfBoundsException e) {
            tips.add("List 越界：list.get(2)，size = 2");
        }

        String str = "hi";
        try {
            char c = str.charAt(2);
        } catch (StringIndexOutOfBoundsException e) {
            tips.add("字符串越界：str.charAt(2)，长度 = 2");
        }

        tips.add("防御：操作前检查 size/length，或用 JDK 安全方法如 getOrDefault");
        result.put("tips", tips);
        return result;
    }

    /**
     * ConcurrentModificationException：fail-fast 机制。
     */
    public Map<String, Object> concurrentModification() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        try {
            for (String s : list) {
                if ("b".equals(s)) {
                    list.remove(s); // 触发 CME
                }
            }
        } catch (ConcurrentModificationException e) {
            tips.add("for-each 中直接 remove 会触发 ConcurrentModificationException（fail-fast）");
        }

        // 正确方式 1：Iterator.remove
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if ("c".equals(it.next())) {
                it.remove();
            }
        }
        tips.add("正确方式 1：Iterator.remove()");

        // 正确方式 2：倒序 for
        for (int i = list.size() - 1; i >= 0; i--) {
            if ("d".equals(list.get(i))) {
                list.remove(i);
            }
        }
        tips.add("正确方式 2：倒序 for 索引删除");

        // 正确方式 3：removeIf（Java 8）
        list.removeIf("a"::equals);
        tips.add("正确方式 3：list.removeIf()");

        result.put("remaining", list);
        result.put("tips", tips);
        return result;
    }

    /**
     * UnsupportedOperationException：不可变集合坑。
     */
    public Map<String, Object> unsupportedOperation() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        List<String> asList = Arrays.asList("a", "b");
        try {
            asList.add("c");
        } catch (UnsupportedOperationException e) {
            tips.add("Arrays.asList 返回固定大小列表，add/remove 会抛 UnsupportedOperationException");
        }

        List<String> singleton = Collections.singletonList("x");
        try {
            singleton.add("y");
        } catch (UnsupportedOperationException e) {
            tips.add("Collections.singletonList 不可变");
        }

        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("m", "n")));
        try {
            unmodifiable.add("o");
        } catch (UnsupportedOperationException e) {
            tips.add("Collections.unmodifiableList 不可修改");
        }

        tips.add("需要可变列表时：new ArrayList<>(Arrays.asList(...))");
        result.put("tips", tips);
        return result;
    }

    /**
     * NoSuchElementException：Optional.get / Iterator.next。
     */
    public Map<String, Object> noSuchElement() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        Optional<String> empty = Optional.empty();
        try {
            empty.get();
        } catch (NoSuchElementException e) {
            tips.add("Optional.empty().get() 抛 NoSuchElementException");
        }

        String value = empty.orElse("默认值");
        tips.add("推荐用 orElse / orElseGet / orElseThrow: " + value);

        Iterator<String> it = Collections.emptyIterator();
        try {
            it.next();
        } catch (NoSuchElementException e) {
            tips.add("空 Iterator.next() 抛 NoSuchElementException，调用前先 hasNext()");
        }

        result.put("tips", tips);
        return result;
    }

    /**
     * StackOverflowError：受控演示，不会真正崩溃 JVM。
     */
    public Map<String, Object> stackOverflow() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            recurse(0);
        } catch (StackOverflowError e) {
            result.put("caught", true);
            result.put("message", e.getMessage());
            result.put("tip", "递归/循环依赖初始化可能导致 StackOverflowError，应检查终止条件");
        }
        return result;
    }

    private void recurse(int depth) {
        if (depth > 10000) {
            return;
        }
        recurse(depth + 1);
    }

    /**
     * OutOfMemoryError：不真正触发 OOM，给出原理说明与代码示例。
     */
    public Map<String, Object> oom() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("heapOom", "List<byte[]> list = new ArrayList<>(); while(true) list.add(new byte[1024*1024]);");
        result.put("metaspaceOom", "大量动态生成类 / CGLIB 代理会撑爆 Metaspace");
        result.put("offHeapOom", "ByteBuffer.allocateDirect / Netty 堆外内存泄漏，需显式回收");
        result.put("solution", "用 -Xmx / -XX:MaxMetaspaceSize 限制；结合 MAT/VisualVM 分析 dump");
        return result;
    }

    /**
     * ClassNotFoundException vs NoClassDefFoundError。
     */
    public Map<String, Object> classNotFound() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        try {
            Class.forName("com.example.NotExist");
        } catch (ClassNotFoundException e) {
            tips.add("ClassNotFoundException：编译期不存在或类加载器找不到，常见于反射 / Class.forName");
        }

        tips.add("NoClassDefFoundError：编译期存在，运行期缺失，如依赖 jar 冲突、打包遗漏");
        tips.add("ClassNotFoundException 是 checked；NoClassDefFoundError 是 Error");

        result.put("tips", tips);
        return result;
    }

    /**
     * AssertionError：assert 关键字。
     */
    public Map<String, Object> assertion() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("note", "assert 默认被 JVM 关闭，需用 -ea 参数启用");
        result.put("example", "assert x > 0 : \"x 必须大于 0\";");
        result.put("production", "生产代码不建议用 assert 做业务校验，因为可能被关闭；推荐用 if + 抛异常");
        return result;
    }

    // 辅助类
    static class User {
        private final Address address;
        User(Address address) { this.address = address; }
        Address getAddress() { return address; }
    }

    static class Address {
        private final String city;
        Address(String city) { this.city = city; }
        String getCity() { return city; }
    }
}
