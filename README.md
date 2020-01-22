# newcommunity
A SpringBoot project based on Redis, Kafka,Elasticsearch  from nowcoder.


牛客讨论区项目总结
本项目是基于牛客高级课的关于牛客讨论区的项目，项目的技术栈以及框架都是比较新的，是一个基于SpringBoot的Java Web项目，框架使用了SSM，数据库采用了Mysql和Redis，使用Kafka作为消息队列，以及Elastic Search作为搜索引擎。

## 项目准备
准备工作就是一些工作环境的配置，IDE使用IntellijIDEA，具体的框架的版本与配置，以及mysql，redis，kafka，es的安装与配置，视频里都会有详细的介绍，kafka，es在mac系统上的安装我也有写博客介绍，在此就不必再做详细的说明。下面介绍一些关于此项目的主要模块后端代码的实现部分。
    
## 注册与登录功能的实现

注册是登录功能是每个网站最基本的功能，实现的主要难点我觉得在于怎么解决分布式Session问题，密码安全问题，以及怎么优化登录的问题。

用户表实现

| id | username | password | salt  | email  | type | status | activation_code | header_url | create_time |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |


### 密码MD5加密
为了保证安全，密码不能明文的在网络中进行传输，也不能以明文的形式存到数据库中。
存在数据库的密码 = MD5( 密码 + salt ) 防止密码泄露

### 会话管理（分布式Session问题）
由于现在网站基本是多台服务器分布式部署的，如果将用于信息存到session中，而session是存到服务器上，在分布式环境下，由于各个服务器主机之间的信息并不共享，将用户信息存到服务器1上，同一个用户的下一个请求过来的时候，由于nginx的负载均衡策略，去请求了服务器2，就找不到之前的session了。下面介绍几种分布式Session问题的解决策略。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150313188.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)

* 粘性session：同一个ip分给同一个服务器，很难做负载均衡

* 同步Session：当一个服务器创建了session之后，会将该Session同步给其他
服务器。服务器之间耦合，加大服务器之间的同步开销

* Session服务器：专门一个服务器管理Session，这台服务器是单体的，万一挂掉，有安全隐患
* 将客户端会话数据不存到Session中而是存到数据库中：
关系型数据库性能较慢
存到redis中（项目中采用的方式）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150324266.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)

### Kaptcha生成验证码

### Loginticket生成凭证 记录登录状态
本项目中先采用将用户登录信息存到数据库的login_ticket表中，后续采用存到redis中优化。
#### V1 将用户登录凭证ticket存到mysql的login_ticket表中
由于Http是无状态的，为了保证用户每次请求不用重新输入账号密码，

登陆成功的时候生成登录凭证，生成Loginticket往数据库login_ticket存，并且被设置为cookie，下次用户登录的时候会带上这个ticket，ticket是个随机的UUID字符串，有过期的时间expired和有效的状态status。

用login_ticket存储用户的登录信息，每次请求会随着cookie带到服务端，服务端只要与数据库比对携带的ticket，就可以通过表中的used_id字段查到用户的信息。
用户退出时将status更改为0即可。


| id | user_id | ticket | status | expired |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150351451.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)

#### V2：  使用Redis优化登录模块
* 使用Redis存储验证码
    1. 验证码需要频繁的访问与刷新，对性能要求比较高
    2. 验证码不需要永久保存，通常在很短的时间后就会失效（redis设置失效时间）
    3. 分布式部署的时候，存在Session共享的问题（之前验证码是存到session里面，使用redis避免session共享问题）
         
     | Key | Value |
    | --- | --- |
    |kaptcha:owner |string |

owner : 由于此时用户还未登录，owner为临时生成的凭证,存到cookie中发送给客户端。
登录的时候从cookie中取值构造redisKey,再从redis中取值。并与用户输入的验证码进行比对。

* 使用Redis存储登录凭证，作废login_ticket
    * 处理每次请求的时候，都要从请求的cookie中取出登录凭证并与从数据库mysql中查询用户的登录凭证作比对，访问的频率非常高（原来登录凭证ticket是存到mysql里面，ticket如果用redis存，mysql就可以不用存了，login_ticket可以作废）  
    
     | Key | Value |
    | --- | --- |
    |ticket:ticket | Loginticket|
    
    第二个ticket为实际的ticket字符串，Loginticket被序列化为Json字符串。
    
    *   退出登录的是，需要让登录凭证失效，此时根据key将Loginticket取出来，在更改其状态为1失效态，再重新存回Redis中。 （不删除可以保留用户登录的记录）
    
