package com.wsc.community.config;

import com.wsc.community.util.CommunityConstant;
import com.wsc.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * springsecuity配置类
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                    "/user/setting",
                    "/user/upload",
                    "/discuss/add",
                    "/comment/add/**",
                    "/letter/**",
                    "/notice/**",
                    "/like",
                    "/follow",
                    "/unfollow"
                ).hasAnyAuthority(
                     AUTHORITY_ADMIN,
                    AUTHORITY_MODERATOR,
                    AUTHORITY_USER
                ).
                antMatchers("/discuss/top","/discuss/wonderful").hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/detail","/data/**").hasAnyAuthority(AUTHORITY_ADMIN).anyRequest().permitAll()
                .and().csrf().disable();
        //权限不够的时候的处理
        http.exceptionHandling()
                //没有登录
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedwith=request.getHeader("x-requested_with");
                        //异步请求
                        if("XMLHttpRequest".equals(xRequestedwith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJsonString(403,"你还没有登录"));
                        }else{
                            response.sendRedirect(request.getContextPath()+"/login");
                        }
                    }
                })//权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedwith=request.getHeader("x-requested_with");
                        //异步请求
                        if("XMLHttpRequest".equals(xRequestedwith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJsonString(403,"你没有访问此功能的权限"));
                        }else{
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });
        //security的底层默认为拦截/logout,进行退出处理
        //覆盖它默认的逻辑才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");
    }
}
