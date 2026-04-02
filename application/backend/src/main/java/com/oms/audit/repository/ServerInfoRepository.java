package com.oms.audit.repository;

import com.oms.audit.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 服务器信息的数据访问层接口
 * 继承 JpaRepository，提供对 ServerInfo 实体类的基本 CRUD 功能
 */
@Repository
public interface ServerInfoRepository extends JpaRepository<ServerInfo, Long> {
    
    /**
     * 根据分配的用户 ID 查询其名下管理的服务器列表
     *
     * @param userId 用户的唯一标识 ID
     * @return 该用户负责管理的服务器列表
     */
    List<ServerInfo> findByAssignedUserId(Long userId);
}
