package com.example.mail.support;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 最近发送记录的内存存储。
 *
 * 教学项目不引入数据库：用有界内存队列保存最近 50 封，前端面板可随时查看
 * 每封邮件的主题、收件人、大小与发送耗时，直观对比不同构造方式的差异。
 */
@Component
public class MailRecordStore {

    /** 最多保留的记录数 */
    private static final int MAX_RECORDS = 50;

    private final AtomicLong seq = new AtomicLong();
    private final List<MailRecord> records = Collections.synchronizedList(new LinkedList<>());

    /**
     * 追加一条记录，超出上限时淘汰最旧的一条。
     */
    public MailRecord add(MailRecord record) {
        record.setId(seq.incrementAndGet());
        synchronized (records) {
            records.add(0, record);
            while (records.size() > MAX_RECORDS) {
                records.remove(records.size() - 1);
            }
        }
        return record;
    }

    /**
     * 最近发送记录（新的在前）。
     */
    public List<MailRecord> recent() {
        synchronized (records) {
            return new LinkedList<>(records);
        }
    }
}
