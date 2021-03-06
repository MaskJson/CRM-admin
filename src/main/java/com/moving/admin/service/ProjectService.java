package com.moving.admin.service;

import com.moving.admin.dao.customer.DepartmentDao;
import com.moving.admin.dao.natives.ProjectNative;
import com.moving.admin.dao.project.*;
import com.moving.admin.dao.sys.TeamDao;
import com.moving.admin.dao.talent.TalentDao;
import com.moving.admin.dao.talent.TalentRemindDao;
import com.moving.admin.entity.customer.Department;
import com.moving.admin.entity.project.*;
import com.moving.admin.entity.sys.Team;
import com.moving.admin.entity.talent.Talent;
import com.moving.admin.entity.talent.TalentRemind;
import com.moving.admin.exception.WebException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private TalentService talentService;

    @Autowired
    private TeamDao teamDao;

    @Autowired
    private TalentRemindDao talentRemindDao;

    @Autowired
    private ProjectAdviserDao projectAdviserDao;

    // 编辑项目
    @Transactional
    public Long save(Project project) {
        if (project.getId() == null) {
            Team team = teamDao.findTeamByUserIdAndLevel(project.getCreateUserId(), 1);
            if (team != null) {
                project.setTeamId(team.getId());
            } else {
                throw new WebException(400, "您还未拥有团队，请联系管理员分配团队成员", null);
            }
            project.setCreateTime(new Date(System.currentTimeMillis()));
            project.setFirstApplyTime(new Date(System.currentTimeMillis() + 86400000L*21));
        } else {
            project.setUpdateTime(new Date(System.currentTimeMillis()));
        }
        projectDao.save(project);
        Long projectId = project.getId();
        List<ProjectAdviser> list = new ArrayList<>();
        project.getAdvisers().forEach(itemId -> {
            ProjectAdviser projectAdviser = new ProjectAdviser();
            projectAdviser.setProjectId(projectId);
            projectAdviser.setUserId(itemId);
            list.add(projectAdviser);
        });
        projectAdviserDao.deleteAllByProjectId(projectId);
        projectAdviserDao.saveAll(list);
        return project.getId();
    }

    // 获取项目详情
    public Project getById(Long id) {
        Project project = projectDao.findById(id).get();
        project.setAdvisers(projectAdviserDao.findAllByProjectId(id));
        return project;
    }

    // 关注装修改
    public void toggleFollow(Long id, Boolean follow) {
        Project project = projectDao.findById(id).get();
        if (project != null) {
            project.setFollow(follow);
            projectDao.save(project);
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
                talent.setStatus(10);
                List<Integer> status = new ArrayList<Integer>();
                status.add(7);
                status.add(8);
                List<ProjectTalent> projectTalents = projectTalentDao.findAllByTalentIdAndCreateUserIdNotAndStatusNotIn(talentId, talent.getCreateUserId(), status);
                // 若该人才被其他用户列为项目进展人才了，则设为普通人才
                if (projectTalents.size() > 0) {
                    talent.setType(0);
                    talent.setFollowUserId(null);
                    talent.setUpdateTime(new Date(System.currentTimeMillis()));
                }
                talentDao.save(talent);
            }
        }
        Long id;
        if (old != null) {
            old.setStatus(0);
            old.setType(1);
            old.setCreateUserId(projectTalent.getCreateUserId());
            old.setUpdateTime(date);
            projectTalentDao.save(old);
            id = old.getId();
        } else {
            if (projectTalent.getId() == null) {
                projectTalent.setCreateTime(date);
            } else {
                projectTalent.setUpdateTime(date);
            }
            projectTalentDao.save(projectTalent);
            id = projectTalent.getId();
        }
        if (!StringUtils.isEmpty(projectTalent.getRemark())) {
            ProjectRemind remind = new ProjectRemind();
            remind.setRemark(projectTalent.getRemark());
            remind.setRecommendation(projectTalent.getRemark());
            remind.setType(100);
            remind.setStatus(0);
            remind.setRoleId(projectTalent.getRoleId());
            remind.setProjectTalentId(id);
            remind.setCreateUserId(projectTalent.getCreateUserId());
            addProjectRemind(remind);
        }
        // 人才进入项目进展后 将待根据的常规跟踪都置为已跟进
        List<TalentRemind> reminds = talentRemindDao.findAllByTalentIdAndFinish(talentId, false);
        reminds.forEach(r -> {
            r.setFinish(true);
        });
        talentRemindDao.saveAll(reminds);
