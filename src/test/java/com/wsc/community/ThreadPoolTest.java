package com.wsc.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommuntiyApplication.class)
public class ThreadPoolTest {



    private static  final Logger logger= LoggerFactory.getLogger(ThreadPoolTest.class);

    private ExecutorService executorService= Executors.newFixedThreadPool(5);

    private ScheduledExecutorService scheduledExecutorService=Executors.newScheduledThreadPool(5);

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private void sleep(long m){
        try{
            Thread.sleep(m);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    @Test
    public void testExecutorService(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ExecutorService");
            }
        };
        for(int i=0;i<10;i++){
            executorService.submit(task);
        }
        sleep(10000);
    }

    @Test
    public void testScheduledExecutorService(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                logger.debug("hello ExecutorService");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task,10000,1000, TimeUnit.MILLISECONDS);
        sleep(10000);
    }

    @Test
    public void testSpringExecutor(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                logger.debug("hello SpringExecutor");
            }

        };
        for(int i=0;i<10;i++){
            taskExecutor.submit(task);
        }
        sleep(10000);
    }

    @Test
    public void testSpringScheduledExecutor(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                logger.debug("hello testSpringScheduledExecutor");
            }
        };
        Date startTime=new Date(System.currentTimeMillis()+10000);
        taskScheduler.scheduleAtFixedRate(task,startTime,10000);
        sleep(10000);
    }




}
