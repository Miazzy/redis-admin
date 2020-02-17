package com.xianxin.redis.admin.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.xianxin.redis.admin.bean.vo.HookCommonVo;
import com.xianxin.redis.admin.bean.po.HookExpireConfig;
import com.xianxin.redis.admin.bean.po.HookSimpleJobConfig;
import com.xianxin.redis.admin.bean.po.RedisConfig;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.framework.config.SysConfig;
import com.xianxin.redis.admin.framework.listener.KeyExpiredListener;
import com.xianxin.redis.admin.framework.utils.DateUtils;
import com.xianxin.redis.admin.service.HookService;
import com.xianxin.redis.admin.service.RedisTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Service
@Slf4j
public class HookServiceImpl implements HookService {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat sdfYmd = new SimpleDateFormat("yyy-MM-dd");

    private static SimpleDateFormat sdfHms = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private KeyExpiredListener listener;

    @Autowired
    private RedisTaskService redisTaskService;

    @Override
    public Response psubscribe(HookCommonVo vo) {

        RedisConfig redisConfig = SysConfig.findRedisConfigByUsername(vo.getHost());

        JedisPool pool = new JedisPool(new JedisPoolConfig(), redisConfig.getHost(), redisConfig.getPort(), 60000, redisConfig.getPassword(), Integer.parseInt(vo.getDb()));

        Jedis jedis = pool.getResource();
        config(jedis);

        listener = new KeyExpiredListener(pool);
        listener.setRedisTaskService(redisTaskService);

        String[] a = new String[vo.getPatterns().size()];
        String[] b = vo.getPatterns().toArray(a);


        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                jedis.psubscribe(listener, b);
            }
        });

        return Response.success("订阅启用成功");
    }

    @Override
    public void punsubscribe(HookCommonVo vo) {
        String[] a = new String[vo.getPatterns().size()];
        String[] b = vo.getPatterns().toArray(a);

        listener.punsubscribe(b);
//        return Response.success("订阅取消成功");
    }

    @Override
    public Response pubsub(HookCommonVo vo) {

        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());

        String[] a = new String[vo.getPatterns().size()];
        String[] channels = vo.getPatterns().toArray(a);

        Map<String, String> data = jedis.pubsubNumSub(channels);

        return Response.success(data);
    }

    @Override
    public Response createHook(HookSimpleJobConfig hookSimpleJobConfig) throws ParseException {

        RedisConfig redisConfig = SysConfig.findRedisConfigByUsername(hookSimpleJobConfig.getHost());
        int db = Integer.parseInt(hookSimpleJobConfig.getDb());

        JedisPool pool = new JedisPool(new JedisPoolConfig(), redisConfig.getHost(), redisConfig.getPort(), 60000, redisConfig.getPassword(), db);
        Jedis jedis = pool.getResource();

        HookExpireConfig hookExpireConfig = getExpireConfig(hookSimpleJobConfig);

        log.info("hookExpireConfig={}", JSON.toJSONString(hookExpireConfig));

        if (hookSimpleJobConfig.getDateType().equals("-1")) {

        } else {
            // 主要
            jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookExpireConfig));

            jedis.set(hookSimpleJobConfig.getKeyStart(), JSON.toJSONString(hookExpireConfig));
            jedis.expire(hookSimpleJobConfig.getKeyStart(), hookExpireConfig.getStartExpire());

            jedis.set(hookSimpleJobConfig.getKeyEnd(), JSON.toJSONString(hookExpireConfig));
            jedis.expire(hookSimpleJobConfig.getKeyEnd(), hookExpireConfig.getEndExpire());
        }

        return Response.success("添加成功");
    }

    private static HookExpireConfig getExpireConfig(HookSimpleJobConfig hookSimpleJobConfig) throws ParseException {
        HookExpireConfig hookExpireConfig = new HookExpireConfig();

        boolean isAdd = false;
        Date currDate = new Date();

        // 计算当前 任务名_start key 的存活时间
        String currDateYmd = sdfYmd.format(currDate);

        String startDateStr = "";

        if (hookSimpleJobConfig.getDateType().equals("1")) {
            startDateStr = hookSimpleJobConfig.getStartTime();
        } else if (hookSimpleJobConfig.getDateType().equals("2")) {
            // HH:mm:ss
            startDateStr = currDateYmd + " " + hookSimpleJobConfig.getStartTime();
        }

        log.info("拼接后的开始时间字符串：{}", startDateStr);
        // 开始时间
        Date startDate = sdf.parse(startDateStr);
        long startSeconds = DateUtils.calcSeconds(currDate, startDate);

        log.info("任务名_start key有效存活时间：{}(s)", startSeconds);

        if (startSeconds < 0) {
            // 小于0 日期往前加1天 重新计算失效时间
            log.info("小于0 日期往前加1天 重新计算失效时间");

            startDate = DateUtils.addDays(startDate, 1);
            startDateStr = sdf.format(startDate);
            log.info("加1天后的时间字符串：{}", startDateStr);

            startSeconds = DateUtils.calcSeconds(currDate, startDate);
            log.info("重新计算后 - 任务名_start key有效存活时间：{}(s)", startSeconds);

            // 如果开始时间 +1 了
            isAdd = true;
        }

        hookExpireConfig.setStartTime(startDateStr);
        hookExpireConfig.setStartExpire(Integer.parseInt(startSeconds + ""));

        // 计算当前 任务名_end key 的存活时间
        String endDateYmd = sdfYmd.format(startDate);
        String endDateStr = "";

        if (isAdd) {
            Date endTime = sdf.parse(hookSimpleJobConfig.getEndTime());
            String endDateHms = sdfHms.format(endTime);
            endDateStr = endDateYmd + " " + endDateHms;
        } else {
            if (hookSimpleJobConfig.getDateType().equals("1")) {
                endDateStr = hookSimpleJobConfig.getEndTime();
            } else if (hookSimpleJobConfig.getDateType().equals("2")) {
                // HH:mm:ss
                endDateStr = endDateYmd + " " + hookSimpleJobConfig.getEndTime();
            }
        }

        log.info("拼接后的结束时间字符串：{}", endDateStr);
        Date endDate = sdf.parse(endDateStr);
        long endSeconds = 0;
        if (isAdd) {
            endSeconds = DateUtils.calcSeconds(startDate, endDate) + startSeconds;
        } else {
            endSeconds = DateUtils.calcSeconds(currDate, endDate);
        }

        log.info("任务名_end key有效存活时间：{}(s)", endSeconds);

        hookExpireConfig.setEndTime(endDateStr);
        hookExpireConfig.setEndExpire(Integer.parseInt(endSeconds + ""));


        // 计算下一次 任务名_start key 的存活时间
        Date nextStartDate = DateUtils.addDays(startDate, 1);
        String nextStartDateStr = sdf.format(nextStartDate);
        log.info("下一次 任务名_start key 的日期字符串：{}", nextStartDateStr);
        long nextStartSeconds = DateUtils.calcSeconds(endDate, nextStartDate);
        log.info("下一次 任务名_start key 的有效存活时间：{}", nextStartSeconds);

        hookExpireConfig.setNextStartTime(nextStartDateStr);
        hookExpireConfig.setNextStartExpire(Integer.parseInt(nextStartSeconds + ""));

        // 计算下一次 任务名_end key 的存活时间
        Date nextEndDate = DateUtils.addDays(endDate, 1);
        String nextEndDateStr = sdf.format(nextEndDate);
        log.info("下一次 任务名_end key 的日期字符串：{}", nextEndDateStr);
        long nextEndSeconds = DateUtils.calcSeconds(nextStartDate, nextEndDate);
        int nextEndExpire = Integer.parseInt(nextEndSeconds + "") + Integer.parseInt(nextStartSeconds + "");
        log.info("下一次 任务名_end key 的有效存活时间：{}", nextEndExpire);

        hookExpireConfig.setNextEndTime(nextEndDateStr);
        hookExpireConfig.setNextEndExpire(nextEndExpire);

        hookExpireConfig.setHookSimpleJobConfig(hookSimpleJobConfig);

        return hookExpireConfig;
    }

    private static void config(Jedis jedis) {
        String parameter = "notify-keyspace-events";
        List<String> notify = jedis.configGet(parameter);

        if (notify.get(1).equals("")) {
            jedis.configSet(parameter, "KEA");
        }
    }

}
