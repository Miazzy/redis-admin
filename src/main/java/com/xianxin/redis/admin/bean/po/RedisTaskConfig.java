package com.xianxin.redis.admin.bean.po;

import com.xianxin.redis.admin.bean.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 任务组管理类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedisTaskConfig extends BaseVo {

    /**
     * redis key
     */
    private String key;

    /**
     * 任务组名称
     */
    private String name;

    /**
     * 任务是否可用
     */
    private boolean available;

    /**
     * 任务集合
     */
    private List<TaskConfig> taskConfigs;
}


