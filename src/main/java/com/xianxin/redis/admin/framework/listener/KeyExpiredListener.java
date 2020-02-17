package com.xianxin.redis.admin.framework.listener;

import com.alibaba.fastjson.JSON;
import com.xianxin.redis.admin.bean.po.HookExpireConfig;
import com.xianxin.redis.admin.bean.po.HookSimpleJobConfig;
import com.xianxin.redis.admin.bean.vo.RedisTaskExecuteVo;
import com.xianxin.redis.admin.framework.utils.DateUtils;
import com.xianxin.redis.admin.service.RedisTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Slf4j
@Component
public class KeyExpiredListener extends JedisPubSub {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyy-MM-dd");

    private static SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

    private Jedis jedis;

    public Jedis getJedis() {
        return jedis;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    public KeyExpiredListener() {
        super();
    }

    private RedisTaskService redisTaskService;

    public RedisTaskService getRedisTaskService() {
        return redisTaskService;
    }

    public void setRedisTaskService(RedisTaskService redisTaskService) {
        this.redisTaskService = redisTaskService;
    }

    public KeyExpiredListener(JedisPool pool) {
        super();
        this.setJedis(pool.getResource());
    }

    @Override
    public void onMessage(String channel, String message) {
        log.info("onMessage - channel:{} - message:{}", channel, message);
        super.onMessage(channel, message);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        // log.info("onPMessage - pattern:{} - channel:{} - message:{}", pattern, channel, message);

        if (channel.equalsIgnoreCase("__keyevent@0__:expired")) {

            log.info("任务 {} 已失效，触发Hook", message);

            if (message.contains("_start")) {

                String message1 = message.replace("_start", "");
                log.info("截取后的message：{}", message1);

                String text = jedis.get(message1 + "_job");
                HookExpireConfig hookExpireConfig = JSON.parseObject(text, HookExpireConfig.class);

                HookSimpleJobConfig hookSimpleJobConfig = hookExpireConfig.getHookSimpleJobConfig();

                hookSimpleJobConfig.setAvailable(true);

                // 设置游戏规则
                hookExpireConfig.setHookSimpleJobConfig(hookSimpleJobConfig);

                // set 任务key
                jedis.set(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getValue());
                jedis.expire(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getExpire());

                // set 任务_expire key
                jedis.set(hookSimpleJobConfig.getKeyExpire(), hookSimpleJobConfig.getExpire() + "");
                // set 任务—_job key
                jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookExpireConfig));

                log.info("任务已启动，{}(s)后开始运行", hookSimpleJobConfig.getExpire());

            } else if (message.contains("_end")) {

                String message1 = message.replace("_end", "");
                log.info("截取后的message：{}", message1);

                String key = message1 + "_job";
                String text = jedis.get(key);
                HookExpireConfig hookExpireConfig = JSON.parseObject(text, HookExpireConfig.class);

                HookSimpleJobConfig hookSimpleJobConfig = hookExpireConfig.getHookSimpleJobConfig();

                hookSimpleJobConfig.setAvailable(false);

                // job生命周期 1=执行一次，2=每天，3=工作日(不包含双休)
                String lifeCycle = hookSimpleJobConfig.getLifeCycle();
                if (lifeCycle.equals("1")) {
                    // 执行一次
                    log.info("job生命周期：执行一次");
                } else if (lifeCycle.equals("2")) {
                    // 每天
                    log.info("job生命周期：每天");
                    try {
                        // 计算下一次 开始时间和结束时间
                        hookExpireConfig = getNextExpireConfig(hookExpireConfig);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } else if (lifeCycle.equals("3")) {
                    // 工作日(不包含双休)
                }

                hookExpireConfig.setHookSimpleJobConfig(hookSimpleJobConfig);

                if (hookSimpleJobConfig.getDateType().equals("-1")) {

                } else {

                    // 主要 set 任务—_job key
                    jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookExpireConfig));

                    jedis.set(hookSimpleJobConfig.getKeyStart(), JSON.toJSONString(hookExpireConfig));
                    jedis.expire(hookSimpleJobConfig.getKeyStart(), hookExpireConfig.getStartExpire());

                    jedis.set(hookSimpleJobConfig.getKeyEnd(), JSON.toJSONString(hookExpireConfig));
                    jedis.expire(hookSimpleJobConfig.getKeyEnd(), hookExpireConfig.getEndExpire());
                }
            } else {
                // 执行job 任务
                String key = message + "_job";
                String text = jedis.get(key);

                HookExpireConfig hookExpireConfig = JSON.parseObject(text, HookExpireConfig.class);
                HookSimpleJobConfig hookSimpleJobConfig = hookExpireConfig.getHookSimpleJobConfig();

                log.info("任务内容：{}", hookSimpleJobConfig.getValue());

                try {
                    if (StringUtils.isNotBlank(hookSimpleJobConfig.getType())) {
                        String type = hookSimpleJobConfig.getType();
                        if ("task".equals(type)) {

                            RedisTaskExecuteVo executeVo = JSON.parseObject(hookSimpleJobConfig.getValue(), RedisTaskExecuteVo.class);

                            redisTaskService.execute(executeVo);
                        }
                    }
                } catch (Exception e) {
                    log.info("执行任务出现异常：", e);
                }


                if (hookSimpleJobConfig.isAvailable()) {
                    // set 任务key
                    jedis.set(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getValue());
                    jedis.expire(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getExpire());
                } else {

                    // 删除
                    jedis.del(hookSimpleJobConfig.getKey());
                    log.info("任务已停止");
                }
            }


//            String text = jedis.get(message + "_job");
//
//            HookSimpleJobConfig hookSimpleJobConfig = JSON.parseObject(text, HookSimpleJobConfig.class);
//
//            log.info("任务内容：{}", hookSimpleJobConfig.getValue());
//
//            log.info("任务备注：{}", hookSimpleJobConfig.getRemark());
//
//            if (hookSimpleJobConfig.getTotal() <= -1) {
//
//                if(hookSimpleJobConfig.isAvailable()){
//
//                    jedis.set(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getValue());
//
//                    jedis.expire(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getExpire());
//                }
//
//            } else if (hookSimpleJobConfig.getTotal() >= 1) {
//
//                // 增加执行次数
//                int successful = hookSimpleJobConfig.getSuccessful() + 1;
//                boolean available = successful <= hookSimpleJobConfig.getTotal();
//                log.info("总次数：{}，计数：{}，是否继续执行：{}", hookSimpleJobConfig.getTotal(), successful, available);
//
//                if (available) {
//
//                    hookSimpleJobConfig.setAvailable(available);
//
//                    if (successful <= hookSimpleJobConfig.getTotal()) {
//                        hookSimpleJobConfig.setSuccessful(successful);
//
//                        jedis.set(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getValue());
//                        jedis.expire(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getExpire());
//                    }
//
//                } else {
//                    log.info("{} 任务终止。", hookSimpleJobConfig.getKey());
//                }
//
//            }
//
//            jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookSimpleJobConfig));

        }

        super.onPMessage(pattern, channel, message);

    }

    private HookExpireConfig getNextExpireConfig(HookExpireConfig hookExpireConfig) throws ParseException {
        Date endTime = sdf.parse(hookExpireConfig.getEndTime());
        Date nextStartTime = sdf.parse(hookExpireConfig.getNextStartTime());
        Date nextEndTime = sdf.parse(hookExpireConfig.getNextEndTime());

        long startSeconds = DateUtils.calcSeconds(endTime, nextStartTime);
        long endSeconds = DateUtils.calcSeconds(endTime, nextEndTime);

        int startExpire = Integer.parseInt(startSeconds + "");
        int endExpire = Integer.parseInt(endSeconds + "");

        hookExpireConfig.setStartExpire(startExpire);
        hookExpireConfig.setEndExpire(endExpire);
        hookExpireConfig.setStartTime(hookExpireConfig.getNextStartTime());
        hookExpireConfig.setEndTime(hookExpireConfig.getNextEndTime());

        // 加1天
        Date nextStartTime1 = DateUtils.addDays(nextStartTime, 1);
        Date nextEndTime1 = DateUtils.addDays(nextEndTime, 1);

        long nextStartSeconds = DateUtils.calcSeconds(nextEndTime, nextStartTime1);
        long nextEndSeconds = DateUtils.calcSeconds(nextEndTime, nextEndTime1);

        int nextStartExpire = Integer.parseInt(nextStartSeconds + "");
        int nextEndExpire = Integer.parseInt(nextEndSeconds + "");

        hookExpireConfig.setNextStartExpire(nextStartExpire);
        hookExpireConfig.setNextEndExpire(nextEndExpire);

        String nextStartTimeStr = sdf.format(nextStartTime1);
        hookExpireConfig.setNextStartTime(nextStartTimeStr);

        String nextEndTimeStr = sdf.format(nextEndTime1);
        hookExpireConfig.setNextEndTime(nextEndTimeStr);

        return hookExpireConfig;
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        log.info("onSubscribe - channel:{} - subscribedChannels:{}", channel, subscribedChannels);
        super.onSubscribe(channel, subscribedChannels);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        log.info("onUnsubscribe - channel:{} - subscribedChannels:{}", channel, subscribedChannels);
        super.onUnsubscribe(channel, subscribedChannels);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        log.info("onPUnsubscribe - pattern:{} - subscribedChannels:{}", pattern, subscribedChannels);
        super.onPUnsubscribe(pattern, subscribedChannels);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        log.info("onPSubscribe - pattern:{} - subscribedChannels:{}", pattern, subscribedChannels);
        super.onPSubscribe(pattern, subscribedChannels);
    }

    @Override
    public void onPong(String pattern) {
        log.info("onPong - pattern:{}", pattern);
        super.onPong(pattern);
    }

    /**
     * 取消所有订阅
     */
    @Override
    public void unsubscribe() {
        super.unsubscribe();
    }

    /**
     * 取消订阅频道
     *
     * @param channels
     */
    @Override
    public void unsubscribe(String... channels) {
        super.unsubscribe(channels);
    }

    /**
     * 增加订阅频道
     *
     * @param channels
     */
    @Override
    public void subscribe(String... channels) {
        super.subscribe(channels);
    }

    /**
     * 增加订阅频道的匹配表达式
     *
     * @param patterns
     */
    @Override
    public void psubscribe(String... patterns) {
        super.psubscribe(patterns);
    }

    /**
     * 取消所有按表达式的订阅
     */
    @Override
    public void punsubscribe() {
        super.punsubscribe();
    }

    /**
     * 取消表达式匹配的频道
     *
     * @param patterns
     */
    @Override
    public void punsubscribe(String... patterns) {
        super.punsubscribe(patterns);
    }

    @Override
    public void ping() {
        super.ping();
    }

    @Override
    public boolean isSubscribed() {
        return super.isSubscribed();
    }

    @Override
    public void proceedWithPatterns(Client client, String... patterns) {
        super.proceedWithPatterns(client, patterns);
    }

    @Override
    public void proceed(Client client, String... channels) {
        super.proceed(client, channels);
    }

    @Override
    public int getSubscribedChannels() {
        return super.getSubscribedChannels();
    }
}
