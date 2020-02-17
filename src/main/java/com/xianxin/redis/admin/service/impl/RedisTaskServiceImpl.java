package com.xianxin.redis.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xianxin.redis.admin.bean.po.HttpRequestConfig;
import com.xianxin.redis.admin.bean.po.HttpResponsePlus;
import com.xianxin.redis.admin.bean.po.RedisTaskConfig;
import com.xianxin.redis.admin.bean.po.TaskConfig;
import com.xianxin.redis.admin.bean.vo.CacheRedisVo;
import com.xianxin.redis.admin.bean.vo.RedisTaskExecuteVo;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.framework.config.SysConfig;
import com.xianxin.redis.admin.service.HttpClientService;
import com.xianxin.redis.admin.service.RedisTaskService;
import com.xianxin.redis.admin.service.SysRedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class RedisTaskServiceImpl implements RedisTaskService {

    @Autowired
    private SysRedisService sysRedisService;

    @Autowired
    private HttpClientService httpClientService;


    @Override
    public Response create(RedisTaskConfig config) {

        List<TaskConfig> taskConfigs = config.getTaskConfigs();

        if (taskConfigs != null && taskConfigs.size() > 0) {

            AtomicReference<Response> response = new AtomicReference<>(new Response());
            taskConfigs.forEach(taskConfig -> {

                CacheRedisVo vo = new CacheRedisVo();
                BeanUtils.copyProperties(config, vo);

                vo.setRedisKey(config.getKey());
                vo.setDataType("list");
                String value = JSON.toJSONString(taskConfig);
                vo.setRedisValue(value);

                response.set(sysRedisService.cacheCreate(vo));

                if (response.get().getCode() == HttpStatus.OK.value()) {

                    vo = new CacheRedisVo();
                    BeanUtils.copyProperties(config, vo);

                    JSONObject object = new JSONObject();

                    object.put("key", config.getKey());
                    object.put("name", config.getName());
                    object.put("available", config.isAvailable());
                    object.put("host", config.getHost());
                    object.put("db", config.getDb());

                    vo.setRedisKey("REDIS_ADMIN:REDIS_TASK_GROUP");
                    vo.setRedisHKey(config.getKey());
                    vo.setRedisValue(object.toJSONString());
                    vo.setDataType("hash");
                    response.set(sysRedisService.cacheCreate(vo));
                }
            });

            return response.get();
        } else {

            return Response.error("任务组不能为空");
        }

    }

    @Override
    public Response execute(RedisTaskExecuteVo vo) {

        Jedis jedis = SysConfig.getJedis(vo.getHost(), vo.getDb());

        String text = jedis.hget("REDIS_ADMIN:REDIS_TASK_GROUP", vo.getKey());

        if (StringUtils.isNotBlank(text)) {
            RedisTaskConfig redisTaskConfig = JSON.parseObject(text, RedisTaskConfig.class);

            if (redisTaskConfig.isAvailable()) {

                List<TaskConfig> taskConfigList = getTaskConfigs(jedis, vo);

                executeTask(taskConfigList);

                return Response.success("执行成功");
            } else {
                log.info("任务：{} 不存在", vo.getKey());
                return Response.error("[" + redisTaskConfig.getName() + "]已停用");
            }
        } else {
            return Response.error("任务组不存在");
        }
    }

    @Async
    public void executeTask(List<TaskConfig> taskConfigList) {

        Map<String, Map<String, Object>> resultRepository = new HashMap<>(16);

        for (TaskConfig taskConfig : taskConfigList) {

            List<HttpRequestConfig> httpRequestConfigs = taskConfig.getHttpRequestConfigs();
            if (httpRequestConfigs != null && httpRequestConfigs.size() > 0) {

                for (HttpRequestConfig httpRequestConfig : httpRequestConfigs) {
                    Object responseData = null;
                    Object resultObj = null;
                    // 处理
                    if ("request".equals(httpRequestConfig.getHttpType())) {
                        log.info("执行 request 请求");

                        resultObj = httpClientService.request(httpRequestConfig);

                        if ("sys".equals(httpRequestConfig.getRespFormat())) {
                            if (resultObj instanceof Response) {
                                Response response = (Response) resultObj;
                                responseData = response.getData();
                            }
                        }

                        if ("api".equals(httpRequestConfig.getRespFormat())) {
                            responseData = resultObj;
                        }

                        if ("http".equals(httpRequestConfig.getRespFormat())) {
                            if (resultObj instanceof HttpResponsePlus) {
                                HttpResponsePlus responsePlus = (HttpResponsePlus) resultObj;
                                responseData = responsePlus.getBody();
                            }
                        }

                    } else if ("callback".equals(httpRequestConfig.getHttpType())) {
                        log.info("执行 callback 请求");

                        if (StringUtils.isNotBlank(httpRequestConfig.getParentId())) {
                            Map<String, Object> object = resultRepository.get(httpRequestConfig.getParentId());

                            if (object != null && object.size() > 0) {
                                Object responseDataObj = object.get("responseData");

                                HttpRequestConfig preHttpRequestConfig = (HttpRequestConfig) object.get("httpRequestConfig");

                                if (StringUtils.isNotBlank(httpRequestConfig.getContentType())) {

                                    if ("form-data".equals(httpRequestConfig.getContentType())) {
                                        Map<String, Object> formData = httpRequestConfig.getFormData();

                                        String respDataType = StringUtils.isNotBlank(preHttpRequestConfig.getRespDataType()) ? preHttpRequestConfig.getRespDataType() : "string";

                                        if ("string".equals(respDataType) || "json".equals(respDataType)) {
                                            formData.put("body", responseDataObj.toString());
                                        }

                                        if ("object".equals(respDataType)) {
                                            String text = JSONObject.toJSONString(responseDataObj);
                                            JSONObject jsonObject = JSON.parseObject(text);

                                            for (String key : formData.keySet()) {
                                                Object value = jsonObject.get(key);
                                                formData.put(key, value);
                                            }
                                        }

                                        httpRequestConfig.setFormData(formData);
                                    }

                                    if (MediaType.APPLICATION_JSON_VALUE.equals(httpRequestConfig.getContentType())) {
                                        String body = JSON.toJSONString(responseDataObj);
                                        httpRequestConfig.setBody(body);
                                    }
                                }

                            }
                        }

                        resultObj = httpClientService.request(httpRequestConfig);

                        if ("sys".equals(httpRequestConfig.getRespFormat())) {
                            if (resultObj instanceof Response) {
                                Response response = (Response) resultObj;
                                responseData = response.getData();
                            }
                        }

                        if ("api".equals(httpRequestConfig.getRespFormat())) {
                            responseData = resultObj;
                        }

                        if ("http".equals(httpRequestConfig.getRespFormat())) {
                            if (resultObj instanceof HttpResponsePlus) {
                                HttpResponsePlus responsePlus = (HttpResponsePlus) resultObj;
                                responseData = responsePlus.getBody();
                            }
                        }
                    }

                    // 存储需要回调http请求 处理的
                    if (httpRequestConfig.isDispose()) {
                        Map<String, Object> map = new HashMap<>(2);
                        map.put("responseData", responseData);
                        map.put("httpRequestConfig", httpRequestConfig);

                        resultRepository.put(httpRequestConfig.getId(), map);
                    }
                }
            }
        }

        // 清空内存
        resultRepository.clear();
    }

    private List<TaskConfig> getTaskConfigs(Jedis jedis, RedisTaskExecuteVo vo) {

        long elCount = jedis.llen(vo.getKey());

        if (elCount > 0) {

            List<String> values = jedis.lrange(vo.getKey(), 0, elCount);

            jedis.close();

            if (values != null && values.size() > 0) {
                List<TaskConfig> taskConfigs = new ArrayList<>();

                for (String value : values) {
                    TaskConfig taskConfig = JSON.parseObject(value, TaskConfig.class);
                    taskConfigs.add(taskConfig);
                }

                return taskConfigs;
            }
        }

        return null;
    }


}
