package com.moving.admin.service;

import com.moving.admin.dao.natives.TaskNative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskService {

    @Autowired
    private TaskNative taskNative;

    // 每天凌晨执行人才类型定时任务
    @Scheduled(cron = "0 50 23 * * ?")
    public void checkTalentType() {
        taskNative.talentTaskWithoutProject();
        taskNative.talentTaskWithFinish();
        taskNative.projectTalentTask();
    }

}
