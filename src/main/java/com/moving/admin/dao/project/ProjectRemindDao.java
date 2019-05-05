package com.moving.admin.dao.project;

import com.moving.admin.entity.project.ProjectRemind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectRemindDao extends JpaRepository<ProjectRemind, Long>, JpaSpecificationExecutor<ProjectRemind> {

    List<ProjectRemind> findAllByCreateUserId(Long createUserId);

    List<ProjectRemind> findAllByProjectTalentIdOrderByCreateTimeDesc(Long projectTalentId);

    // 进展人才某状态的跟踪记录
    List<ProjectRemind> findAllByProjectTalentIdAndStatusOrderByCreateTimeDesc(Long projectTalentId, Integer status);

}
