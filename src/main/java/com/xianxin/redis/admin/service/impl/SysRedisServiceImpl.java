package com.xianxin.redis.admin.service.impl;

import com.xianxin.redis.admin.bean.dto.SysRedisDto;
import com.xianxin.redis.admin.bean.po.RedisConfig;
import com.xianxin.redis.admin.bean.po.SysRedis;
import com.xianxin.redis.admin.bean.vo.*;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.framework.config.SysConfig;
import com.xianxin.redis.admin.framework.utils.DateUtils;
import com.xianxin.redis.admin.service.SysRedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/01/30
 */
@Service
@Slf4j
public class SysRedisServiceImpl implements SysRedisService {

    @Override
    public boolean saveOrUpdate(RedisConfig po, boolean conn) {
        // 尝试连接redis
//        if (conn) {
//            SysConfig.testConnection(po);
//        }
        SysConfig.saveOrUpdateRedisConfig(po);
        return true;
    }

    @Override
    public boolean enabledOrDisabled(String host, boolean status) {
        RedisConfig redisConfig = SysConfig.findRedisConfigByUsername(host);
        if (redisConfig != null) {
            redisConfig.setStatus(status);

            return saveOrUpdate(redisConfig, false);
        }
        return false;
    }

    @Override
    public Response enabled(String host) {
        boolean isSuccess = enabledOrDisabled(host, true);
        if (isSuccess) {
            return Response.success("启用成功");
        }
        return Response.error("启用失败");
    }

    @Override
    public Response disabled(String host) {
        boolean isSuccess = enabledOrDisabled(host, false);
        if (isSuccess) {
            return Response.success("停用成功");
        }
        return Response.error("停用失败");
    }

    @Override
    public Response save(SysRedisCreateVo vo) {
        RedisConfig po = new RedisConfig();
        BeanUtils.copyProperties(vo, po);
        boolean isSuccess = saveOrUpdate(po, true);
        if (isSuccess) {
            return Response.success("添加成功");
        }
        return Response.error("添加失败");
    }

    @Override
    public Response update(SysRedisUpdateVo vo) {
        RedisConfig po = new RedisConfig();
        BeanUtils.copyProperties(vo, po);
        boolean isSuccess = saveOrUpdate(po, true);
        if (isSuccess) {
            return Response.success("修改成功");
        }
        return Response.error("修改失败");
    }

    @Override
    public Response<List<RedisConfig>> list() {

        return Response.success(listAll());
    }

    public List<RedisConfig> listAll() {
        List<RedisConfig> list = new ArrayList<>();
        for (RedisConfig redisConfig : SysConfig.getRedisConfigValues()) {
            //cacheRedis.setPassword("******");
            list.add(redisConfig);
        }
        return list;
    }

    @Override
    public Response<List<Map<String, String>>> select() {
        List<RedisConfig> list = listAll();
        List<Map<String, String>> selectList = new ArrayList<>();
        list.forEach(redisConfig -> {
            if (redisConfig.getStatus()) {
                Map<String, String> map = new HashMap<>();
                map.put("host", redisConfig.getHost());
                map.put("name", redisConfig.getName());
                selectList.add(map);
            }
        });

        return Response.success(selectList);
    }

    @Override
    public Response<List<SysRedis>> cacheList(CacheRedisQueryVo vo) {
        RedisConfig redisConfig = SysConfig.findRedisConfigByUsername(vo.getHost());
        if (redisConfig == null) {
            return Response.error("配置信息不存在");
        }
        String match = "*";
        if (StringUtils.isNotBlank(vo.getKeyword())) {
            match = "*" + vo.getKeyword() + "*";
        } else {
            if ("prod".equals(redisConfig.getEnv())) {
                return Response.error("线上环境不支持查询全部");
            }
        }

        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        ScanParams scanParams = new ScanParams();
        int count = 10000;
        scanParams.count(count);

        scanParams.match(match);
        String scanRet = "0";

        List<String> keyList = new ArrayList<>();
        do {
            ScanResult<String> scanResult = jedis.scan(scanRet, scanParams);
            scanRet = scanResult.getCursor();
            //scan 487439 MATCH * COUNT 10000
            log.info("scan {} MATCH {} COUNT {}", scanRet, vo.getKeyword(), count);

            keyList.addAll(scanResult.getResult());
        } while (!"0".equals(scanRet));

        if (!CollectionUtils.isEmpty(keyList)) {
            List<SysRedis> list = new ArrayList<>();
            int start = vo.getPageNo() == 1 ? 0 : (vo.getPageNo() - 1) * vo.getPageSize();
            for (int i = start, j = 0; i < keyList.size() && j < vo.getPageSize(); i++, j++) {
                String key = keyList.get(i);

                SysRedis base = baseInfo(key, jedis);
                list.add(base);
            }
            jedis.close();
            int total = keyList.size();
            // 清空加载出来全部
            keyList.clear();
            return Response.success(200, total, list);
        }
        return Response.success(200, 0, "暂无数据");
    }

