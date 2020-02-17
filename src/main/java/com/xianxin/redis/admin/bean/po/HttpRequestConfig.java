package com.xianxin.redis.admin.bean.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HttpRequestConfig implements Serializable {

    /**
     * 唯一ID
     */
    private String id;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 请求地址
     */
    @NotBlank(message = "请求地址不能为空")
    private String url;

    /**
     * 请求方法
     */
    @NotBlank(message = "请求方法不能为空")
    private String method;

    @NotBlank(message = "Content-Type不能为空")
    private String contentType;

    /**
     * 请求headers
     */
    private Map<String, String> headers;

    /**
     * 表单数据
     */
    private Map<String, Object> formData;

    /**
     * json数据
     */
    private String body;

    /**
     * 字符集
     */
    private String charset = "UTF-8";

    /**
     * request=请求 , callback=回调
     */
    @NotBlank(message = "HTTP类型不能为空")
    private String httpType = "request";

    /**
     * 任务级别 按数字大小 从小到大依次执行
     */
    private int level;

    /**
     * 是否处理结果
     */
    private boolean isDispose = false;

    /**
     * 响应体格式 sys=系统默认、api=接口返回、http=Http全部响应体
     */
    private String respFormat = "sys";

    /**
     * string=字符串 、object=对象 、json=JSON格式Body
     */
    private String respDataType = "string";
}
