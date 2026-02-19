package com.oms.audit.repository;

import com.oms.audit.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerInfoRepository extends JpaRepository<ServerInfo, Long> {
    List<ServerInfo> findByAssignedUserId(Long userId);
}
