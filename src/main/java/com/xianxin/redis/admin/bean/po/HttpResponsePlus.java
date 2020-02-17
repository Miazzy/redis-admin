package com.xianxin.redis.admin.bean.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.HttpCookie;
import java.util.List;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HttpResponsePlus {

    private String body;

    private int status;

    private List<HttpCookie> cookies;

    private boolean gzip;

    private boolean deflate;

    private boolean chunked;

    private String cookieStr;

    private String ok;

}
