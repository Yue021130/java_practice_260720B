package com.example.fcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fcp.common.BusinessException;
import com.example.fcp.dto.NoticeSaveDTO;
import com.example.fcp.dto.NoticeVO;
import com.example.fcp.entity.Notice;
import com.example.fcp.mapper.NoticeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 公告 Service：CRUD，给过滤器链提供业务流量。
 */
@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public Page<NoticeVO> page(long current, long size, String title, Integer type, Integer status) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .like(title != null && !title.isEmpty(), Notice::getTitle, title)
                .eq(type != null, Notice::getType, type)
                .eq(status != null, Notice::getStatus, status)
                .orderByAsc(Notice::getSortOrder)
                .orderByDesc(Notice::getCreateTime);
        Page<Notice> entityPage = noticeMapper.selectPage(new Page<>(current, size), wrapper);
        Page<NoticeVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public NoticeVO getById(Long id) {
        return toVO(noticeMapper.selectById(id));
    }

    public NoticeVO save(NoticeSaveDTO dto) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(dto, notice);
        notice.setId(null);
        if (notice.getStatus() == null) {
            notice.setStatus(0);
        }
        if (notice.getSortOrder() == null) {
            notice.setSortOrder(0);
        }
        noticeMapper.insert(notice);
        return toVO(notice);
    }

    public NoticeVO update(NoticeSaveDTO dto) {
        Notice notice = noticeMapper.selectById(dto.getId());
        if (notice == null) {
            throw new BusinessException(400, "公告不存在");
        }
        BeanUtils.copyProperties(dto, notice);
        noticeMapper.updateById(notice);
        return toVO(noticeMapper.selectById(dto.getId()));
    }

    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }

    private NoticeVO toVO(Notice notice) {
        if (notice == null) {
            return null;
        }
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        return vo;
    }
}
