package com.example.eep.repository;

import com.example.eep.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统用户数据访问层。
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    boolean existsByRealname(String realname);

    boolean existsByWorkNo(String workNo);
}
