package com.xianxin.redis.admin;

import com.xianxin.redis.admin.framework.listener.KeyExpiredListener;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

public class KeyExpiredListenerOne {

   private static KeyExpiredListener listener = new KeyExpiredListener();

    public static void main(String[] args) {
//        start();

        stop();
    }

    public static void start(){
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "47.95.12.18", 6397, 60000, "yunfeiredis2020");
        Jedis jedis = pool.getResource();
        config(jedis);
        listener = new KeyExpiredListener(pool);
        jedis.psubscribe(listener,  "__key*__:*");
    }

    public static void stop(){
        listener.punsubscribe("__key*__:*");
    }

    private static void config(Jedis jedis) {
        String parameter = "notify-keyspace-events";
        List<String> notify = jedis.configGet(parameter);

        if (notify.get(1).equals("")) {
            jedis.configSet(parameter, "KEA");
        }
    }
}
