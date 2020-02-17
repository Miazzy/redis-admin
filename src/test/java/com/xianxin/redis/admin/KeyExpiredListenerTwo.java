package com.xianxin.redis.admin;

import com.xianxin.redis.admin.framework.listener.KeyExpiredListener;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

public class KeyExpiredListenerTwo {

    public static void main(String[] args) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "47.95.12.18", 6397, 60000, "yunfeiredis2020");
        Jedis jedis = pool.getResource();
        config(jedis);
        jedis.psubscribe(new KeyExpiredListener(), "test_publish");
    }

    private static void config(Jedis jedis) {
        String parameter = "notify-keyspace-events";
        List<String> notify = jedis.configGet(parameter);

        if (notify.get(1).equals("")) {
            jedis.configSet(parameter, "KEA");
        }
    }
}
