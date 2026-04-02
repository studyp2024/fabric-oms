package com.oms.audit.repository;

import com.oms.audit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户信息的数据访问层接口
 * 继承 JpaRepository，提供对 User 实体类的基本 CRUD 功能
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户信息
     *
     * @param username 用户的登录名
     * @return 包含用户信息的 Optional 对象（如果找到），否则返回空 Optional
     */
    Optional<User> findByUsername(String username);
}
