package com.xianxin.redis.admin.controller;

import com.xianxin.redis.admin.bean.po.RedisTaskConfig;
import com.xianxin.redis.admin.bean.vo.RedisTaskExecuteVo;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.service.RedisTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis/task")
@Slf4j
public class RedisTaskController {

    @Autowired
    private RedisTaskService redisTaskService;

    @PostMapping(path = "/create")
    public Response create(@RequestBody RedisTaskConfig config){


        return redisTaskService.create(config);
    }

    @PostMapping(path = "/execute")
    public Response execute(@RequestBody RedisTaskExecuteVo vo){


        return redisTaskService.execute(vo);
    }

}
