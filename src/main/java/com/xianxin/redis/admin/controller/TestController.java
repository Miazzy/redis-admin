package com.xianxin.redis.admin.controller;

import com.xianxin.redis.admin.framework.common.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {


    @RequestMapping(path = "/callback")
    public Response testCallback(String body) {

        log.info("body:{}", body);

        return Response.success("回调成功");
    }

    @RequestMapping(path = "/ceshi1")
    public Response ceshi1(String body) {

        log.info("ceshi1 body:{}", body);

        return Response.success("调用成功");
    }

    @RequestMapping(path = "/ceshi2")
    public Response ceshi2(String body) {

        log.info("ceshi2 body:{}", body);

        return Response.success("调用成功");
    }
}
