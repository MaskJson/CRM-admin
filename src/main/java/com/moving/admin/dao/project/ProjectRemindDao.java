package com.moving.admin.dao.project;

import com.moving.admin.entity.project.ProjectRedmind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRemindDao extends JpaRepository<ProjectRedmind, Long>, JpaSpecificationExecutor<ProjectRedmind> {
}
