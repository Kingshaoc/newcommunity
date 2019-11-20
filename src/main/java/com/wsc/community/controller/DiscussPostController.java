package com.wsc.community.controller;

import com.sun.org.apache.regexp.internal.RE;
import com.wsc.community.entity.*;
import com.wsc.community.event.EventProducer;
import com.wsc.community.service.CommentService;
import com.wsc.community.service.DiscussPostService;
import com.wsc.community.service.LikeService;
import com.wsc.community.service.UserService;
import com.wsc.community.util.CommunityConstant;
import com.wsc.community.util.CommunityUtil;
import com.wsc.community.util.HostHolder;
import com.wsc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody//返回json
    public String addDiscussPost(String title,String content){
        User user = hostHolder.getUser();
        if (user==null){
            //返回的是json
            return CommunityUtil.getJsonString(403,"你还没有登录哦");
        }
        DiscussPost post=new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //触发发帖事件
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(post.getId());
        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,post.getId());

        return CommunityUtil.getJsonString(0,"发布成功！");
    }




    @RequestMapping(path="/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId")int discussPostId, Model model, Page page){
       //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount",likeCount);
        //点赞状态
       int likeStatus= hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("likeStatus",likeStatus);

        //评论分页的信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论 帖子会有很多评论 每个评论又会有很多回复
        //回复：给评论的评论
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论vo列表viewobject视图对象
        List<Map<String,Object>> commentVoList=new ArrayList<>();
        if(commentList!=null){
            for(Comment comment:commentList){
                //评论vo
                Map<String,Object> commentVo=new HashMap<>();
                //评论
                commentVo.put("comment",comment);
                //作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));

                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus=hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //评论的回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                if(replyList!=null){
                    for(Comment reply:replyList){
                        Map<String,Object> replyVo=new HashMap<>();
                        replyVo.put("reply",reply);
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        User target=reply.getTargetId()==0?null:userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        //回复的点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);
                        //对某条回复的点赞状态
                        likeStatus=hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_COMMENT,reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
               commentVo.put("replys",replyVoList);
                //回复数量
                int replyCount=commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments",commentVoList);
        return "/site/discuss-detail";

    }

    //置顶
    @RequestMapping(path="/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);
        //同步到elasticSearch

        //触发发帖事件
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);

    }


    //加精
    @RequestMapping(path="/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);
        //同步到elasticSearch
        //触发发帖事件
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJsonString(0);

    }

    //删除
    @RequestMapping(path="/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateType(id,2);
        //同步到elasticSearch
        //触发删帖事件
        Event event=new Event().setTopic(TOPIC_DELETE).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);

    }
}
