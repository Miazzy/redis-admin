package com.xianxin.redis.admin.bean.po;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HookExpireConfig implements Serializable {

    private String startTime;

    private int startExpire;

    private String endTime;

    private int endExpire;

    private String nextStartTime;

    private int nextStartExpire;

    private String nextEndTime;

    private int nextEndExpire;

    private HookSimpleJobConfig hookSimpleJobConfig;
}
