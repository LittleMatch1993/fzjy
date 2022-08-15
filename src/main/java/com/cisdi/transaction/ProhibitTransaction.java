package com.cisdi.transaction;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 14:02
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.cisdi.transaction.**"})
//@MapperScan(basePackages = "com.cisdi.transaction.mapper.master.**")
public class ProhibitTransaction implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(ProhibitTransaction.class,args);
    }

    @Override
    public void run(String... args) throws Exception {
     log.info("服务器启动成功");
    }
}
