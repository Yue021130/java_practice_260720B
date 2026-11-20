package com.example.epc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.epc.common.BusinessException;
import com.example.epc.dto.DeptSaveDTO;
import com.example.epc.dto.DeptVO;
import com.example.epc.entity.Dept;
import com.example.epc.mapper.DeptMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门 Service：部门树数据与 CRUD。
 *
 * <p>部门是数据权限的维度，删除部门会影响 DataScopeResolver 的
 * 子孙部门递归结果——本章通过 PermissionCache 的 TTL 与角色维护时的
 * evictAll 兜底收敛，不做跨层联动清理。</p>
 */
@Slf4j
@Service
public class DeptService {

    @Resource
    private DeptMapper deptMapper;

    /** 全部部门（平铺，前端自行建树；用户表单的下拉树也用这份数据） */
    public List<DeptVO> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSortOrder))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    public void save(DeptSaveDTO dto) {
        Dept dept = new Dept();
        BeanUtils.copyProperties(dto, dept);
        dept.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        deptMapper.insert(dept);
    }

    public void update(DeptSaveDTO dto) {
        Dept dept = deptMapper.selectById(dto.getId());
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BusinessException("父部门不能是自己");
        }
        BeanUtils.copyProperties(dto, dept);
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        if (deptMapper.selectById(id) == null) {
            throw new BusinessException("部门不存在");
        }
        // 有子部门不允许删：自关联树删掉父节点会留孤儿，数据权限递归也会断
        Long children = deptMapper.selectCount(
                new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在子部门，不能删除");
        }
        deptMapper.deleteById(id);
    }

    private DeptVO toVO(Dept dept) {
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(dept, vo);
        return vo;
    }
}
