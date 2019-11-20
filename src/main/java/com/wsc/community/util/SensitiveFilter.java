package com.wsc.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //定义前缀树
    private class TrieNode {
        //表示是否为关键词的结尾
        private boolean isKeyWordEnd = false;

        //子节点
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        //判断是否是关键词的结尾
        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        //设置关键词的结尾为true
        public void setKeyWordEnd(boolean isKeyWordEnd) {
            this.isKeyWordEnd = isKeyWordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }


    }

    //替换符号
    private static final String REPLACEMENT = "***";

    //根节点
    private TrieNode rootNode = new TrieNode();


    //初始化前缀树
    @PostConstruct
    public void init() {
        //读取配置文件
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                this.addKeyWord(keyword);
            }

        } catch (IOException e) {
            logger.error("加载敏感词的配置文件出错" + e.getMessage());
        }
    }


    //将一个敏感词添加到前缀树中
    private void addKeyWord(String keyword) {
        TrieNode temp = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = temp.getSubNode(c);
            //如果子节点中不存在该字符节点
            if (subNode == null) {
                subNode = new TrieNode();
                temp.addSubNode(c, subNode);
            }
            //指向下一个节点，进行下一个循环
            temp = subNode;
            //如果遍历到字符结束的位置，就要设置节点的字符结束标志位为true
            if (i == keyword.length() - 1) {
                temp.setKeyWordEnd(true);
            }
        }
    }

    //敏感词过滤算法的实现
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针1
        TrieNode tempNode=rootNode;
        //指针2 指向字符的开头
        int begin=0;
        //指针3 指向疑似敏感字符的结尾
        int position=0;
        //结果
        StringBuilder sb=new StringBuilder();

        while(begin<text.length()) {
            char c = text.charAt(position);
            //跳过符号
            if (isSymbol(c)) {
                //如果指针1处于根节点，将此符号计入结果，让指针2向下走一步
                if (tempNode==rootNode) {
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头还是中间，指针3都向下走一步
                position++;
                continue;
            }
            //检查下级节点
            tempNode=tempNode.getSubNode(c);
            if (tempNode==null){
                //以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                //进入下一个位置
                position=++begin;
                //重新指向根节点
                tempNode=rootNode;
            }else if (tempNode.isKeyWordEnd()){
                //发现敏感词，将begin-position字符串替换点
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin=++position;
                //重新指向根节点
                tempNode=rootNode;
            }else{
                //检查下一个字符
                if (position<text.length()-1){
                    position++;
                }

            }

        }
        sb.append(text.substring(begin));
        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c)&&(c<0x2E80||c>0x9FFF);
    }

}
