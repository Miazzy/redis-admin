package com.xianxin.redis.admin.service;

import com.xianxin.redis.admin.bean.vo.HookCommonVo;
import com.xianxin.redis.admin.bean.po.HookSimpleJobConfig;
import com.xianxin.redis.admin.framework.common.Response;

import java.text.ParseException;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
public interface HookService {

    Response psubscribe(HookCommonVo vo);

    void punsubscribe(HookCommonVo vo);

    Response pubsub(HookCommonVo vo);

    Response createHook(HookSimpleJobConfig config) throws ParseException;
}
