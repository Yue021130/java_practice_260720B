package com.example.comm.pipe;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 09. 基于 IO / 其他通道：管道流 + 跨进程通信思路。
 *
 * - PipedInputStream / PipedOutputStream：字节/字符流形式的单向管道，
 *   一个线程写、另一个线程读（官方不建议单线程两端都用，可能死锁）；
 * - Socket 回环 / MappedByteBuffer 共享内存 / 文件：主要用于跨 JVM / 跨进程，
 *   线程间用属于杀鸡用牛刀，但属于标准答案之一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipeService {

    private final CommLogStore logStore;

    /**
     * Piped 管道流演示：写线程向 PipedOutputStream 写 messages 条消息，
     * 读线程从连接的 PipedInputStream 逐行读回。
     */
    public Map<String, Object> pipedDemo(int messages) {
        int safeMessages = Math.max(1, Math.min(messages, 100));
        Map<String, Object> result = new LinkedHashMap<>();

        PipedOutputStream out = new PipedOutputStream();
        StringBuilder received = new StringBuilder();
        int[] readCount = {0};

        try {
            PipedInputStream in = new PipedInputStream(out);   // 两端连接
            Thread writer = new Thread(() -> {
                try (OutputStreamWriter ow = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                    for (int i = 0; i < safeMessages; i++) {
                        ow.write("消息-" + i + "\n");
                        ow.flush();
                    }
                } catch (Exception e) {
                    log.warn("管道写端异常：{}", e.getMessage());
                }
            }, "pipe-writer");
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        received.append(line).append("\n");
                        readCount[0]++;
                    }
                } catch (Exception e) {
                    log.warn("管道读端异常：{}", e.getMessage());
                }
            }, "pipe-reader");

            long start = System.nanoTime();
            writer.start();
            reader.start();
            writer.join(5000);
            reader.join(5000);
            long totalMs = (System.nanoTime() - start) / 1_000_000;

            result.put("messages", safeMessages);
            result.put("written", safeMessages);
            result.put("read", readCount[0]);
            result.put("consistent", readCount[0] == safeMessages);
            result.put("sample", received.toString().trim());
            result.put("totalMs", totalMs);
            result.put("tip", "写线程写了 " + safeMessages + " 条，读线程从管道另一端读回 " + readCount[0]
                    + " 条（" + (readCount[0] == safeMessages ? "完整" : "缺漏") + "）：Piped 是单向字节/字符流管道，"
                    + "一个线程写、一个线程读。");

            logStore.add("pipe", "piped-demo", 2, readCount[0] == safeMessages, "PipedStream");
            return result;
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 跨进程通道思路（概念速览）。
     */
    public Map<String, Object> crossProcess() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ways", new LinkedHashMap<String, Object>() {{
            put("Socket 回环", "线程/进程都能用：127.0.0.1 上连 TCP/UDP，通用但重；同一 JVM 内用它明显是杀鸡用牛刀");
            put("共享内存", "MappedByteBuffer（FileChannel.map）把文件映射进内存，多个 JVM 读写同一块区域；快但要自己做同步");
            put("文件", "读写同一文件 + 文件锁（FileLock），最简单的跨进程通道，慢但可靠");
            put("MQ / 数据库", "生产环境跨服务通信的标准：消息队列削峰、数据库表做任务队列");
        }});
        result.put("verdict", "同 JVM 线程间：这些都能用但不该用——有更轻的线程内手段（队列/锁/异步），"
                + "管道流是「标准答案之一」，Socket/共享内存留给真正的跨进程场景。");
        result.put("tip", "面试被问「线程间通信还有哪些」：PipedStream 点到为止，再强调「跨进程才用 Socket/共享内存」就圆了。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("what", "PipedInputStream/PipedOutputStream（及对应的 Reader/Writer）：用管道把两个线程连起来，"
                + "一个写一个读，数据像水一样流过去，是「IO 形式的线程通信」");
        result.put("danger", new String[]{
                "官方警告：不要在同一线程里两端都用（同一线程读写管道可能死锁）",
                "单向：只能一边写一边读，要双向就建两根管道",
                "容量受限：管道有缓冲区，写满会阻塞",
                "实际工程几乎不用：有更直接的队列/锁，管道流更多是历史与考试知识点"
        });
        result.put("compare", new LinkedHashMap<String, Object>() {{
            put("线程内", "共享变量 / 锁 / 队列 / 异步 —— 快、轻、同进程可见");
            put("跨进程", "Socket / 共享内存 / 文件 / MQ —— 网络或文件系统开销，但要跨 JVM 只能靠它们");
        }});
        result.put("tip", "一句话：管道流属于「知道有这回事」的标准答案，真用线程通信还是队列和锁。");
        return result;
    }
}