//        talentRemindDao.finishRemind(talentId);
        return id;
    }

    // 添加进展人才跟踪，并同时修改对应人才状态
    @Transactional
    public ProjectRemind addProjectRemind(ProjectRemind projectRemind) {
        // 查找是否存在已处于其他项目的进展中的入职状态或保证期状态， 若存在，抛异常
        ProjectTalent projectTalent = projectTalentDao.findById(projectRemind.getProjectTalentId()).get();
        List<ProjectTalent> pts = projectTalentDao.findAllByTalentIdAndProjectIdNotAndStatusBetween(projectTalent.getTalentId(), projectTalent.getProjectId(), 5, 6);
        if (projectRemind.getTalentRemind() != null) {
            TalentRemind talentRemind = projectRemind.getTalentRemind();
            talentRemind.setCreateTime(new Date());
            talentRemind.setRemark(StringUtils.isEmpty(projectRemind.getRemark()) ? projectRemind.getKillRemark() : projectRemind.getRemark());
//            talentRemindDao.save(talentRemind);
            talentService.saveRemind(talentRemind);
        }
        if (pts.size() > 0) {
            projectTalent.setStatus(8);
            projectTalent.setType(200);
//            projectTalent.setKillStatus(-1);
            projectTalent.setKillRemark("该人才已在其他项目入职或进入保证期");
            projectTalent.setUpdateTime(new Date());
            projectTalentDao.save(projectTalent);
            return null;
        }
        if (projectRemind.getType() == 100 && projectRemind.getRoleId() == 3) {
            projectRemind.setStatus(1);
            projectRemind.setType(1);
        }
        projectRemind.setCreateTime(new Date());
        Integer remarkStatus = projectRemind.getRemarkStatus();
        Integer type = projectRemind.getType();
        if (type == 16 && remarkStatus != null && remarkStatus > 2) {
            projectRemind.setStatus(remarkStatus);
        }
        projectRemindDao.save(projectRemind);
        // 跟进后修改人才进展状态
        Integer status = projectRemind.getStatus();
        if (projectTalent != null) {
            if (type == 100) {
                projectTalent.setRecommendation(projectRemind.getRecommendation());
            }
            if (projectRemind.getRoleId() == 3 &&  type == 100) {
                projectTalent.setType(1);
                projectTalent.setStatus(1);
            } else if (type != 99){
                projectTalent.setType(type);
                projectTalent.setStatus(projectRemind.getStatus());
            }
            if (status == 6) {
                projectTalent.setProbationTime(projectRemind.getProbationTime());
            } else if (status == 8){
                projectTalent.setKillRemark(projectRemind.getKillRemark());
//                projectTalent.setKillStatus(-1);
            }
//            if (type == 16 && remarkStatus != null && remarkStatus > 2) {
//                if (remarkStatus == 8) {
////                    projectTalent.setKillStatus(-1);
//                }
//                projectTalent.setStatus(remarkStatus);
//            }
            projectTalent.setUpdateTime(new Date());
            projectTalentDao.save(projectTalent);
            if (status == 6) {
                // 若该项目入职，则改变其在其他项目进展状态
                List<ProjectTalent> list = projectTalentDao.findAllByTalentIdAndProjectIdNotAndStatusLessThan(projectTalent.getTalentId(), projectTalent.getProjectId(), 7);
                list.forEach(pt -> {
                    pt.setType(200);
                    pt.setStatus(8);
//                    projectTalent.setKillStatus(-1);
                    projectTalent.setKillRemark("该人才已在其他项目入职或进入保证期");
                    pt.setUpdateTime(new Date());
                    projectTalentDao.save(pt);
                });
            }
            Talent talent = talentDao.findById(projectTalent.getTalentId()).get();
            if (talent != null) {
                switch (status) {
                    case 1: talent.setStatus(10);break;
                    case 7: talent.setStatus(2);break;
//                    case 8: talent.setStatus(1);break;
                }
                talentDao.save(talent);
            }
        }
        return projectRemind;
    }

    // 项目总监-推荐给客户二次审核
    @Transactional
    public void reviewToCustomer(Long projectTalentId, Long projectRemindId, Boolean flag, Long userId) {
        ProjectTalent projectTalent = projectTalentDao.findById(projectTalentId).get();
        ProjectRemind followRemind = null;
        if (projectRemindId != null) {
            followRemind = projectRemindDao.findById(projectRemindId).get();
        }
        List<ProjectRemind> reminds = projectRemindDao.findAllByProjectTalentIdAndStatusAndType(projectTalentId, 0, 100);
        if (projectTalent != null && projectTalent.getType() == 100) {
            if (flag) { // 推荐成功
                if (followRemind != null) {
                    followRemind.setType(1);
                    followRemind.setStatus(1);
                    projectRemindDao.save(followRemind);
                } else {
                    reminds.forEach(remind -> {
                        remind.setType(101);
                    });
                    projectRemindDao.saveAll(reminds);
                }
            } else { // 淘汰
                if (followRemind != null) {
                    followRemind.setType(101); // 未推荐成功
                    projectRemindDao.save(followRemind);
                } else {
                    reminds.forEach(remind -> {
                        remind.setType(101);
                    });
                    projectRemindDao.saveAll(reminds);
                }
//                projectTalent.setKillStatus(-1);
                // 新增总监淘汰记录
                ProjectRemind remind = new ProjectRemind();
                remind.setType(15);
                remind.setStatus(8);
                remind.setCreateUserId(userId);
                remind.setCreateTime(new Date(System.currentTimeMillis()));
                remind.setProjectTalentId(projectTalentId);
                projectRemindDao.save(remind);
            }
            projectTalent.setType(flag ? 1 : 15);
            projectTalent.setStatus(flag ? 1 : 8);
            if (!flag) {
//                projectTalent.setKillStatus(-1);
                projectTalent.setKillRemark("推荐审核未通过");
            }
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
        Long followId = report.getFollowId();
        if (followId != null) {
            ProjectReport projectReport = projectReportDao.findById(followId).get();
            if (projectReport != null) {
                projectReport.setStatus(false);
                projectReportDao.save(projectReport);
            }
        }
        report.setCreateTime(new Date(System.currentTimeMillis()));
        projectReportDao.save(report);
        return report.getId();
    }

    // 修改项目状态
    public Boolean changeProjectStatus(Long id, Integer status) {
        Project project = projectDao.findById(id).get();
        if (project != null) {
            project.setStatus(status);
            projectDao.save(project);
            return true;
        } else {
            return false;
        }
    }

    // 项目进展回退
    public int reBack(Long projectTalentId, Integer status) {
        ProjectTalent projectTalent = projectTalentDao.findById(projectTalentId).get();
        if (projectTalent != null) {
            if (status == 1) {
                projectTalent.setStatus(0);
                projectTalent.setType(0);
                projectTalent.setUpdateTime(new Date());
                projectTalentDao.save(projectTalent);
                return 0;
            }
            List<ProjectRemind> reminds = projectRemindDao.findAllByProjectTalentIdOrderByCreateTimeDesc(projectTalentId);
            for (int i=0; i<reminds.size(); i++) {
                ProjectRemind remind = reminds.get(i);
                if (remind.getStatus() == status && remind.getStatus() != remind.getPrevStatus()) {
//                    if (remind.getStatus() == 8) {
//                        projectTalent.setKillStatus(1);
//                    }
                    if (remind.getPrevStatus() != null && remind.getPrevStatus() == 3) {
                        projectTalent.setRemarkStatus(null);
                    }
                    projectTalent.setStatus(remind.getPrevStatus());
                    projectTalent.setType(remind.getPrevType());
                    projectTalent.setUpdateTime(new Date());
                    projectTalentDao.save(projectTalent);
                    return remind.getPrevStatus();
                };
            }
        }
        return -1;
    }

}
