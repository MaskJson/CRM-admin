package com.moving.admin.service;

import com.moving.admin.dao.natives.ProjectNative;
import com.moving.admin.dao.project.ProjectDao;
import com.moving.admin.dao.project.ProjectRemindDao;
import com.moving.admin.dao.project.ProjectReportDao;
import com.moving.admin.dao.project.ProjectTalentDao;
import com.moving.admin.dao.talent.TalentDao;
import com.moving.admin.entity.project.Project;
import com.moving.admin.entity.project.ProjectRemind;
import com.moving.admin.entity.project.ProjectReport;
import com.moving.admin.entity.project.ProjectTalent;
import com.moving.admin.entity.talent.Talent;
import com.moving.admin.exception.WebException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService extends AbstractService {

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ProjectTalentDao projectTalentDao;

    @Autowired
    private ProjectRemindDao projectRemindDao;

    @Autowired
    private ProjectReportDao projectReportDao;

    @Autowired
    private ProjectNative projectNative;

    @Autowired
    private TalentDao talentDao;

    @Autowired
    private CommonService commonService;

    // 编辑项目
    public Long save(Project project) {
        if (project.getId() == null) {
            project.setCreateTime(new Date(System.currentTimeMillis()));
        } else {
            project.setUpdateTime(new Date(System.currentTimeMillis()));
        }
        Long departmentId = commonService.addDepartmentFromTalentInfo(project.getCustomerId(), project.getDepartment());
        project.setDepartmentId(departmentId);
        projectDao.save(project);
        return project.getId();
    }

    // 获取项目详情
    public Project getById(Long id) {
        return projectDao.findById(id).get();
    }

    // 关注装修改
    public void toggleFollow(Long id, Boolean follow) {
        Project project = projectDao.findById(id).get();
        if (project != null) {
            project.setFollow(follow);
            save(project);
        }
    }

    // 添加进展人才
    @Transactional
    public Long saveProjectTalent(ProjectTalent projectTalent) {
        Date date = new Date(System.currentTimeMillis());
        ProjectTalent old = projectTalentDao.findProjectTalentByProjectIdAndTalentId(projectTalent.getProjectId(), projectTalent.getTalentId());
        // 若已存在，重置状态
        Long talentId = projectTalent.getTalentId();
        if (talentId != null) {
            Talent talent = talentDao.findById(talentId).get();
            if (talent != null) {
                List<Integer> status = new ArrayList<Integer>();
                status.add(7);
                status.add(8);
                List<ProjectTalent> projectTalents = projectTalentDao.findAllByTalentIdAndCreateUserIdNotAndStatusNotIn(talentId, talent.getCreateUserId(), status);
                // 若该人才被其他用户列为项目进展人才了，则设为普通人才
                if (projectTalents.size() > 0) {
                    talent.setType(0);
                    talent.setFollowUserId(null);
                    talent.setUpdateTime(new Date(System.currentTimeMillis()));
                    talentDao.save(talent);
                }
            }
        }
        if (old != null) {
            old.setStatus(0);
            old.setType(1);
            old.setUpdateTime(date);
            projectTalentDao.save(old);
            return old.getId();
        } else {
            if (projectTalent.getId() == null) {
                projectTalent.setCreateTime(date);
            } else {
                projectTalent.setUpdateTime(date);
            }
            projectTalentDao.save(projectTalent);
            return projectTalent.getId();
        }
    }

    // 添加进展人才跟踪，并同时修改对应人才状态
    @Transactional
    public Long addProjectRemind(ProjectRemind projectRemind) {
        projectRemindDao.save(projectRemind);
        ProjectTalent projectTalent = projectTalentDao.findById(projectRemind.getProjectTalentId()).get();
        // 跟进后修改人才进展状态
        if (projectTalent != null) {
            projectTalent.setType(projectRemind.getType());
            projectTalent.setStatus(projectRemind.getStatus());
            projectTalent.setUpdateTime(new Date(System.currentTimeMillis()));
            projectTalentDao.save(projectTalent);
        }
        return projectRemind.getId();
    }

    // 项目总监-推荐给客户二次审核
    @Transactional
    public void reviewToCustomer(Long id, Boolean flag) {
        ProjectTalent projectTalent = projectTalentDao.findById(id).get();
        if (projectTalent != null && projectTalent.getType() == 100) {
            projectTalent.setType(1);
            projectTalent.setStatus(flag ? 1 : 8);
            projectTalent.setUpdateTime(new Date(System.currentTimeMillis()));
            projectTalentDao.save(projectTalent);
        }
    }

    // 诊断报告所需数据，获取各个状态的人数
    public Map<String, Object> getProjectTalentCounts(Long projectId) {
        Map<String, Object> map = new HashMap();
        List<ProjectTalent> all = projectTalentDao.findAllByProjectId(projectId);
        // list 分组过滤
        Map<Integer, List<ProjectTalent>> filter = all.stream().collect(
                Collectors.groupingBy(ProjectTalent::getStatus)
        );
        map.put("allCount", all.size());
        map.put("recommendCount", filter.get(1) != null ? filter.get(1).size() : 0);
        map.put("interviewCount", filter.get(3) != null ? filter.get(3).size() : 0);
        map.put("offerCount", filter.get(4) != null ? filter.get(4).size() : 0);
        map.put("workingCount", filter.get(5) != null ? filter.get(5).size() : 0);
        map.put("qualityCount", filter.get(6) != null ? filter.get(6).size() : 0);
        map.put("qualityPassCount", filter.get(7) != null ? filter.get(7).size() : 0);
        return map;
    }

    // 获取项目诊断记录
    public List<Map<String, Object>> getReportList(Long projectId) {
        return projectNative.getDiagnosisByProjectId(projectId);
    }

    // 添加项目诊断报告
    public Long addProjectReport(ProjectReport report) {
        report.setCreateTime(new Date(System.currentTimeMillis()));
        projectReportDao.save(report);
        return report.getId();
    }

}