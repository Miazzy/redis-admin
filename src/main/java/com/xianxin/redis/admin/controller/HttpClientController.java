package com.xianxin.redis.admin.controller;

import com.xianxin.redis.admin.bean.po.HttpRequestConfig;
import com.xianxin.redis.admin.service.HttpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@RestController
@RequestMapping("/httpclient")
public class HttpClientController {

    @Autowired
    private HttpClientService httpClientService;

    @PostMapping(path = "/request")
    public Object request(@RequestBody HttpRequestConfig config) {

        return httpClientService.request(config);
    }
}
