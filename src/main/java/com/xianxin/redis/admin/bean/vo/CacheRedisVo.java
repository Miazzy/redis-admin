package com.xianxin.redis.admin.bean.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author 敖燕飞
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CacheRedisVo extends BaseVo implements Serializable {
    /**
     * 数据类型
     */
    private String dataType;
    /**
     * 原始的缓存key
     */
    private String oldRedisKey;
    
    /**
     * 缓存键
     */
    private String redisKey;

    /**
     * hash键
     */
    private String redisHKey;

    /**
     * 缓存值
     */
    private String redisValue;

    /**
     * 分值
     */
    private double score;

    /**
     * 过期时间
     */
    private int expire;
    /**
     * 时间单位
     */
    private String unit;
}
