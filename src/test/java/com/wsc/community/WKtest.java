package com.wsc.community;


import java.io.IOException;

public class WKtest {
    public static void main(String[] args) throws IOException {
        String cmd = "d:/work/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com d:/work/data/wk-images/3.png";
        Runtime.getRuntime().exec(cmd);
        System.out.println("ok");


    }

}
