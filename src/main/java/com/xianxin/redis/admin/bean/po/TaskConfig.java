package com.xianxin.redis.admin.bean.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TaskConfig implements Serializable {


    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务类型code
     */
    private String type;

    /**
     * 任务类型别名
     */
    private String typeAlias;


    /**
     * http 请求配置
     */
    private List<HttpRequestConfig> httpRequestConfigs;

    /**
     * 任务级别 按数字大小 从小到大依次执行
     */
    private int level;
}
