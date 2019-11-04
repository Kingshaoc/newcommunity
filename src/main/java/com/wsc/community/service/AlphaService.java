package com.wsc.community.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//@Scope("prototype")
public class AlphaService {
    public AlphaService() {
        //System.out.println("实例AlphaService");

    }

    @PostConstruct//在构造器之后
    public void init() {

        //System.out.println("初始化AlphaService");
    }

    @PreDestroy
    public void destory() {
       // System.out.println("销毁AlphaService");

    }
}