* 使用Redis缓存用户信息

    * 处理每次请求的时候，都要根据登录凭证查询用户信息，访问的评率非常高（每次请求的时候需要根据凭证中的用户id查询用户）
        
    * 查询User的时候，先尝试从缓存中取值，如果没有的话，就需要初始化，有些地方会改变用户数据，需要更新缓存，可以直接把该用户的缓存删除，下一次请求的时候发现没有用户的信息，就会重新查一次再放到缓存中
    

### 拦截器的使用
声明拦截器（实现HandleInterceptor）并在spring中配置拦截信息

* 在请求开始时查询登录用户
* 在本次请求中持有用户数据 
    
使用拦截器Interceptor来拦截所有的用户请求，判断请求中的cookie是否存在有效的ticket，如果有的话就将查询用户信息并将用户的信息写入ThreadLocal在本次请求中持有用户，将每个线程的threadLocal都存在一个叫做hostHolder的实例中，根据这个实例就可以在本次请求中全局任意的位置获取用户信息。


## 发布帖子与敏感词过滤

使用AJAX异步发帖

    *     AJAX - Asychronous JavaScript and XML
    *     异步的JavaScript与XML, 不是一门新的技术，只是一门新的术语
    *     使用AJAX，网页能够将改变的量更新呈现在页面上，而不需要刷新整个页面
    *     虽然X代表XML,但是目前JSON的使用比XML更加普遍

发布帖子的时候需要对帖子的标题和内容进行敏感词，通过Trie实现敏感词过滤算法，过滤敏感词首先需要建立一颗字典树，并且读取一份保存敏感词的文本文件，并用文件初始化字典树，最后将敏感词作为一个服务，让需要过滤敏感词的服务进行调用即可。

