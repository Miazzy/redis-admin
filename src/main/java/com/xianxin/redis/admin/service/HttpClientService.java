package com.xianxin.redis.admin.service;

import com.xianxin.redis.admin.bean.po.HttpRequestConfig;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
public interface HttpClientService {

    Object request(HttpRequestConfig config);
}
