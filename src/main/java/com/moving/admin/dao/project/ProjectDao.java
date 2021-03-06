package com.moving.admin.dao.project;

import com.moving.admin.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectDao extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // 获取客户关联的项目数
    @Query("select count(id) from Project where customerId=:customerId")
    Long getCountByCustomerId(@Param("customerId") Long customerId);

    // 获取所有离职总监创建的项目
    List<Project> findAllByCreateUserId(Long createUserId);

}
