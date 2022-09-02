package com.cisdi.transaction.config.task;

import cn.hutool.core.date.DateUtil;
import com.cisdi.transaction.service.GbBasicInfoService;
import com.cisdi.transaction.service.OrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/8 0:20
 */
@Component
@EnableScheduling   // 1.开启定时任务
@EnableAsync        // 2.开启多线程
public class MultithreadScheduleTask {

    @Autowired
    private OrgService orgService;

    @Autowired
    private GbBasicInfoService gbBasicInfoService;

    @PostConstruct      //项目启动执行1次 全部查询
    public  void syncOrgInfoForStart(){
        long i = DateUtil.date().getTime();
        System.out.println("执行组织同步定时任务");
        String stringDate = DateUtil.format(new Date(), "yyyyMMdd");
        System.out.println("当前时间"+stringDate);
        orgService.syncDa(null);
        long j = DateUtil.date().getTime();
        System.out.println("执行组织同步定时任务完成:"+(j-i));
    }
    //@Scheduled(cron = "0 0 18 *  *  ? ") //每天晚上九点执行 增量查询
    public  void syncOrgInfo(){
        long i = DateUtil.date().getTime();
        System.out.println("执行组织同步定时任务");
        String stringDate = DateUtil.format(new Date(), "yyyyMMdd");
        System.out.println("当前时间"+stringDate);
        orgService.syncDa(stringDate);
        long j = DateUtil.date().getTime();
        System.out.println("执行组织同步定时任务完成:"+(j-i));
    }

   // @PostConstruct      //项目启动执行一次
    @Scheduled(fixedDelay = 7200000) //两小时执行一次
    //@Scheduled(cron = "0 0 07 *  *  ? ") //每天晚上九点执行
    public  void syncGbBasicInfo(){
        long i = DateUtil.date().getTime();
        System.out.println("执行干部信息定时任务");
        gbBasicInfoService.syncData();
        long j = DateUtil.date().getTime();
        System.out.println("执行干部信息定时任务完成+"+(j-i));
    }
}
