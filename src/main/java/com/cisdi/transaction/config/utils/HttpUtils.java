package com.cisdi.transaction.config.utils;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Base64;

/** 调用第三方接口工具
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/2 15:24
 */
public class HttpUtils {

    /**
     * 调用有认证的接口 忽略了安全证书
     * @param uri
     * @param obj
     * @return
     */
    public static String  sendPostOfAuth(String uri,JSONObject obj) {
        String rev = null;
        try {
            CertificateValidationIgnored ci = new CertificateValidationIgnored();
            HttpClient httpclient  = ci.wrapClient();
           // HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(uri);
            //添加http头信息
            httppost.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString(("PO_JSBQY" + ":" + "Jsbqy2022!").getBytes())); //认证token
            httppost.addHeader("Content-Type", "application/json");
           // httppost.addHeader("User-Agent", "imgfornote");
            /*JSONObject obj = new JSONObject();
            obj.put("app_id","test");
            obj.put( "table_name","");
            obj.put( "stringDate","18000101");
            obj.put( "condition","1=1");
            obj.put( "secretKey","195a101a2ceee131104928b440626785");*/
            httppost.setEntity(new StringEntity(obj.toString()));
            HttpResponse response;
            response = httpclient.execute(httppost);
            //检验状态码，如果成功接收数据
            int code = response.getStatusLine().getStatusCode();
            System.out.println(code+"code");

            if (code == 200) {
                 rev = EntityUtils.toString(response.getEntity());//返回json格式： {"id": "","name": ""}

            }
           // System.out.println(rev);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rev;
    }

    public static void main(String[] args) {
        String uri = "https://podev.minmetals.com.cn:50001/RESTAdapter/BS_OA/BS_MDM/GetAssHrTreeGxb";
        JSONObject obj = new JSONObject();
        obj.put("app_id","test");
        obj.put( "table_name","");
        obj.put( "stringDate","18000101");
        obj.put( "condition","1=1");
        obj.put( "secretKey","195a101a2ceee131104928b440626785");
        sendPostOfAuth(uri,obj);
    }
}

