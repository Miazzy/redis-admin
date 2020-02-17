package com.xianxin.redis.admin.bean.po;

import com.xianxin.redis.admin.bean.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HookSimpleJobConfig extends BaseVo implements Serializable {

    /**
     * job key
     */
    private String key;

    /**
     * key_job 增加_job 后缀
     */
    private String keyJob;

    /**
     * key_expire 增加_expire 后缀
     */
    private String keyExpire;

    /**
     * key_start 增加_start 后缀
     */
    private String keyStart;

    /**
     * key_end 增加_end 后缀
     */
    private String keyEnd;

    private String startTime;

    private String endTime;

    /**
     * -1=不设置时间区间 1=yyyy-MM-dd HH:mm:ss 2=HH:mm:ss
     */
    private String dateType;

    /**
     * 1=执行一次，2=每天，3=工作日(不包含双休)
     */
    private String lifeCycle;

    /**
     * key 的内容
     */
    private String value;

    /**
     * 失效时间=key失效一次代表job执行一次
     */
    private int expire;

    /**
     * job 运行总次数
     */
    private int total = 1;

    /**
     * job 运行成功次数
     */
    private int successful = 0;

    /**
     * job 运行失败次数
     */
    private int failure = 0;

    /**
     * 备注
     */
    private String remark;

    /**
     * job 是否可以运行
     */
    private boolean available;

    /**
     * task=执行任务 、redis_notice=Redis发布
     */
    private String type = "task";

    public void setKey(String key) {
        this.key = key;
        this.keyJob = key + "_job";
        this.keyExpire = key + "_expire";
        this.keyStart = key + "_start";
        this.keyEnd = key + "_end";
    }
}
