package com.example.sfp.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源出参。
 */
@Data
public class ResourceVO implements Serializable {

    private Long id;

    private String name;

    private String url;

    private Integer type;

    private Integer status;

    private Integer sortOrder;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
