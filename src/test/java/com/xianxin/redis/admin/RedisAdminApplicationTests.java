package com.xianxin.redis.admin;

import com.alibaba.fastjson.JSON;
import com.xianxin.redis.admin.bean.po.HookExpireConfig;
import com.xianxin.redis.admin.bean.po.HookSimpleJobConfig;
import com.xianxin.redis.admin.bean.po.SysRedis;
import com.xianxin.redis.admin.framework.listener.KeyExpiredListener;
import com.xianxin.redis.admin.framework.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;
import redis.clients.jedis.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@SpringBootTest
class RedisAdminApplicationTests {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyy-MM-dd");

    private static SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");


    public static void main2(String[] args) {

        JedisPool pool = new JedisPool(new JedisPoolConfig(), "47.95.12.18", 6397, 60000, "yunfeiredis2020");

        Jedis jedis = pool.getResource();

        config(jedis);

        jedis.psubscribe(new KeyExpiredListener(pool), "__key*__:*");
    }

    private static void config(Jedis jedis) {
        String parameter = "notify-keyspace-events";
        List<String> notify = jedis.configGet(parameter);

        if (notify.get(1).equals("")) {
            jedis.configSet(parameter, "KEA");
        }
    }

    public static void main(String[] args) throws ParseException {
        HookSimpleJobConfig hookSimpleJobConfig = new HookSimpleJobConfig();

        hookSimpleJobConfig.setKey("test:hook_1");

        hookSimpleJobConfig.setValue("测试hook simple job运行");

        hookSimpleJobConfig.setExpire(5);

        hookSimpleJobConfig.setTotal(10);

        hookSimpleJobConfig.setAvailable(false);

//        hookSimpleJobConfig.setDateType("2");
        hookSimpleJobConfig.setDateType("1");

//        hookSimpleJobConfig.setStartTime("13:00:00");
//        hookSimpleJobConfig.setStartTime("2020-02-16 19:24:10");
//        hookSimpleJobConfig.setStartTime("2020-02-17 09:24:10");

//        hookSimpleJobConfig.setEndTime("14:00:00");
//        hookSimpleJobConfig.setEndTime("2020-02-16 19:25:15");
//        hookSimpleJobConfig.setEndTime("2020-02-17 19:42:10");

        Date curr = new Date();
        Date currStart = DateUtils.addSeconds(curr, 5);
        Date currEnd = DateUtils.addSeconds(curr, 35);
        hookSimpleJobConfig.setStartTime(sdf.format(currStart));
        hookSimpleJobConfig.setEndTime(sdf.format(currEnd));

        hookSimpleJobConfig.setLifeCycle("2");

        hookSimpleJobConfig.setRemark("这是测试备注。。。");


        HookExpireConfig hookExpireConfig = getExpireConfig(hookSimpleJobConfig);
        log.info("hookExpireConfig={}", JSON.toJSONString(hookExpireConfig));

        JedisPool pool = new JedisPool(new JedisPoolConfig(), "47.95.12.18", 6397, 60000, "yunfeiredis2020");
        Jedis jedis = pool.getResource();

        if (hookSimpleJobConfig.getDateType().equals("-1")) {

        } else {
            // 主要
            jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookExpireConfig));

            jedis.set(hookSimpleJobConfig.getKeyStart(), JSON.toJSONString(hookExpireConfig));
            jedis.expire(hookSimpleJobConfig.getKeyStart(), hookExpireConfig.getStartExpire());