具体实现见下：
[利用Trie(字典树)实现敏感词过滤算法](https://blog.csdn.net/weixin_41927235/article/details/102975797)

防止xss注入：防止xss注入直接使用HTMLUtils的方法即可实现。

## 发表评论以及私信
### 评论

评论的是每一个帖子都评论，并且支持对评论进行评论，也就是评论的回复，
能够显示评论的数量，具体的内容，以及评论人回复人等等。

**评论表的实现：**

| id | user_id | entity_type | entity_id | target_id | content | status | create_time |
| --- | --- | --- | --- | --- | --- | --- | --- |

其中

* Entity_type 评论的目标的类别 1：帖子 2: 评论   支持回复评论
* entity_id   评论具体的目标
* target_id   记录回复指向的人 (只会发生在回复中 判断target_id==0)
* user_id     评论的作者

添加评论时：(将添加评论和更新评论数量放在一个事务中)使用spring声明式事务管理@Transactional实现


### 私信
私信是支持两个用户之间发送消息，有一个唯一的会话id，以用户id小的开头，id大的结尾比如12_111，以varchar的形式存储到数据库中，这个会话里可以有多条两个用户交互的消息。

**私信表的实现**

| id | form_id | to_id | conversion_id | content  | status | create_time |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |


* form_id 112  其中id = 1代表的是：系统通知
* to_id 111
* conversion_id 111_112(id小的在前)111与112 之间的会话
* status 0：未读 1:已读 2：删除
 
 **复杂sql**
 这块的sql编写是相对比较复杂的，主要是在会话列表中显示每条会话中最新的消息内容。
 显示未读消息的数量。根据时间顺序排列会话
 
 查询当前用户的会话列表，针对每个会话只返回一条最新的私信 
    
```
select id, from_id, to_id, conversion_id, content, status, create_time from message
where id in(
    select max(id) from message 
    where status != 2 
    and from_id != 1 
    and (from_id = user_id or to_id = user_id)
    group by conversion_id
)
order by id desc
limit 0, 10

```
同样这里也采用异步请求的方式发送私信，发送成功后刷新私信列表

### Spring aop记录日志
aop实现对service层所有的业务方法记录日志

* Aop是一种编程思想，是对OOP的补充，可以进一步提升效率
* Aop解决纵向切面的问题，主要实现日志和权限控制的功能
* aspect实现切面，并且使用Logger来记录日志。用该切面的切面方法来监听controller
* 拦截器主要针对的是控制层controller


## 使用Redis实现点赞关注
点赞和关注从功能上来说用传统的关系型数据库实现，但是其关系性并非很强，而且也是非常频繁的操作，用简单快速的Nosql也可以实现。

### 点赞功能

* 支持对帖子、评论点赞
* 第一次点赞，第2次取消点赞(判断userId在不在set集合中，就可以判断用户有否点过赞，如果已经点过赞了，就将用户从集合中删除)
* 在查询某人对某实体的点在状态时，用可以用boolean作为返回值，但项目中使用int（支持业务扩展，可以支持是否点踩）

| Key | Value |
| --- | --- |
| like:entity:entityType:entityId  | set(userId) |


**value使用set集合存放userId是为了能看对谁点了赞。**

### 我收到的赞：

点赞时同样需要记录点赞实体的用户id

某个用户收到的赞

| Key | Value |
| --- | --- |
| like:user:userId  | int |

### 关注、取消关注功能
使用Redis实现了每一个用户的粉丝列表，以及每一个用户的关注列表。

**由于关注成功以及添加粉丝成功需要在同一个事务中，Redis的事务主要是由multi与exec两个命令实现。**

关键点：

* A关注了B，则A是B的Follwer，B是A的Followee(目标）
* 关注的目标，可是用户、帖子、题目，在是现实将这些目标抽象为实体

**某个用户关注的实体**

| Key | Value |
| --- | --- |
|followee:userId:entityType  |zset(entityId,now) |
        
使用zset以当前时间作为分数排序

**某个实体拥有的粉丝**

| Key | Value |
| --- | --- |
|follower:entityType:entityId  |zset(userId,now) |

## Kafka，实现异步消息系统
在项目中，会有一些不需要实时执行但是是非常频繁的操作或者任务，为了提升网站的性能，可以使用异步消息的形式进行发送，再次消息队列服务器kafka来实现。kafka的具体配置与使用可参考下文：

[mac下消息队列服务器kafka（结合springboot）的配置与使用
](https://blog.csdn.net/weixin_41927235/article/details/103036077)

### 发送系统通知

评论，点赞，关注等事件是非常频繁的操作，发送关系其的系统通知却并不是需要立刻执行的。主要实现分为下面几步：

* 触发事件
    *    评论后，发布通知
    *    点赞后，发布通知
    *    关注后，发布通知
 
* 处理事件
    *    封装事件对象（Event）
        
    ```
    private String topic;
    private int userId;
    private int entityType;
    private int entityUserId;
    private Map<String,object> data;
    ```
    *    开发事件的生产者
        向特定的主题（评论，点赞，关注）发送事件
        
     ```java
      //处理事件(发送事件)
    public void fireEvent(Event event){
        //将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }  
     ```
        
    *    开发事件的消费者
      使用@KafkaListener注解监听事件，如果监听成果并进行相应的处理，最后调用messageService添加到数据库中，下次用户显示消息列表的时候就可以看到系统消息了。
        
```java
 @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
       
        //发送站内的通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());//comment like follow
        message.setCreateTime(new Date());
              
    message.setContent(JSONObject.toJSONString(content));

        System.out.println(content);
        //调用messageService添加到数据库中
        messageService.addMessage(message);
    }
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150436541.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)
 
## Elastic Search 实现网站的搜索功能
项目中的搜索服务采用的是ES来实现，有关es的介绍与使用可以点击下文。
[mac下Elasticsearch的安装与简单使用，结合命令行和postman](https://blog.csdn.net/weixin_41927235/article/details/103050095)
[SpringBoot整合Elasticsearch配置与使用](https://blog.csdn.net/weixin_41927235/article/details/103051052)


* 搜索服务
    *    将帖子保存到Elasticsearch服务器
    *    从 Elasticsearch 服务器中删除帖子
    *    从 Elasticsearch 服务器搜索帖子
* 发布事件（将发帖或者更改帖子的事件存到kafka中，消费事件并将帖子存到es服务器中）
    *    发布帖子时，将帖子异步的提交到Elasticsearch服务器
    *   增加评论的时候，将帖子异步的提交到Elasticsearch服务
    *    在kafka消费组件中增加一个方法，消费帖子发布事件
* 显示结果
    *    在控制器中处理搜索请求，在HTML高亮显示搜索结果
   

##Redis高级数据类型，实现网站数据统计
项目中的数据统计功能如独立访客，以及日活跃用户，需要使用redis的高级数据类型超级日志以及位图来实现。
### 高级数据类型
* HyperLogLog（超级日志）
    * 采用一种基数算法，用于完成独立总数（独立访客）的统计
    * 占据空间小，我无论统计多少个数据，只占有12k的内存空间
    * 不精确的统计算法，标准误差为0.81%
* Bitmap（位图）
    * 不是一种独立的数据结构，实际上就是字符串
    * 支持按位存取数据，可以将其看成是byte数据
    * 适合存储所大量的连续的数据的布尔值（签到）
    
### 网站数据统计
* UV(Unique Vistior)
    * 独立访客，需要通过用户IP排重统计数据（可以统计游客，在拦截器中实现）
    * 每次访问都要进行统计
    * HyperLogLog,性能好，且存储空间小
        单日uv
        
     | Key | Value |
    | --- | --- |
    | uv:date |ip | 
    区间UV
    
     | Key | Value |
     | --- | --- |
     | uv:startData:endData |ip |
    
    
* DAU(Daily active User)
    * 日活跃用户，需要通过用户ID排重统计数据（排除游客，需要精确）
    * 访问一次，则认为其活跃
    * Bitmap，性能好，且可以统计精确的结果
    单日dau（在userId的比特位上标记为true）
    
     | Key | Value |
    | --- | --- |
    |dau:date |userId,true |
    区间DAU(在区间内，任何一天登录都算活跃，对区间内日期做异或运算)
    
     | Key | Value |
    | --- | --- |
    |dau:startDate:endDate |userId,true |


## Spring Quartz实现定时热帖排行
由于热帖排行功能的实现是需要定时实现的，即每隔段时间就要从数据库中查询最热门的帖子显示，所以可以使用定时任务的形式来实现，JDK自带的ScheduledExecutorService以及Spring自带的ThreadPoolTaskScheduler都可以实现定时任务的功能，但是其在分布式的环境下会出现问题，Scheduler是基于内存，服务器1和服务器2的上的Scheduler代码是相同的，会定时的做同样的事情。本项目采用的是Quartz，因为quartz实现定时任务的参数是存到数据库中的，所以不会出现重复代码的问题。
### Quartz

*   JDK线程池
    *   ExecutorService
    *   ScheduledExecutorService   
*   Spring线程池
    *   ThreadPoolTaskExecutor
    *   ThreadPoolTaskScheduler
   
*    分布式定时任务
   	 *   Spring Quartz
  
 Job:定义任务
 JobDetail、trigger :任务的配置类，一旦任务初始化完成就会存到数据库的表中qrtz_triggers，只在第一次时有用
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150529167.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)

###     热帖排行

帖子排行的计算公式：
**log（精华分 + 评论数 *10 + 点赞数 *2 + 收藏数 * 2）+ （发布时间 - 纪元）** 

将分数发生变换的帖子丢到Redis缓存中去，每10分钟计算一下帖子的分数。

 
| key | value |
| --- | --- |
| post:score | set(存帖子id） |


## 使用本地缓存Caffeine优化网站性能
由于热门帖子数量不大，直接存储在服务器本地上并不会给服务器带来过重的存储负担，而且热门帖子的改变并不是很频繁，适合缓存存储，并且热门帖子不涉及用户信息，不会存在服务器之间的共享问题，综上比较适合使用本地缓存存储。

**项目中使用了本地缓存Caffeine优化热门帖子**

* 本地缓存
    *     将数据缓存在应用服务器上，性能最好
    *     常用缓存工具:Ehcache、Guava、Caffeine
* 分布式缓存
    *     将数据缓存在NoSQL数据库上，跨服务器（登录凭证不适合直接存到服务器上
    *     常用缓存工具:Redis
* 多级缓存
    *   一级缓存（本地缓存）->二级缓存（分布式缓存）->DB
    *    避免缓存雪崩（缓存失效，大量请求直达DB），提高系统的可用性

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150603146.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTkyNzIzNQ==,size_16,color_FFFFFF,t_70)



| Key | Value |
| --- | --- |
|offset:limit |list(热门帖子) |

offset以及limit代表的是起始行和终止行，就是某一页的热门帖子。

###    JMeter压力测试
使用本地缓存优化后，用JMeter做一下压力测试
    使用JMeter创建了100个线程请求首页热门帖子
优化前：qps稳定在9.5左右：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150620506.png)
启用缓存优化后：qps达到了189左右，可以看到性能有明显的提升
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200122150727465.png)



## 结语

以上只是对本项目的相对比较重要的功能进行了总结，当然了这个项目还有一些其他的功能，比如SpringSecurity实现权限控制,文件上传到七牛云服务器，spring统一处理异常之类的。感兴趣的同学可以参考视频以及我提供的代码敲一敲。




