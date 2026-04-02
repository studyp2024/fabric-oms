package com.oms.audit.entity;

import javax.persistence.*;
import lombok.Data;

/**
 * 系统用户实体类
 * 映射数据库中的 users 表，存储系统管理员和运维人员信息
 */
@Entity
@Data
@Table(name = "users")
public class User {
    
    // 主键 ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名，具有唯一性约束
    @Column(unique = true)
    private String username;

    // 用户密码（实际生产环境中应加密存储）
    private String password;

    // 用户角色权限，例如：ADMIN（管理员） 或 OPS（运维人员）
    private String role; 
}
