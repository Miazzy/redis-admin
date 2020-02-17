package com.xianxin.redis.admin.service;

import com.xianxin.redis.admin.bean.po.RedisTaskConfig;
import com.xianxin.redis.admin.bean.vo.RedisTaskExecuteVo;
import com.xianxin.redis.admin.framework.common.Response;

public interface RedisTaskService {

    Response create(RedisTaskConfig config);

    Response execute(RedisTaskExecuteVo vo);
}
