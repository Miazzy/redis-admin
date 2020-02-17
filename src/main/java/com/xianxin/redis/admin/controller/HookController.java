package com.xianxin.redis.admin.controller;

import com.xianxin.redis.admin.bean.vo.HookCommonVo;
import com.xianxin.redis.admin.bean.po.HookSimpleJobConfig;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.service.HookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@RestController
//@Controller
@RequestMapping("/hook")
public class HookController {

    @Autowired
    private HookService hookService;

    @PostMapping(path = "/psubscribe")
    public Response psubscribe(@RequestBody HookCommonVo vo) {

        return hookService.psubscribe(vo);
    }

    @PostMapping(path = "/punsubscribe")
    public void punsubscribe(@RequestBody HookCommonVo vo) {

        hookService.punsubscribe(vo);
    }

    @PostMapping(path = "/pubsub")
    public Response pubsub(@RequestBody HookCommonVo vo) {

        return hookService.pubsub(vo);
    }

    @PostMapping(path = "/create")
    public Response createHook(@RequestBody HookSimpleJobConfig config) throws ParseException {

        return hookService.createHook(config);
    }

}
