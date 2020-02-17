package com.xianxin.redis.admin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class HttpUtilTest {

    public static void main(String[] args) {

        String url = "https://baike.baidu.com/item/钩子程序/10772325?fr=aladdin";

        // String html = HttpUtil.get(url);

        HttpRequest httpRequest = HttpUtil.createGet(url);

//        httpRequest.header("Cookie", "BIDUPSID=CD2F3C2718B8D9A68A4A8ABF33CC38F4; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; PSTM=1581643175; BAIDUID=5520AD0C9A4D8C1EE399BA319B954E69:FG=1; H_PS_PSSID=1460_21086; delPer=0; PSINO=6; BDSFRCVID=hF4sJeCCxG3ewlnusJ9Ps4Hyc-rBD9fOGIsd3J; H_BDCLCKID_SF=tR3f0Rrob6rDHJTg5DTjhPrMM-DJbMT-027OKKOHbbR_ftO_eq35jMk--gcH3f085eOxa4OethF0HPonHjKKjjQ-3J; session_id=1581863091514; session_name=sp0.baidu.com; Hm_lvt_55b574651fcae74b0a9f1cf9c8d7c93a=1581743915,1581863102; Hm_lpvt_55b574651fcae74b0a9f1cf9c8d7c93a=1581863102");
//        httpRequest.header("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36");


        HttpResponse httpResponse = httpRequest.execute();

        String html = httpResponse.body();

        System.out.println("结果：\n" + html);
    }
}
