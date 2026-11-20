package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门出参：树节点结构，children 由前端或后端建树填充。
 */
@Data
public class DeptVO implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    private Integer sortOrder;

    private List<DeptVO> children;
}