    @Override
    public Response<SysRedisDto> cacheDetails(CacheRedisQueryVo vo) {
        String type = vo.getType();
        String key = vo.getKeyword();
        Long elCount = 0L;
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        Long expire = jedis.ttl(key);
        String expireStr = "";
        if (expire == -1) {
            expireStr = "永不过期";
        } else {
            expireStr = DateUtils.getTimeStrBySecond(expire);
        }
        SysRedisDto dto = new SysRedisDto();
        if ("string".equals(type)) {
            elCount = 1L;
            String value = jedis.get(key);
            dto.setRedisValue(value);
        } else if ("list".equals(type)) {
            elCount = jedis.llen(key);

            if (elCount > 0) {
                List<String> listValues = jedis.lrange(key, 0, elCount);
                if (listValues != null && listValues.size() > 0) {
                    List<Map<String, Object>> values = new ArrayList<>();
                    int no = 1;
                    for (String m : listValues) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("no", no);
                        map.put("svalue", m);
                        values.add(map);
                        no++;
                    }
                    dto.setValues(values);
                }
            }
        } else if ("hash".equals(type)) {
            elCount = jedis.hlen(key);
            Map<String, String> hgetAll = jedis.hgetAll(key);
            if (hgetAll != null && hgetAll.size() > 0) {
                List<Map<String, Object>> values = new ArrayList<>();
                int no = 1;
                for (String hk : hgetAll.keySet()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("no", no);
                    map.put("hkey", hk);
                    map.put("hvalue", hgetAll.get(hk));
                    values.add(map);
                    no++;
                }
                dto.setValues(values);
            }
        } else if ("set".equals(type)) {
            elCount = jedis.scard(key);
            Set<String> smembers = jedis.smembers(key);
            if (smembers != null && smembers.size() > 0) {
                List<Map<String, Object>> values = new ArrayList<>();
                int no = 1;
                for (String m : smembers) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("no", no);
                    map.put("svalue", m);
                    values.add(map);
                    no++;
                }
                dto.setValues(values);
            }
        } else if ("zset".equals(type)) {
            elCount = jedis.zcard(key);
            long zcard = jedis.zcard(key);
            Set<String> zset = jedis.zrange(key, 0, zcard);
            if (zset != null && zset.size() > 0) {
                List<Map<String, Object>> values = new ArrayList<>();
                int no = 1;
                for (String m : zset) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("no", no);
                    map.put("zvalue", m);
                    double zscore = jedis.zscore(key, m);
                    map.put("zscore", zscore);
                    values.add(map);
                    no++;
                }
                dto.setValues(values);
            }
        }

        if (dto.getValues() != null && dto.getValues().size() > 0) {
            // 过滤查询值列表关键字
            List<Map<String, Object>> valueAll = dto.getValues();
            List<Map<String, Object>> filterValueAll = new ArrayList<>();
            if (StringUtils.isNotBlank(vo.getCacheKeyword())) {
                for (int i = 0; i < valueAll.size(); i++) {
                    Map<String, Object> map = valueAll.get(i);
                    String hk = "";
                    if ("hash".equals(type)) {
                        hk = map.get("hkey").toString();
                    } else if ("zset".equals(type)) {
                        hk = map.get("zvalue").toString();
                    } else if ("set".equals(type)) {
                        hk = map.get("svalue").toString();
                    } else if ("list".equals(type)) {
                        hk = map.get("svalue").toString();
                    }
                    if (hk.contains(vo.getCacheKeyword())) {
                        filterValueAll.add(map);
                    }
                }
                elCount = Long.parseLong(filterValueAll.size() + "");
            } else {
                filterValueAll.addAll(valueAll);
            }

            // 分页
            List<Map<String, Object>> values = new ArrayList<>();
            int start = vo.getPageNo() == 1 ? 0 : (vo.getPageNo() - 1) * vo.getPageSize();
            for (int i = start, j = 0; i < filterValueAll.size() && j < vo.getPageSize(); i++, j++) {
                Map<String, Object> map = filterValueAll.get(i);

                values.add(map);
            }

            dto.setValues(values);
        }

        dto.setDataType(type);
        dto.setExpireStr(expireStr);

        // 过期时间 数值类型
        dto.setExpire(expire.toString());

        dto.setElCount(elCount);
        dto.setRedisKey(key);
        dto.setOldRedisKey(key);
        jedis.close();
        int count = elCount != null ? Integer.parseInt(elCount.toString()) : 0;
        return Response.success(count, dto);
    }

    @Override
    public Response cacheCreate(CacheRedisVo vo) {
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        String type = vo.getDataType();
        if ("string".equals(type)) {
            jedis.set(vo.getRedisKey(), vo.getRedisValue());
        } else if ("list".equals(type)) {
            // 原始value
            if (StringUtils.isNotBlank(vo.getOldRedisKey())) {
                jedis.lrem(vo.getRedisKey(), 1, vo.getOldRedisKey());
                log.info("list - {} 从 {} 移除", vo.getOldRedisKey(), vo.getRedisKey());
            }
            // 将一个值插入到已存在的列表头部
            jedis.lpush(vo.getRedisKey(), vo.getRedisValue());
//            try {
//                JSONArray array = JSON.parseArray(vo.getRedisValue());
//                for (int i = 0; i < array.size(); i++) {
//                    Object obj = array.get(i);
//                    jedis.lset(vo.getRedisKey(), i, JSON.toJSONString(obj));
//                }
//            } catch (Exception e) {
//                log.error("格式转换错误", e);
//                return Response.error("格式不正确");
//            }
        } else if ("hash".equals(type)) {
            if (StringUtils.isNotBlank(vo.getOldRedisKey())) {
                // 修改 删除以前的key
                jedis.hdel(vo.getRedisKey(), vo.getOldRedisKey());
                log.info("hash - {} 从 {} 移除", vo.getOldRedisKey(), vo.getRedisKey());
            }
            //新增
            jedis.hset(vo.getRedisKey(), vo.getRedisHKey(), vo.getRedisValue());
        } else if ("set".equals(type)) {
            if (StringUtils.isNotBlank(vo.getOldRedisKey())) {
                // 修改 删除以前的key
                jedis.srem(vo.getRedisKey(), vo.getOldRedisKey());
                log.info("set - {} 从 {} 移除", vo.getOldRedisKey(), vo.getRedisKey());
            }
            // 新增
            jedis.sadd(vo.getRedisKey(), vo.getRedisValue());
        } else if ("zset".equals(type)) {
            if (StringUtils.isNotBlank(vo.getOldRedisKey())) {
                // 修改 删除以前的key
                jedis.zrem(vo.getRedisKey(), vo.getOldRedisKey());
                log.info("zset - {} 从 {} 移除", vo.getOldRedisKey(), vo.getRedisKey());
            }
            // 新增
            jedis.zadd(vo.getRedisKey(), vo.getScore(), vo.getRedisValue());
        }

        boolean exists = jedis.exists(vo.getRedisKey());
        if (exists) {
            if (vo.getExpire() > 0) {
                if (StringUtils.isNotBlank(vo.getUnit())) {
                    int expire = DateUtils.getExpire(vo.getExpire(), vo.getUnit());
                    jedis.expire(vo.getRedisKey(), expire);
                } else {
                    jedis.expire(vo.getRedisKey(), vo.getExpire());
                }
            }
            jedis.close();
            return Response.success("添加成功");
        }
        jedis.close();
        return Response.error("添加失败");
    }

    @Override
    public Response cacheDelete(CacheRedisVo vo) {
        String type = vo.getDataType();
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        boolean exists = jedis.exists(vo.getRedisKey());
        if (exists) {
            long count = 0;
            if ("string".equalsIgnoreCase(type)) {
                count = jedis.del(vo.getRedisKey());
            } else if ("hash".equals(type)) {

                if (StringUtils.isNotBlank(vo.getRedisKey()) && StringUtils.isNotBlank(vo.getRedisValue())) {
                    count = jedis.hdel(vo.getRedisKey(), vo.getRedisHKey());
                    log.info("hash - 单个 {} 从 {} 中移除", vo.getRedisHKey(), vo.getRedisKey());
                } else {
                    Map<String, String> hgetAll = jedis.hgetAll(vo.getRedisKey());
                    if (hgetAll != null && hgetAll.size() > 0) {
                        for (String hk : hgetAll.keySet()) {
                            count += jedis.hdel(vo.getRedisKey(), hk);
                        }
                        log.info("hash - 移除全部");
                    }
                }

            } else if ("set".equals(type)) {
                long elCount = jedis.scard(vo.getRedisKey());
                if (StringUtils.isNotBlank(vo.getRedisValue())) {
                    jedis.srem(vo.getRedisKey(), vo.getRedisValue());
                    log.info("set - 单个 {} 从 {} 中移除", vo.getRedisValue(), vo.getRedisKey());
                } else {
                    for (long i = 0; i < elCount; i++) {
                        // 删除全部
                        jedis.spop(vo.getRedisKey());
                        log.info("set - 移除 {} - 计数：{}", vo.getRedisKey(), (i + 1));
                    }
                    log.info("set - 移除全部完成");
                }
                count = 1;
            } else if ("zset".equals(type)) {
                if (StringUtils.isNotBlank(vo.getRedisValue())) {
                    jedis.zrem(vo.getRedisKey(), vo.getRedisValue());
                    log.info("zset - 单个 {} 从 {} 中移除", vo.getRedisValue(), vo.getRedisKey());
                    count = 1;
                } else {
                    long zcard = jedis.zcard(vo.getRedisKey());
                    Set<String> zset = jedis.zrange(vo.getRedisKey(), 0, zcard);
//                    log.info("zset size={}", zset.size());
                    for (String s : zset) {
                        jedis.zrem(vo.getRedisKey(), s);
                        count++;
                    }
                    log.info("zset - 移除全部");
                }
            } else if ("list".equals(type)) {
                // 删除单个
                if (StringUtils.isNotBlank(vo.getRedisValue())) {
                    count = 1;
                    jedis.lrem(vo.getRedisKey(), 1, vo.getRedisValue());
                    log.info("list - 单个 {} 从 {} 中移除", vo.getRedisValue(), vo.getRedisKey());
                } else {
                    long len = jedis.llen(vo.getRedisKey());
                    if (len > 0) {
                        List<String> lvalues = jedis.lrange(vo.getRedisKey(), 0, len);
                        for (String value : lvalues) {
                            jedis.lrem(vo.getRedisKey(), 1, value);
                            count++;
                        }
                        log.info("list - 移除全部");
                    }
                }
                // 删除全部
            }

            jedis.close();
            if (count > 0) {
                return Response.success(200, (int) count, "删除成功");
            }
        }
        return Response.error("删除失败");
    }

    @Override
    public Response cacheNameUpdate(CacheRedisVo vo) {
        String type = vo.getDataType();
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        boolean exists = jedis.exists(vo.getOldRedisKey());
        if (exists) {
//            if ("string".equalsIgnoreCase(type)) {
//            } else if ("hash".equalsIgnoreCase(type)) {
//            } else if ("set".equalsIgnoreCase(type)) {
//            } else if ("zset".equalsIgnoreCase(type)) {
//            }
            jedis.rename(vo.getOldRedisKey(), vo.getRedisKey());
            return Response.success("更新成功");
        } else {
            return Response.error(vo.getOldRedisKey() + " 不存在或已失效");
        }
    }

    @Override
    public Response cacheValueCreate(CacheRedisVo vo) {
//        String type = vo.getDataType();
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        boolean exists = jedis.exists(vo.getRedisKey());
        if (exists) {
            Long expire = jedis.ttl(vo.getRedisKey());
            vo.setExpire(Integer.parseInt(expire + ""));
            return cacheCreate(vo);
        } else {
            jedis.close();
            return Response.error(vo.getRedisKey() + " 不存在或已失效");
        }
    }

    @Override
    public Response cacheValueUpdate(CacheRedisVo vo) {
        String type = vo.getDataType();
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        boolean exists = jedis.exists(vo.getRedisKey());
        if (exists) {
            Long expire = jedis.ttl(vo.getRedisKey());
            vo.setExpire(Integer.parseInt(expire + ""));
            Response response = new Response();
            if ("string".equals(type)) {
                response = cacheCreate(vo);
            } else if ("list".equals(type)) {
                response = cacheCreate(vo);
            } else if ("hash".equals(type)) {
                response = cacheCreate(vo);
            } else if ("set".equals(type)) {
                response = cacheCreate(vo);
            } else if ("zset".equals(type)) {
                response = cacheCreate(vo);
            }

            if (response.getCode() == 200) {
                response.setMsg("更新成功");
            } else {
                response.setMsg("更新失败");
            }
            return response;
        } else {
            jedis.close();
            return Response.error(vo.getRedisKey() + " 不存在或已失效");
        }
    }

    /**
     * 更新过期时间
     *
     * @param vo
     * @return
     */
    @Override
    public Response cacheExpireUpdate(CacheRedisVo vo) {
        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());
        boolean exists = jedis.exists(vo.getRedisKey());
        if (exists) {
            if (vo.getExpire() == 0) {
                return Response.error("过期时间不能为0");
            }
            if (vo.getExpire() > 0) {
                if (StringUtils.isNotBlank(vo.getUnit())) {
                    int expire = DateUtils.getExpire(vo.getExpire(), vo.getUnit());
                    jedis.expire(vo.getRedisKey(), expire);
                } else {
                    jedis.expire(vo.getRedisKey(), vo.getExpire());
                }
            } else {
                // 如果小于0 就移除key的有效时间
                jedis.persist(vo.getRedisKey());
            }
            jedis.close();
            return Response.success("过期时间更新成功");
        }
        jedis.close();
        return Response.error("过期时间更新失败");
    }

    @Override
    public Response delete(String host) {
        SysConfig.deleteRedisConfig(host);
        return Response.success("删除成功");
    }

    @Override
    public Response selectDb(String host) {
        log.info("HOST： {}", host);

        RedisConfig redisConfig = SysConfig.findRedisConfigByUsername(host);

        // 创建redis连接
        JedisShardInfo jedisShardInfo = new JedisShardInfo(host, redisConfig.getPort());
        if (StringUtils.isNotBlank(redisConfig.getPassword())) {
            jedisShardInfo.setPassword(redisConfig.getPassword());
        } else {
            jedisShardInfo.setPassword(null);
        }
        Jedis jedis = new Jedis(jedisShardInfo);

        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            log.info("SELECT {}", i);

            try {
                // 检查db是否连接扫描成功
                String selectDb = jedis.select(i);
                log.info(selectDb);
                Map<String, String> map = new HashMap<>(2);
                map.put("name", "db" + i);
                map.put("db", i + "");

                list.add(map);
            } catch (Exception e) {
                log.info(e.getMessage());
                if (e.getMessage().contains("NOAUTH Authentication required")) {
                    return Response.error("需要密码认证");
                }

                if (e.getMessage().contains("ERR DB index is out of range")) {
                    // 查询数据库下标越界时 跳出循环
                    break;
                }
            }
        }

        if (list.size() > 0) {
            return Response.success(list);
        }
        // 关闭连接
        jedis.close();
        // ERR DB index is out of range ：ERR数据库索引超出范围
        return Response.error("ERR数据库索引超出范围");
    }

    @Override
    public Response cachSynch(CacheSynchVo vo) {

        // 查询 源缓存的详情
        CacheRedisQueryVo cacheRedisQueryVo = new CacheRedisQueryVo();
        BeanUtils.copyProperties(vo, cacheRedisQueryVo);
        Response<SysRedisDto> redisDtoResponse = cacheDetails(cacheRedisQueryVo);

        if (redisDtoResponse.getCode() == HttpStatus.OK.value()) {
            SysRedisDto sysRedisDto = redisDtoResponse.getData();
            String type = sysRedisDto.getDataType();
            // 创建 目标缓存
            CacheRedisVo cacheRedisVo = new CacheRedisVo();
            cacheRedisVo.setHost(vo.getTargetHost());
            cacheRedisVo.setDb(vo.getTargetDb());
            cacheRedisVo.setDataType(type);

            int expire = StringUtils.isNotBlank(sysRedisDto.getExpire()) ? Integer.parseInt(sysRedisDto.getExpire()) : -1;
            cacheRedisVo.setExpire(expire);

            cacheRedisVo.setRedisKey(sysRedisDto.getRedisKey());

            if ("string".equals(type)) {
                cacheRedisVo.setRedisValue(sysRedisDto.getRedisValue());

                Response cacheCreateString = cacheCreate(cacheRedisVo);
                String msg = cacheCreateString.getCode() == HttpStatus.OK.value() ? "同步成功" : "同步失败";
                log.info("string - {} {}", sysRedisDto.getRedisKey(), msg);
                cacheCreateString.setMsg(msg);

                return cacheCreateString;
            } else {

                return assemblyCachSynch(redisDtoResponse, cacheRedisVo, vo, cacheRedisQueryVo);
            }

        } else {

            return redisDtoResponse;
        }
    }

    @Override
    public Response cachPublish(CachPublishVo vo) {

        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());

        jedis.publish(vo.getChannel(),vo.getMessage());


        return null;
    }

    private Response assemblyCachSynch(Response<SysRedisDto> redisDtoResponse, CacheRedisVo cacheRedisVo, CacheSynchVo vo, CacheRedisQueryVo cacheRedisQueryVo) {
        SysRedisDto sysRedisDto = redisDtoResponse.getData();

        List<Map<String, Object>> values = sysRedisDto.getValues();

        String type = sysRedisDto.getDataType();

        if (values != null && values.size() > 0) {
            // 循环存储缓存
            AtomicReference<Response> responseAtomicReference = new AtomicReference<>();
            values.forEach(val -> {

                if ("list".equals(type) || "set".equals(type)) {

                    Object svalue = val.get("svalue");
                    cacheRedisVo.setRedisValue(svalue.toString());

                } else if ("hash".equals(type)) {

                    Object hkey = val.get("hkey");
                    Object hvalue = val.get("hvalue");
                    cacheRedisVo.setRedisHKey(hkey.toString());
                    cacheRedisVo.setRedisValue(hvalue.toString());

                } else if ("zset".equals(type)) {
                    Object zvalue = val.get("zvalue");
                    Object zscore = val.get("zscore");

                    cacheRedisVo.setScore(Double.parseDouble(zscore.toString()));
                    cacheRedisVo.setRedisValue(zvalue.toString());
                }


                responseAtomicReference.set(cacheCreate(cacheRedisVo));
                String msg = responseAtomicReference.get().getCode() == HttpStatus.OK.value() ? "同步成功" : "同步失败";
                log.info("{} - {} {}", type, sysRedisDto.getRedisKey(), msg);

            });

            if (responseAtomicReference.get().getCode() == HttpStatus.OK.value()) {
                int pageCount = (redisDtoResponse.getCount() + cacheRedisQueryVo.getPageSize() - 1) / cacheRedisQueryVo.getPageSize();
                int nextPage = cacheRedisQueryVo.getPageNo() + 1;

                log.info("{} - 总条数：{}，总页数：{}，当前页：{}，下一页：{}", type, redisDtoResponse.getCount(), pageCount, cacheRedisQueryVo.getPageNo(), nextPage);
                boolean isNextPage = nextPage <= pageCount;
                log.info("是否自动查询下一页数据：{}", isNextPage);
                if (isNextPage) {
                    vo.setPageNo(nextPage);
                    cachSynch(vo);
                }
            }

            Response response = responseAtomicReference.get();
            String msg = responseAtomicReference.get().getCode() == HttpStatus.OK.value() ? "同步成功" : "同步失败";
            log.info("{} - {}", type, msg);
            response.setMsg(msg);
            return response;
        }
        return Response.error("同步失败");
    }

    private SysRedis baseInfo(String key, Jedis jedis) {
        String type = jedis.type(key);
        Long expire = jedis.ttl(key);
        log.info("key={}，type={}", key, type);
        String expireStr = "";
        if (expire == -1) {
            expireStr = "永不过期";
        } else {
            expireStr = DateUtils.getTimeStrBySecond(expire);
        }
        SysRedis sysRedis = new SysRedis(type, key, null, expireStr);
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
