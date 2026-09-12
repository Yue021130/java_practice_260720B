package com.example.sfp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sfp.common.BusinessException;
import com.example.sfp.dto.ResourceSaveDTO;
import com.example.sfp.dto.ResourceVO;
import com.example.sfp.entity.BizResource;
import com.example.sfp.mapper.ResourceMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 资源 Service：CRUD，给拦截器提供业务流量。
 */
@Service
public class ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    public Page<ResourceVO> page(long current, long size, String name, Integer type) {
        LambdaQueryWrapper<BizResource> wrapper = new LambdaQueryWrapper<BizResource>()
                .like(name != null && !name.isEmpty(), BizResource::getName, name)
                .eq(type != null, BizResource::getType, type)
                .orderByAsc(BizResource::getSortOrder)
                .orderByDesc(BizResource::getCreateTime);
        Page<BizResource> entityPage = resourceMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ResourceVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public ResourceVO getById(Long id) {
        return toVO(resourceMapper.selectById(id));
    }

    public ResourceVO save(ResourceSaveDTO dto) {
        BizResource resource = new BizResource();
        BeanUtils.copyProperties(dto, resource);
        resource.setId(null);
        if (resource.getStatus() == null) {
            resource.setStatus(1);
        }
        if (resource.getSortOrder() == null) {
            resource.setSortOrder(0);
        }
        resourceMapper.insert(resource);
        return toVO(resource);
    }

    public ResourceVO update(ResourceSaveDTO dto) {
        BizResource resource = resourceMapper.selectById(dto.getId());
        if (resource == null) {
            throw new BusinessException(400, "资源不存在");
        }
        BeanUtils.copyProperties(dto, resource);
        resourceMapper.updateById(resource);
        return toVO(resourceMapper.selectById(dto.getId()));
    }

    public void delete(Long id) {
        resourceMapper.deleteById(id);
    }

    private ResourceVO toVO(BizResource resource) {
        if (resource == null) {
            return null;
        }
        ResourceVO vo = new ResourceVO();
        BeanUtils.copyProperties(resource, vo);
        return vo;
    }
}
