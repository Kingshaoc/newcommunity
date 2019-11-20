package com.wsc.community.controller;

import com.wsc.community.entity.Comment;
import com.wsc.community.entity.DiscussPost;
import com.wsc.community.entity.Event;
import com.wsc.community.event.EventProducer;
import com.wsc.community.service.CommentService;
import com.wsc.community.service.DiscussPostService;
import com.wsc.community.util.CommunityConstant;
import com.wsc.community.util.HostHolder;
import com.wsc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private RedisTemplate redisTemplate;


    //添加评论
    @RequestMapping(value = "/add/{discussPostId}" ,method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论的事件，加入到kafka消息队列，告诉用户谁评论了你的哪个帖子
        Event event=new Event().setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);//帖子的id号 用于跳转到该帖子

        //如果评论的是帖子
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());

        }else if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            //如果评论的是评论
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        //触发发帖事件
       if(comment.getEntityType()==ENTITY_TYPE_POST){
            event=new Event().setTopic(TOPIC_PUBLISH).setUserId(comment.getUserId())
                   .setEntityType(ENTITY_TYPE_POST).setEntityId(discussPostId);
           eventProducer.fireEvent(event);
           //计算帖子的分数
           String redisKey= RedisKeyUtil.getPostScoreKey();
           redisTemplate.opsForSet().add(redisKey,discussPostId);
       }
        return "redirect:/discuss/detail/"+discussPostId;
    }



}
