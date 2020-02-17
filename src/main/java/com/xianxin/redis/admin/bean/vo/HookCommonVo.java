package com.xianxin.redis.admin.bean.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author 贤心i
 * @email 1138645967@qq.com
 * @date 2020/02/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HookCommonVo extends BaseVo {

    private List<String> patterns;
}
