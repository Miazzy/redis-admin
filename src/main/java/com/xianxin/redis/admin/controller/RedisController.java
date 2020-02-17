package com.xianxin.redis.admin.controller;

import com.xianxin.redis.admin.bean.dto.SysRedisDto;
import com.xianxin.redis.admin.bean.po.RedisConfig;
import com.xianxin.redis.admin.bean.po.SysRedis;
import com.xianxin.redis.admin.bean.vo.*;
import com.xianxin.redis.admin.framework.annotation.LogAnnotation;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.framework.config.SysConfig;
import com.xianxin.redis.admin.service.SysRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/01/29
 */
@RestController
@RequestMapping(path = "/redis")
public class RedisController extends BaseController {

    @Autowired
    private SysRedisService sysRedisService;

    @LogAnnotation(module = "配置", business = "列表查询")
    @GetMapping(path = "/config/query")
    public Response<List<RedisConfig>> queryConfig() {

        return sysRedisService.list();
    }

    @LogAnnotation(module = "缓存", business = "列表查询")
    @PostMapping(path = "/cache/query")
    public Response<List<SysRedis>> queryCache(@RequestBody CacheRedisQueryVo vo) {

        return sysRedisService.cacheList(vo);
    }

    @LogAnnotation(module = "缓存", business = "详情查询")
    @PostMapping(path = "/cache/details")
    public Response<SysRedisDto> queryCacheDetails(@RequestBody CacheRedisQueryVo vo) {

        return sysRedisService.cacheDetails(vo);
    }

    @LogAnnotation(module = "缓存", business = "同步")
    @PostMapping(path = "/cache/synch")
    public Response cachSynch(@RequestBody CacheSynchVo vo) {

        return sysRedisService.cachSynch(vo);
    }

    @LogAnnotation(module = "缓存", business = "发布")
    @PostMapping(path = "/cache/publish")
    public Response cachPublish(@RequestBody CachPublishVo vo) {

        return sysRedisService.cachPublish(vo);
    }

    @LogAnnotation(module = "缓存", business = "创建缓存")
    @PostMapping(path = "/cache/create")
    public Response cacheCreate(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheCreate(vo);
    }

    @LogAnnotation(module = "缓存", business = "更新缓存名称")
    @PostMapping(path = "/cache/name/update")
    public Response cacheNameUpdate(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheNameUpdate(vo);
    }

    @LogAnnotation(module = "缓存", business = "更新缓存失效时间")
    @PostMapping(path = "/cache/expire/update")
    public Response cacheExpireUpdate(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheExpireUpdate(vo);
    }

    @LogAnnotation(module = "缓存", business = "创建缓存值")
    @PostMapping(path = "/cache/value/create")
    public Response cacheValueCreate(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheValueCreate(vo);
    }

    @LogAnnotation(module = "缓存", business = "更新缓存值")
    @PostMapping(path = "/cache/value/update")
    public Response cacheValueUpdate(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheValueUpdate(vo);
    }

    @LogAnnotation(module = "缓存", business = "删除缓存")
    @PostMapping(path = "/cache/delete")
    public Response cacheDelete(@RequestBody CacheRedisVo vo) {

        return sysRedisService.cacheDelete(vo);
    }

    @LogAnnotation(module = "缓存", business = "删除缓存")
    @GetMapping(path = "/config/select")
    public Response<List<Map<String, String>>> select() {

        return sysRedisService.select();
    }

    @LogAnnotation(module = "配置", business = "创建配置")
    @PostMapping(path = "/config/create")
    public Response create(@RequestBody SysRedisCreateVo vo) {

        return sysRedisService.save(vo);
    }

    @LogAnnotation(module = "配置", business = "更新配置")
    @PostMapping(path = "/config/update")
    public Response update(@RequestBody SysRedisUpdateVo vo) {

        return sysRedisService.update(vo);
    }

    @LogAnnotation(module = "配置", business = "启用配置")
    @GetMapping(path = "/config/enabled")
    public Response enabled(String host) {

        return sysRedisService.enabled(host);
    }

    @LogAnnotation(module = "配置", business = "停用配置")
    @GetMapping(path = "/config/disabled")
    public Response disabled(String host) {

        return sysRedisService.disabled(host);
    }

    @LogAnnotation(module = "配置", business = "删除配置")
    @GetMapping(path = "/config/delete")
    public Response delete(String host) {

        return sysRedisService.delete(host);
    }

    @LogAnnotation(module = "配置", business = "测试连接")
    @PostMapping(path = "/config/test/conn")
    public Response connection(@RequestBody RedisConfig config) {

        SysConfig.testConnection(config);

        return Response.success("redis-server连接成功");
    }

    @LogAnnotation(module = "配置", business = "获取DB")
    @GetMapping(path = "/config/select/db")
    public Response selectDb(String host) {

        return sysRedisService.selectDb(host);
    }
}
