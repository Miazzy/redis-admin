package com.xianxin.redis.admin.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.xianxin.redis.admin.bean.po.HttpRequestConfig;
import com.xianxin.redis.admin.bean.po.HttpResponsePlus;
import com.xianxin.redis.admin.framework.common.Response;
import com.xianxin.redis.admin.service.HttpClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Service
@Slf4j
public class HttpClientServiceImpl implements HttpClientService {

    @Override
    public Object request(HttpRequestConfig config) {
        HttpRequest httpRequest = null;

        String url = "";
        try {
            url = URLDecoder.decode(config.getUrl(), "UTF-8");
            log.info("请求地址：{}", url);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        log.info("请求方式：{}", config.getMethod());

        if (config.getMethod().equalsIgnoreCase("post")) {
            httpRequest = HttpUtil.createPost(url);
        } else if (config.getMethod().equalsIgnoreCase("get")) {
            httpRequest = HttpUtil.createGet(url);
        }

        if (StringUtils.isNotBlank(config.getCharset())) {
            httpRequest.charset(config.getCharset());
            log.info("Add Charset {}", config.getCharset());
        }

        Map<String, String> headers = config.getHeaders();
        if (headers != null && headers.size() > 0) {

            for (String key : headers.keySet()) {
                String value = headers.get(key);
                httpRequest.header(key, value);
                log.info("Add Header name={},value={}", key, value);
            }
        }

        if (StringUtils.isNotBlank(config.getContentType())) {

            if (config.getContentType().equals("form-data")) {
                Map<String, Object> formData = config.getFormData();
                if (formData != null && formData.size() > 0) {

                    httpRequest.form(formData);
                    log.info("Add frorm-data={}", formData.toString());
                }
            }

            if (config.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)) {
                if (StringUtils.isNotBlank(config.getBody())) {

                    httpRequest.body(config.getBody());
                    log.info("Add body={}", config.getBody());
                }

                httpRequest.contentType(config.getContentType());
            }

        }


        HttpResponse httpResponse = httpRequest.execute();

        HttpResponsePlus httpResponsePlus = new HttpResponsePlus();
        BeanUtils.copyProperties(httpResponse, httpResponsePlus);

        httpResponsePlus.setBody(httpResponse.body());

        if (StringUtils.isNotBlank(config.getRespFormat())) {
            if ("sys".equals(config.getRespFormat())) {

                return Response.success(httpResponsePlus);
            }

            if ("api".equals(config.getRespFormat())) {

                return httpResponse.body();
            }

            if ("http".equals(config.getRespFormat())) {

                return httpResponsePlus;
            }
        }


        return Response.success(httpResponsePlus);
    }
}