            jedis.set(hookSimpleJobConfig.getKeyEnd(), JSON.toJSONString(hookExpireConfig));
            jedis.expire(hookSimpleJobConfig.getKeyEnd(), hookExpireConfig.getEndExpire());
        }
    }

    private static HookExpireConfig getExpireConfig(HookSimpleJobConfig hookSimpleJobConfig) throws ParseException {
        HookExpireConfig hookExpireConfig = new HookExpireConfig();

        boolean isAdd = false;
        Date currDate = new Date();

        // 计算当前 任务名_start key 的存活时间
        String currDateYmd = sdf1.format(currDate);

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
        String endDateYmd = sdf1.format(startDate);
        String endDateStr = "";

        if (isAdd) {
            Date endTime = sdf.parse(hookSimpleJobConfig.getEndTime());
            String endDateHms = sdf2.format(endTime);
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

    @Test
    public void expire() throws ParseException {
//        JedisPool pool = new JedisPool(new JedisPoolConfig(), "47.95.12.18", 6397, 60000, "yunfeiredis2020");
//        Jedis jedis = pool.getResource();

        HookSimpleJobConfig hookSimpleJobConfig = new HookSimpleJobConfig();

        hookSimpleJobConfig.setKey("test:hook_simple_job_config");

        hookSimpleJobConfig.setValue("测试hook simple job运行");

        hookSimpleJobConfig.setExpire(3);

        hookSimpleJobConfig.setTotal(10);

        hookSimpleJobConfig.setAvailable(false);

        hookSimpleJobConfig.setDateType("2");

        hookSimpleJobConfig.setStartTime("13:00:00");

        hookSimpleJobConfig.setEndTime("14:00:00");

        hookSimpleJobConfig.setRemark("这是测试备注。。。");

        if (hookSimpleJobConfig.getDateType().equals("1")) {
            // HH:mm:ss
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyy-MM-dd");
            SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

            // 计算当前 任务名_start key 的失效时间
            Date currDate = new Date();

            String currDateYmd = sdf1.format(currDate);
            String currDateStr = currDateYmd + " " + hookSimpleJobConfig.getStartTime();
            log.info("拼接后的时间字符串：{}", currDateStr);
            Date startTime = sdf.parse(currDateStr);
            long startSeconds = DateUtils.calcSeconds(currDate, startTime);
            log.info("任务名_start key有效存活时间：{}(s)", startSeconds);


            // 计算下一次 任务名_start key 的失效时间
        } else if (hookSimpleJobConfig.getDateType().equals("2")) {

        }

        // set 任务key
//        jedis.set(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getValue());
//        jedis.expire(hookSimpleJobConfig.getKey(), hookSimpleJobConfig.getExpire());


        // set 任务_expire key
//        jedis.set(hookSimpleJobConfig.getKeyExpire(), hookSimpleJobConfig.getExpire() + "");
        // set 任务—_job key
//        jedis.set(hookSimpleJobConfig.getKeyJob(), JSON.toJSONString(hookSimpleJobConfig));


//        long publish = jedis.publish("test_publish", "测试消息推送");
//        log.info("消息推送发送成功！{} 人已接收", publish);
    }

    public void jobTest() {

    }

    @Test
    public void publish() {
        JedisShardInfo jedisShardInfo = new JedisShardInfo("47.95.12.18", 6397);
        jedisShardInfo.setPassword("yunfeiredis2020");
        Jedis jedis = new Jedis(jedisShardInfo);

        long publish = jedis.publish("test_publish", "测试消息推送");

        log.info("消息推送发送成功！{}", publish);
    }

    @Test
    public void selectDb() {
        JedisShardInfo jedisShardInfo = new JedisShardInfo("47.95.12.18", 6397);
        jedisShardInfo.setPassword("yunfeiredis2020");
        Jedis jedis = new Jedis(jedisShardInfo);

        for (int i = 0; i < 16; i++) {
            System.out.println("SELECT " + i);

            try {
                String selectDb = jedis.select(i);
                System.out.println(selectDb);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        }

    }

    @Test
    void contextLoads() {
        //redis://:foobared@10.0.50.11:6379/1
//        JedisShardInfo shardInfo = new JedisShardInfo("redis://:bati@192.168.1.60:6379/0");
        StopWatch watch = new StopWatch();
        watch.start();
        JedisShardInfo shardInfo = new JedisShardInfo("redis://:bati@192.168.16.94:6379/0");
//        shardInfo.setPassword("bati");
        Jedis jedis = new Jedis(shardInfo);
        Long dbsize = jedis.dbSize();
        System.out.println("dbsize=" + dbsize);
        String json = jedis.hget("D_INS_CONFIG:APP_CITY", "340100");
        System.out.println(json);

//        Set<String> keys = jedis.keys("D_INS*");
//        System.out.println("keys.size=" + keys.size());
//        keys.forEach(key -> {
//            String type = jedis.type(key);
//            System.out.println("key=" + key + ", type=" + type);
//        });

//        String scanRet = "0";
        ScanParams scanParams = new ScanParams();
//        int count = 10000;
//        scanParams.count(count);
//        String match = "D_INS_CONFIG*";
////        String match = "*";
//        scanParams.match(match);
//        List<String> keys = new ArrayList<>();
//
//        do {
//            ScanResult<String> scanResult = jedis.scan(scanRet, scanParams);
//            scanRet = scanResult.getCursor();
//            //scan 487439 MATCH * COUNT 10000
//            System.out.println("scan " + scanRet + " MATCH " + match + " COUNT " + count);
//            keys.addAll(scanResult.getResult());
//        } while (!scanRet.equals("0"));
//
//        System.out.println(keys.size());
//
//        List<Map<String, Object>> keyMap = new ArrayList<>();
//        keys.forEach(key -> {
//            Map<String, Object> map = new HashMap<>();
//            map.put("key", key);
//            String type = jedis.type(key);
//            map.put("type", type);
//            keyMap.add(map);
//        });
//
//        System.out.println(keyMap.size());


        Long total = jedis.llen("*");
        System.out.println("total=" + total);

        watch.stop();
        double time = watch.getTotalTimeSeconds();
        System.out.println("耗时：" + time + "s");
    }


    @Test
    public void test() {
        JedisShardInfo shardInfo = new JedisShardInfo("redis://:bati@192.168.1.60:6379/0");
        StopWatch watch = new StopWatch();
        watch.start();
        Jedis jedis = new Jedis(shardInfo);
        List<String> ls = jedis.lrange("*", 10, 10);


        System.out.println(ls.size());
    }

    @Test
    public void selectPage() {

        selectPage("test_set", 1, 10);

    }

    private void selectPage(String keyword, int pageNo, int pageSize) {
        JedisShardInfo shardInfo = new JedisShardInfo("redis://:bati@192.168.1.60:6379/0");
        Jedis jedis = new Jedis(shardInfo);
        ScanParams scanParams = new ScanParams();
        int count = 10000;
        scanParams.count(count);
        String match = "*";
        if (StringUtils.isNotBlank(keyword)) {
            match = keyword + "*";
        }
        scanParams.match(match);
        String scanRet = "0";

        List<String> keyList = new ArrayList<>();
        do {
            ScanResult<String> scanResult = jedis.scan(scanRet, scanParams);
            scanRet = scanResult.getCursor();
            //scan 487439 MATCH * COUNT 10000
            System.out.println("scan " + scanRet + " MATCH " + match + " COUNT " + count);

            keyList.addAll(scanResult.getResult());
        } while (!scanRet.equals("0"));

        if (!CollectionUtils.isEmpty(keyList)) {
            List<SysRedis> list = new ArrayList<>();
            int start = pageNo == 1 ? 0 : (pageNo - 1) * pageSize;
            for (int i = start, j = 0; i < keyList.size() && j < pageSize; i++, j++) {
                String key = keyList.get(i);

                SysRedis base = baseInfo(key, jedis);
                list.add(base);
            }


            System.out.println(list.size());
        }
    }

    private SysRedis baseInfo(String key, Jedis jedis) {
        String type = jedis.type(key);
        Long expire = jedis.ttl(key);
        System.out.println("key=" + key + "，type=" + type);
        SysRedis sysRedis = new SysRedis(type, key, null, String.valueOf(expire));
        Long elCount = 0L;
        if ("string".equals(type)) {
            elCount = 1L;
        } else if ("list".equals(type)) {
            elCount = jedis.llen(key);
        } else if ("hash".equals(type)) {
            elCount = jedis.hlen(key);
        } else if ("set".equals(type)) {
            elCount = jedis.scard(key);
        } else if ("zset".equals(type)) {
            elCount = jedis.zcard(key);
        }
        sysRedis.setElCount(elCount);
        return sysRedis;
    }
}
