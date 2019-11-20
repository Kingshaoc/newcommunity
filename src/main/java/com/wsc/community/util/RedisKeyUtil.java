package com.wsc.community.util;

import com.wsc.community.entity.LoginTicket;

public class RedisKeyUtil {
    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user";

    private static final String PREFIX_FOLLOWEE="followee";
    private static final String PREFIX_FOLLOWER="follower";
    private static final String PREFIX_KAPTCHA="kaptcha";
    private static final String PREFIX_TICKET="ticket";
    private static final String PREFIX_USER="user";

    private static final String PREFIX_UV="uv";
    private static final String PREFIX_DAU="dau";

    private static final String PREFIX_POST="post";


    //某个实体（帖子，回复，评论）的赞 值是一个set 里面存着点赞人的id
    //like:entity:entityType:entityId -> set(userId)
    //like:entity：1：101->set(userIds)
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
    }

    //某个用户的赞
    //like:user:userId->int
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE+SPLIT+userId;
    }

    //某个用户关注的实体（可以是人，帖子，话题）
    //followee：userId:entityType ->zset(userId,now)
    //followee：1:3 ->zset(2,now)
    public static String getFolloweeKey( int userId, int entityType){
        return PREFIX_FOLLOWEE+SPLIT+userId+SPLIT+entityType;
    }

    //某个（实体）用户的粉丝
    //follower:entityType:entityId ->zset(userId,now)
    public static String getFollowerKey(int entityType,int entityId){
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityId;
    }

    //登录验证码 kaptcha:aasd
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA+SPLIT+owner;
    }

    //登录的凭证 ticket:1323123
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET+SPLIT+ticket;
    }

    //用户 user:1
    public static String getUserKey(int userId){
        return PREFIX_USER+SPLIT+userId;
    }


    //单日uv
    public static String getUVkey(String date){
        return PREFIX_UV+SPLIT+date;
    }
    //区间uv
    public static String getUVkey(String startDate,String endDate){
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }

    //单日活跃用户
    public static String getDAUKey(String date) {
        return PREFIX_DAU+SPLIT+date;
    }

    //区间DAU
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU+SPLIT+startDate+SPLIT+endDate;
    }
    //帖子分数
    public static String getPostScoreKey(){
        return PREFIX_POST+SPLIT+"score";
    }


}
