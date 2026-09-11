package com.example.vmp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.vmp.common.BusinessException;
import com.example.vmp.dto.CustomerSaveDTO;
import com.example.vmp.dto.CustomerVO;
import com.example.vmp.entity.Customer;
import com.example.vmp.mapper.CustomerMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 客户 Service：CRUD，出参统一走 CustomerVO 脱敏。
 */
@Service
public class CustomerService {

    @Resource
    private CustomerMapper customerMapper;

    public Page<CustomerVO> page(long current, long size, String name, String phone) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .like(name != null && !name.isEmpty(), Customer::getName, name)
                .like(phone != null && !phone.isEmpty(), Customer::getPhone, phone)
                .orderByDesc(Customer::getCreateTime);
        Page<Customer> entityPage = customerMapper.selectPage(new Page<>(current, size), wrapper);
        Page<CustomerVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public CustomerVO getById(Long id) {
        return toVO(customerMapper.selectById(id));
    }

    public CustomerVO save(CustomerSaveDTO dto) {
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        customer.setId(null);
        if (customer.getGender() == null) {
            customer.setGender(1);
        }
        if (customer.getLevel() == null) {
            customer.setLevel(1);
        }
        customerMapper.insert(customer);
        return toVO(customer);
    }

    public CustomerVO update(CustomerSaveDTO dto) {
        Customer customer = customerMapper.selectById(dto.getId());
        if (customer == null) {
            throw new BusinessException(400, "客户不存在");
        }
        // 编辑时敏感字段留空表示不修改
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            customer.setName(dto.getName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            customer.setPhone(dto.getPhone());
        }
        if (dto.getIdCard() != null && !dto.getIdCard().isEmpty()) {
            customer.setIdCard(dto.getIdCard());
        }
        if (dto.getEmail() != null) {
            customer.setEmail(dto.getEmail());
        }
        if (dto.getAddress() != null) {
            customer.setAddress(dto.getAddress());
        }
        if (dto.getGender() != null) {
            customer.setGender(dto.getGender());
        }
        if (dto.getLevel() != null) {
            customer.setLevel(dto.getLevel());
        }
        if (dto.getRemark() != null) {
            customer.setRemark(dto.getRemark());
        }
        customerMapper.updateById(customer);
        return toVO(customerMapper.selectById(dto.getId()));
    }

    public void delete(Long id) {
        customerMapper.deleteById(id);
    }

    private CustomerVO toVO(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }
}
