package com.xianxin.redis.admin.framework.listener;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileReader;
import com.xianxin.redis.admin.framework.config.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPubSub;

import java.io.File;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/01/29
 */
@Slf4j
@Component
public class LoadSysConfiguration implements ApplicationListener<ContextRefreshedEvent> {


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // 读取配置文件 加载到系统内存中
        log.info("开始读取配置文件");
        // 读取系统用户
        String userJson = "";
        try {
            File userFile = new File("../conf/user.json");
            FileReader userFileReader = new FileReader(userFile);
            userJson = userFileReader.readString();
            log.info("user.json：{}", userJson);
        } catch (IORuntimeException e) {
            log.error("加载user.json文件出错：", e);
        }
        // 加载 读取结果到内存
        SysConfig.loadUserInfoIntoMemory(userJson);

        // 读取系统redis配置
        String configJson = "";
        try {
            File configFile = new File("../conf/config.json");
            FileReader configFileReader = new FileReader(configFile);
            configJson = configFileReader.readString();
            log.info("config.json：{}", configJson);
        } catch (IORuntimeException e) {
            log.error("config.json文件出错：", e);
        }

        SysConfig.loadRedisConfigIntoMemory(configJson);
    }
}
