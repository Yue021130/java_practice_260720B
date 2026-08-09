package com.example.unsafe.memory;

import com.example.unsafe.common.UnsafeBizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 02. 堆外内存：allocateMemory / putInt / getInt / setMemory / copyMemory / freeMemory。
 *
 * <p>堆外内存（off-heap）不在 JVM 堆里，不参与 GC，必须手动 freeMemory，
 * 这正是 Netty 的 PooledByteBufAllocator、Kafka、Cassandra 等高性能组件
 * 用 Unsafe 做大块缓冲/缓存的原因。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final Unsafe unsafe;

    /**
     * 分配一块堆外内存，写入 int 数组再读回，最后释放。
     */
    public Map<String, Object> allocate(int count) {
        if (count <= 0 || count > 100000) {
            throw new UnsafeBizException("count 需在 1 ~ 100000 之间");
        }
        long address = 0L;
        try {
            // 每个 int 占 4 字节；Unsafe 要求按 8 字节对齐，这里按 count*4 + 8 分配
            long size = count * 4L + 8L;
            address = unsafe.allocateMemory(size);
            log.info("分配堆外内存：address={} (0x{})，size={} 字节", address, Long.toHexString(address), size);

            List<Integer> written = new ArrayList<>();
            List<Integer> readBack = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int value = i * 10 + 1;               // 1, 11, 21 ...
                unsafe.putInt(address + i * 4L, value); // 按偏移写入
                written.add(value);
            }
            for (int i = 0; i < count; i++) {
                readBack.add(unsafe.getInt(address + i * 4L)); // 按偏移读回
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("address", "0x" + Long.toHexString(address));
            result.put("sizeBytes", size);
            result.put("written", written);
            result.put("readBack", readBack);
            result.put("equal", written.equals(readBack));
            result.put("tip", "putInt(地址 + i*4) 按字节偏移读写；地址是裸内存地址，不经过 JVM 堆。");
            return result;
        } catch (Exception e) {
            throw new UnsafeBizException("堆外内存读写失败：" + e.getMessage(), e);
        } finally {
            if (address != 0L) {
                unsafe.freeMemory(address); // 必须手动释放，否则内存泄漏
                log.info("已释放堆外内存：0x{}", Long.toHexString(address));
            }
        }
    }

    /**
     * setMemory 批量填充 + copyMemory 内存拷贝，用字节十六进制验证。
     */
    public Map<String, Object> setCopy() {
        long src = 0L;
        long dst = 0L;
        try {
            long size = 16L;
            src = unsafe.allocateMemory(size);
            dst = unsafe.allocateMemory(size);

            // setMemory：把 src 起 16 字节全部填充为 0x5A
            unsafe.setMemory(src, size, (byte) 0x5A);
            // copyMemory：把 src 的 16 字节整体拷贝到 dst
            unsafe.copyMemory(src, dst, size);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("srcHex", hexDump(src, 16));
            result.put("dstHex", hexDump(dst, 16));
            result.put("same", sameBytes(src, dst, 16));
            result.put("tip", "setMemory 常用于对象初始化/清零大块缓冲；copyMemory 是 memcpy 级别，比逐字节循环快得多。");
            return result;
        } catch (Exception e) {
            throw new UnsafeBizException("内存填充/拷贝失败：" + e.getMessage(), e);
        } finally {
            if (src != 0L) {
                unsafe.freeMemory(src);
            }
            if (dst != 0L) {
                unsafe.freeMemory(dst);
            }
        }
    }

    /**
     * 堆外内存生命周期：分配 N 块 1MB 堆外内存，观察“堆”几乎不变，
     * 说明堆外内存不受 GC 管理，然后手动释放全部——演示“忘了 free 会怎样”。
     */
    public Map<String, Object> leakDemo(int blocks) {
        if (blocks <= 0 || blocks > 20) {
            throw new UnsafeBizException("blocks 需在 1 ~ 20 之间");
        }
        long blockSize = 1024L * 1024L; // 1 MB
        List<String> addresses = new ArrayList<>();
        long heapBefore = usedHeapMb();
        long start = System.currentTimeMillis();
        for (int i = 0; i < blocks; i++) {
            long addr = unsafe.allocateMemory(blockSize);
            unsafe.setMemory(addr, blockSize, (byte) 0x01); // 实际触碰物理页
            addresses.add("0x" + Long.toHexString(addr));
        }
        long allocatedMs = System.currentTimeMillis() - start;

        long heapAfterAlloc = usedHeapMb();
        // 演示结束：全部释放（真实泄漏是“忘了释放”）
        long releaseStart = System.currentTimeMillis();
        for (String a : addresses) {
            unsafe.freeMemory(Long.parseLong(a.substring(2), 16));
        }
        long releasedMs = System.currentTimeMillis() - releaseStart;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blocks", blocks);
        result.put("blockSizeMb", 1);
        result.put("totalOffHeapMb", blocks * 1);
        result.put("addresses", addresses);
        result.put("heapUsedBeforeMb", heapBefore);
        result.put("heapUsedAfterAllocMb", heapAfterAlloc);
        result.put("heapGrewMb", heapAfterAlloc - heapBefore);
        result.put("allocatedMs", allocatedMs);
        result.put("releasedMs", releasedMs);
        result.put("tip", "堆外内存不在堆里：分配 1MB×N 块，堆几乎不长（heapGrewMb 很小）。"
                + "它不归 GC 管，只有 freeMemory 能释放；本实验结束已全部释放。"
                + "真实事故：Netty/Kafka 配置不当时堆外内存只涨不降，最终 OOM/被系统杀掉。");
        return result;
    }

    /** 当前堆已用内存（MB） */
    private long usedHeapMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
    }

    /** 从地址起读 n 字节，输出十六进制文本 */
    private String hexDump(long addr, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int b = unsafe.getByte(addr + i) & 0xFF;
            if (i > 0 && i % 8 == 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /** 比较两块内存前 n 字节是否一致 */
    private boolean sameBytes(long a, long b, int n) {
        for (int i = 0; i < n; i++) {
            if (unsafe.getByte(a + i) != unsafe.getByte(b + i)) {
                return false;
            }
        }
        return true;
    }
}
