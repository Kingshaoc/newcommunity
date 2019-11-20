package com.wsc.community;

import com.wsc.community.dao.DiscussPostMapper;
import com.wsc.community.dao.elasticsearch.DiscussPostRepository;
import com.wsc.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommuntiyApplication.class)
public class ElasticSearchTest {


    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    //往ES里面添加数据
    @Test
    public void testInsert(){
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    //插入多条数据
    @Test
    public void testInsertList(){
        //discussRepository.saveAll(discussMapper.selectDiscussPost(101,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(102,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(103,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(111,0,100));
       // discussRepository.saveAll(discussMapper.selectDiscussPost(112,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(131,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(132,0,100));
        //discussRepository.saveAll(discussMapper.selectDiscussPost(133,0,100));
       // discussRepository.saveAll(discussMapper.selectDiscussPost(134,0,100));
    }

    //测试数据的删除
    @Test
    public void testDelete(){
        discussRepository.delete(discussMapper.selectDiscussPostById(231));
    }

    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // 底层获取得到了高亮显示的值, 但是没有返回.

        Page<DiscussPost> page = discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }


}
