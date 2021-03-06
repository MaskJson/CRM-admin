package com.moving.admin.controller;

import com.moving.admin.bean.Result;
import com.moving.admin.dao.natives.CommonNative;
import com.moving.admin.dao.performance.*;
import com.moving.admin.dao.sys.ReportDao;
import com.moving.admin.entity.sys.Report;
import com.moving.admin.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(description = "公共数据")
@RestController
@RequestMapping("/performance")
public class PerformanceController extends AbstractController {

    @Autowired
    private PerformanceNative performanceNative;

    @Autowired
    private PerformanceTalentRemind performanceTalentRemind;

    @Autowired
    private PerformanceCustomerRemind performanceCustomerRemind;

    @Autowired
    private PerformanceReport performanceReport;

    @Autowired
    private PerformanceCount performanceCount;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private CommonNative commonNative;

    @ApiOperation("个人项目进展绩效")
    @GetMapping("/project/info")
    public Result<List<Map<String, Object>>> getPerformance(Long userId, Integer flag, String time) throws Exception {
        return ResultUtil.success(performanceNative.getPerformance(userId, flag, time));
    }

    @ApiOperation("上级获取项目进展报表")
    @GetMapping("/project/infos")
    public Result<List<Map<String, Object>>> getPerformanceReport(Long userId, Long roleId, Integer flag, String time, Long memberId) throws Exception {
        return ResultUtil.success(performanceNative.getPerformanceReport(userId, roleId, flag, time, memberId));
    }

    @ApiOperation("个人人才常规跟踪绩效")
    @GetMapping("/talent/info")
    public Result<List<Map<String, Object>>> getTalentRemindPerformance(Long userId, Integer flag, String time) throws Exception {
        return ResultUtil.success(performanceTalentRemind.getPerformance(userId, flag, time));
    }

    @ApiOperation("上级获取人才常规跟踪报表")
    @GetMapping("/talent/infos")
    public Result<List<Map<String, Object>>> getTalentRemindPerformanceReport(Long userId, Long roleId, Integer flag, String time, Long memberId) throws Exception {
        return ResultUtil.success(performanceTalentRemind.getPerformanceReport(userId, roleId, flag, time, memberId));
    }

    @ApiOperation("个人客户常规跟踪绩效")
    @GetMapping("/customer/info")
    public Result<List<Map<String, Object>>> getCustomerRemindPerformance(Long userId, Integer flag, String time) throws Exception {
        return ResultUtil.success(performanceCustomerRemind.getPerformance(userId, flag, time));
    }

    @ApiOperation("上级获取客户常规跟踪报表")
    @GetMapping("/customer/infos")
    public Result<List<Map<String, Object>>> getCustomerRemindPerformanceReport(Long userId, Long roleId, Integer flag, String time, Long memberId) throws Exception {
        return ResultUtil.success(performanceCustomerRemind.getPerformanceReport(userId, roleId, flag, time, memberId));
    }

    @ApiOperation("个人报告")
    @GetMapping("/report/info")
    public Result<List<Map<String, Object>>> getReportPerformance(Long userId, Integer flag, String time) throws Exception {
        return ResultUtil.success(performanceReport.getPerformance(userId, flag, time));
    }

    @ApiOperation("上级获取报告报表")
    @GetMapping("/report/infos")
    public Result<Map<String, Object>> getReportPerformanceReport(Long userId, Long roleId, Integer flag, String time, Long memberId) throws Exception {
        List<Map<String, Object>> reports = performanceReport.getPerformanceReport(userId, roleId, flag, time, memberId);
        List<Map<String, Object>> members = performanceReport.getMembers(userId, roleId, flag);
        Map<String, Object> map = new HashMap<>();
        map.put("reports", reports);
        map.put("members", members);
        return ResultUtil.success(map);
    }

    @ApiOperation("上级获取客户常规跟踪报表")
    @PostMapping("/report/save")
    public Result<Long> saveReport(@RequestBody Report report) throws Exception {
        report.setCreateTime(new Date());
        Report rp = reportDao.save(report);
        return ResultUtil.success(rp.getId());
    }

    @ApiOperation("获取成员")
    @GetMapping("/members")
    public Result<List<Map<String, Object>>> getMembers(Long userId, Long roleId, Integer flag) throws Exception {
        return ResultUtil.success(commonNative.getMembers(userId, roleId, flag));
    }

    @ApiOperation("今日面试通知")
    @GetMapping("/interview")
    public Result<List<Map<String, Object>>> getInterView(Long userId, Long roleId) throws Exception {
        return ResultUtil.success(performanceNative.getInterview(userId, roleId));
    }

    @ApiOperation("绩效页面统计")
    @GetMapping("/count")
    public Result<Map<String, Object>> count(Long userId, Long roleId, Long memberId) throws Exception {
        return ResultUtil.success(performanceCount.getPerformanceCount(userId, roleId, memberId));
    }

}
