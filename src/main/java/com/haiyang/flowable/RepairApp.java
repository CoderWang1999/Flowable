package com.haiyang.flowable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RepairApp {
    /**
     *日志信息
     */
    private static final Logger log = LoggerFactory.getLogger(RepairApp.class);

    public static void main(String[] args) {
        log.info("项目开始启动");
        SpringApplication.run(RepairApp.class,args);
        log.info("项目启动完成");
    }
}