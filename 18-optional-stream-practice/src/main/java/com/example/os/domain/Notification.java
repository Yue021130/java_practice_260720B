package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知消息实体：演示 Optional.ifPresent + Stream 过滤。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private Boolean read;
    private LocalDateTime createTime;
}
