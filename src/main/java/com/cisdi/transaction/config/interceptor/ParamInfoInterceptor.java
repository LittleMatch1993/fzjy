/*
package com.cisdi.transaction.config.interceptor;

import com.cisdi.transaction.util.ThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

*/
/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/13 16:37
 *//*

@Component
public class ParamInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String orgCode = request.getHeader("orgCode");

        System.out.println(">>>>>>>拦截到api相关请求头<<<<<<<<"+orgCode);

        if(StringUtils.isNotEmpty(orgCode)){

            //直接搂下来，放到ThreadLocal中 后续直接从中获取

            ThreadLocalUtils.set("orgCode",orgCode);

        }

        return true;//注意 这里必须是true否则请求将就此终止。

    }


    @Override

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除app-user
        ThreadLocalUtils.remove();
    }
}
*/
