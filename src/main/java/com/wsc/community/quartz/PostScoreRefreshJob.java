package com.wsc.community.quartz;

import com.wsc.community.entity.DiscussPost;
import com.wsc.community.service.DiscussPostService;
import com.wsc.community.service.ElasticsearchService;
import com.wsc.community.service.LikeService;
import com.wsc.community.util.CommunityConstant;
import com.wsc.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {
    private Logger logger= LoggerFactory.getLogger(PostScoreRefreshJob.class);
    private static final Date epoch;

    static{
        try {
            epoch=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败",e);
        }
    }
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey= RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations=redisTemplate.boundSetOps(redisKey);
        if (operations.size()==0){
            logger.info("任务取消，没有需要刷新的贴子");
            return;
        }
        logger.info("[任务开始]正在刷新帖子分数"+operations.size());
        while(operations.size()>0){

            this.refresh((Integer)operations.pop());
        }
        logger.info("[任务结束]，帖子分数刷新完毕");

    }
    private  void  refresh(int postId){
        DiscussPost post=discussPostService.findDiscussPostById(postId);
        if (post==null){
            logger.error("该贴子不存在"+postId);
        }
        boolean wonderful=post.getStatus()==1;
        int commentCount=post.getCommentCount();
        long likeCount=likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId);

        //计算权重的值
        double w=(wonderful?75:0)+commentCount*10+likeCount*2;
        //分数 权重+天数
        double score=Math.log10(Math.max(w,1))+((post.getCreateTime().getTime()-epoch.getTime())/(1000*3600*24));
        //更新帖子分数
        discussPostService.updateScore(postId,score);
        //同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
