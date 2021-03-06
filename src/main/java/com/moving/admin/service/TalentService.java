package com.moving.admin.service;

import com.moving.admin.dao.customer.CustomerDao;
import com.moving.admin.dao.natives.CountNative;
import com.moving.admin.dao.natives.TalentNative;
import com.moving.admin.dao.project.ProjectTalentDao;
import com.moving.admin.dao.sys.UserDao;
import com.moving.admin.entity.customer.Customer;
import com.moving.admin.dao.customer.DepartmentDao;
import com.moving.admin.dao.folder.FolderItemDao;
import com.moving.admin.dao.talent.*;
import com.moving.admin.entity.customer.Department;
import com.moving.admin.entity.folder.FolderItem;
import com.moving.admin.entity.project.ProjectTalent;
import com.moving.admin.entity.sys.User;
import com.moving.admin.entity.talent.*;
import com.moving.admin.exception.WebException;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sun.rmi.runtime.Log;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TalentService extends AbstractService {

    @Autowired
    private TalentDao talentDao;

    @Autowired
    CustomerDao customerDao;

    @Autowired
    private TalentRemindDao talentRemindDao;

    @Autowired
    private FriendDao friendDao;

    @Autowired
    private ChanceDao chanceDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private FolderItemDao folderItemDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CommonService commonService;

    @Autowired
    private ProjectTalentDao projectTalentDao;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CountNative countNative;

    @Autowired
    private TalentNative talentNative;

    // 手机号验证重复
    public Talent checkPhone(String phone) {
        Talent talent = null;
        talent = talentDao.findTalentByPhone(phone);
        return talent;
    }

    // 分页查询
    public Page<Talent> getCustomerList(String city, String name, String industry, String aptness, Long folderId, String customerName, String phone, Pageable pageable) {
        Page<Talent> result = talentDao.findAll((root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            if (!StringUtils.isEmpty(city)) {
                list.add(cb.equal(root.get("city"), city));
            }
            if (!StringUtils.isEmpty(aptness)) {
                list.add(cb.like(root.get("aptness"), "%" + aptness + "%"));
            }
            if (!StringUtils.isEmpty(name)) {
                list.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (!StringUtils.isEmpty(phone)) {
                list.add(cb.like(root.get("phone"), "%" + phone + "%"));
            }
            if (!StringUtils.isEmpty(industry)) {
                list.add(cb.like(root.get("industry"), "%" + industry + "%"));
            }
            List<Long> ids = new ArrayList<>();
            if (folderId != null) {
                List<FolderItem> folderItems = folderItemDao.findAllByFolderIdAndAndType(folderId, 2);
                folderItems.forEach(folderItem -> {
                    ids.add(folderItem.getItemId());
                });
            }
            if (!StringUtils.isEmpty(customerName)) {
                // 查询项目经历中有这些公司的人才
                ids.addAll(talentNative.getTalentIds(customerName));
            }
            if (folderId != null || !StringUtils.isEmpty(customerName)) {
                Expression<Long> exp = root.<Long>get("id");
                list.add(cb.and(exp.in(ids)));
            }
            Predicate[] predicates = new Predicate[list.size()];
            return query.where(list.toArray(predicates)).getRestriction();
        }, pageable);
        Long userId = super.getCurrentUserId();
        result.forEach(talent -> {
            Map<String, Object> map = countNative.getWorkInfo(talent.getId());
            talent.setPosition(map.get("position") != null ? map.get("position").toString() : "");
            talent.setProjects(projectTalentDao.findProjectIdsOfTalent(talent.getId()));
            talent.setOfferCount(projectTalentDao.getProjectOfferLength(talent.getId()));
            talent.setProgress(projectTalentDao.getProjectLengthByTalentId(talent.getId()));
            // 获取最后一次未跟进的常规跟踪
            List<TalentRemind> reminds = talentRemindDao.findByTalentIdAndFinishAndCreateUserIdOrderByCreateTimeDesc(talent.getId(), false, userId);
            talent.setFollowRemind(reminds != null && reminds.size() > 0 ? reminds.get(0) : null);
        });
        return result;
    }

    // 添加人才
    @Transactional
    public Long save(Talent talent) {
        if (talent.getId() != null) {
            if (talent.getFollowUserId() != null && talent.getFollowUserId() != talent.getActionUserId()) {
                throw new WebException(400, "该人才已被其他用户列为专属人才，您无权限编辑", null);
            }
        }
        Long createUserId = talent.getCreateUserId();
        Long roleId = talent.getRoleId();
        Long projectId = talent.getProjectId();
        String recommendation = talent.getRecommendation();
        TalentRemind remind = talent.getRemind();
        Date createTime = new Date();
        List<Experience> experiences = talent.getExperienceList();
        List<Friend> friends = talent.getFriends();
        List<Chance> chances = talent.getChances();
        talent.setUpdateTime(createTime);
        talentDao.save(talent);
        Long id = talent.getId();
        experienceDao.removeAllByTalentId(id);
        experiences.forEach(item -> {
            Long customerId = addCustomerFromTalentInfo(item.getCompany(), createUserId, roleId);
            Long departmentId = commonService.addDepartmentFromTalentInfo(customerId, item.getDepartment());
            Experience experience = experienceDao.findExperienceByCustomerIdAndDepartmentIdAndTalentId(item.getCustomerId(), item.getDepartmentId(), id);
//            if (experience == null) {
            if (true) {
                item.setTalentId(id);
                item.setCustomerId(customerId);
                item.setDepartmentId(departmentId);
                if (item.getStatus()) {
                    item.setEndTime(new Date(System.currentTimeMillis()));
                }
                experienceDao.save(item);
            }
        });
        friendDao.removeAllByTalentId(id);
        friends.forEach(friend -> {
            Long customerId = addCustomerFromTalentInfo(friend.getCompany(), createUserId, roleId);
            Long departmentId = commonService.addDepartmentFromTalentInfo(customerId, friend.getDepartment());
            friend.setTalentId(id);
            friend.setCustomerId(customerId);
            friend.setDepartmentId(departmentId);
            friendDao.save(friend);
        });
        chanceDao.removeAllByTalentId(id);
        chances.forEach(chance -> {
            Long customerId = addCustomerFromTalentInfo(chance.getCompany(), createUserId, roleId);
            chance.setTalentId(id);
            chance.setCustomerId(customerId);
            chanceDao.save(chance);
        });
        if (remind != null) {
            remind.setTalentId(id);
            remind.setFinish(remind.getNextType() == null);
            talentRemindDao.save(remind);
        }
        if (projectId != null) {
            ProjectTalent projectTalent = new ProjectTalent();
            if (roleId == 3) {
                projectTalent.setStatus(1);
                projectTalent.setType(1);
            } else {
                projectTalent.setStatus(0);
                projectTalent.setType(100);
            }
//            projectTalent.setRecommendation(recommendation);
            projectTalent.setProjectId(projectId);
            projectTalent.setTalentId(id);
            projectTalent.setCreateUserId(createUserId);
            projectTalent.setCreateTime(createTime);
            projectTalent.setRemark(recommendation);
            projectTalent.setRecommendation(recommendation);
            projectTalent.setRoleId(roleId);
            projectService.saveProjectTalent(projectTalent);
//            projectTalentDao.save(projectTalent);
        }
        return id;
    }

    // 添加客户，去重
    public Long addCustomerFromTalentInfo(String name, Long userId, Long roleId) {
        List<Customer> customers = customerDao.findAllByName(name);
        if (customers.size() > 0) {
            return customers.get(0).getId();
        } else {
            Customer customer = new Customer();
            customer.setAuditType(roleId == 3 ? 2 : 0);
            customer.setName(name);
            customer.setCreateUserId(userId);
            customer.setIndustry(null);
            customer.setType(0);
            customerDao.save(customer);
            return customer.getId();
        }
    }

    // 获取人才详情
    public Talent getTalentById(Long id) {
        Talent talent = talentDao.findById(id).get();
        Long userId = talent.getCreateUserId();
        if (userId != null) {
            talent.setCreateUser(userDao.findById(userId).get().getNickName());
        }
        if (talent != null) {
            List<Experience> experienceList = experienceDao.findAllByTalentId(id);
            experienceList.forEach(experience -> {
                if (experience.getCustomerId() != null)
                    experience.setCompany(customerDao.findById(experience.getCustomerId()).get().getName());
                if (experience.getDepartmentId() != null)
                    experience.setDepartment(departmentDao.findById(experience.getDepartmentId()).get().getName());
            });
            talent.setExperienceList(experienceList);
            List<Friend> friendList = friendDao.findAllByTalentId(id);
            friendList.forEach(friend -> {
                if (friend.getCustomerId() != null) {
                    friend.setCompany(customerDao.findById(friend.getCustomerId()).get().getName());
                }
                if (friend.getDepartmentId() != null) {
                    friend.setDepartment(departmentDao.findById(friend.getDepartmentId()).get().getName());
                }
            });
            talent.setFriends(friendList);
            List<Chance> chanceList = chanceDao.findAllByTalentId(id);
            chanceList.forEach(chance -> {
                if (chance.getCustomerId() != null)
                    chance.setCompany(customerDao.findById(chance.getCustomerId()).get().getName());
            });
            talent.setChances(chanceList);
            talent.setProjectCount(projectTalentDao.getProjectLengthByTalentId(id));
            talent.setOfferCount(projectTalentDao.getProjectOfferLength(id));
            talent.setProjects(projectTalentDao.findProjectIdsOfTalent(id));
            talent.setProgress(projectTalentDao.getProjectLengthByTalentId(id));
            // 获取最后一次未跟进的常规跟踪
            List<TalentRemind> reminds = talentRemindDao.findByTalentIdAndFinishAndCreateUserIdOrderByCreateTimeDesc(talent.getId(), false, super.getCurrentUserId());
            talent.setFollowRemind(reminds != null && reminds.size() > 0 ? reminds.get(0) : null);
        }
        return talent;
    }

    // 获取人才跟踪记录
    public List<TalentRemind> getAllRemind(Long id) {
        List<TalentRemind> list = talentRemindDao.findAllByTalentIdOrderByIdDesc(id);
        list.forEach(talentRemind -> {
            Long userId = talentRemind.getCreateUserId();
            if (userId != null) {
                User user = userDao.findById(userId).get();
                talentRemind.setCreateUser(user != null ? user.getNickName() : "");
            }
        });
        return list;
    }

    // 关注装修改
    public void toggleFollow(Long id, Boolean follow) {
        Talent talent = talentDao.findById(id).get();
        if (talent != null) {
            talent.setFollow(follow);
            talentDao.save(talent);
        }
    }

    // 添加人才跟踪
    @Transactional
    public Long saveRemind(TalentRemind remind) {
        remind.setCreateTime(new Date(System.currentTimeMillis()));
        remind.setUpdateTime(new Date(System.currentTimeMillis()));
        if (remind.getNextType() == null) {
            remind.setFinish(true);
        }
        if (remind.getProjectId() != null) {
            ProjectTalent projectTalent = new ProjectTalent();
            if (remind.getRoleId() == 3) {
                projectTalent.setStatus(1);
                projectTalent.setType(1);
            } else {
                projectTalent.setStatus(0);
                projectTalent.setType(100);
            }
            projectTalent.setRecommendation(remind.getRemark() != null ? remind.getRemark() : remind.getSituation());
            projectTalent.setProjectId(remind.getProjectId());
            projectTalent.setTalentId(remind.getTalentId());
            projectTalent.setCreateUserId(remind.getCreateUserId());
            projectTalent.setCreateTime(remind.getCreateTime());
            projectTalentDao.save(projectTalent);
        }
        // 淘汰跟进
        if (remind.getProjectTalentId() != null) {
            ProjectTalent projectTalent = projectTalentDao.findById(remind.getProjectTalentId()).get();
            if (projectTalent != null) {
//                projectTalent.setKillStatus(1);
                projectTalent.setUpdateTime(new Date());
                projectTalentDao.save(projectTalent);
            }
        }
        talentRemindDao.save(remind);
        Long followId = remind.getFollowRemindId();
        if (followId != null) {
            List<Long> ids = new ArrayList<>();
            ids.add(followId);
            finishRemindByIds(ids);
        }
        Talent talent = talentDao.findById(remind.getTalentId()).get();
        if (talent != null) {
            talent.setStatus(remind.getStatus());
            talentDao.save(talent);
        }
        return remind.getId();
    }

    // 客户跟踪跟进结束
    public void finishRemindByIds(List<Long> ids) {
        ids.forEach(id -> {
            TalentRemind talentRemind = talentRemindDao.findById(id).get();
            talentRemind.setFinish(true);
            talentRemindDao.save(talentRemind);
        });
    }

    // 修改人才类型，flag=true：设为专属，false：取消专属
    @Transactional
    public void toggleType(Long id, Long userId, Boolean flag) {
        Talent talent = talentDao.findById(id).get();
        if (talent != null) {
            if (flag) {
                if (talent.getFollowUserId() != null) {
                    throw new WebException(400, "该人才已被列为专属人才，请刷新", null);
                }
                List<Integer> status = new ArrayList<Integer>();
                status.add(7);
                status.add(8);
                List<ProjectTalent> projectTalents = projectTalentDao.findAllByTalentIdAndCreateUserIdNotAndStatusNotIn(id, userId, status);
                if (projectTalents.size() > 0) {
                    throw new WebException(400, "该人才已被其他用户列为项目进展人才，不满足设定条件", null);
                }
                List<TalentRemind> reminds = talentRemindDao.findAllByTalentIdAndCreateUserIdOrderByIdDesc(id, userId);
                if (reminds.size() < 3) {
                    throw new WebException(400, "您需要对该人才进行至少3次常规跟踪，不满足设定条件", null);
                }
                TalentRemind remind = reminds.get(0);
                if ((remind.getCreateTime().getTime() + 2592000000L) < System.currentTimeMillis()) {
                    throw new WebException(400, "您近一个月内未对该人才进行常规跟踪，不满足设定条件", null);
                }
                if (talent.getSex() == null || StringUtils.isEmpty(talent.getPhone())) {
                    throw new WebException(400, "专属人才性别、手机、工作经历等信息必须填写完整", "info");
                }
                List<Experience> experiences = experienceDao.findAllByTalentId(id);
                experiences.forEach(item -> {
                    if (StringUtils.isEmpty(item.getRemark()) || StringUtils.isEmpty(item.getPerformance())) {
                        throw new WebException(400, "专属人才工作经历的工作职责和业绩必须填写完整", "info");
                    }
                });
                talent.setFollowUserId(userId);
                talent.setType(1);
                talentDao.save(talent);
            } else {
                if (talent.getFollowUserId() != userId) {
                    throw new WebException(400, "您不是该人才的专属用户", null);
                }
                talent.setFollowUserId(null);
                talent.setType(0);
                talentDao.save(talent);
            }
        } else {
            throw new WebException(400, "该人才不存在，请刷新", null);
        }
    }

    public void setResume(Long talentId, String path, Boolean flag) {
        Talent talent = talentDao.findById(talentId).get();
        if (talent != null) {
            if (flag) {
                talent.setResume(path);
            } else {
                talent.setResume2(path);
            }
            talentDao.save(talent);
        }
    }
}
