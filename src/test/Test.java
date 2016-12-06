package test;

import DNS.DNSQuery;
import HTTP.HttpClient;
import URL.URLParser;

import java.io.IOException;

/**
 * Created by Administrator on 2016/12/2.
 */
public class Test {
    public static void main(String[] args) {
        try {
            //testURL();
            //testDNSQuery();
            testHttp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void testURL() {
        URLParser urlParser = new URLParser("http://www.google.cn/gwt/x?u=http%3A%2F%2Fanotherbug.blog.chinajavaworld.com%2Fentry%2F4550%2F0%2F&btnGo=Go&source=wax&ie=UTF-8&oe=UTF-8#page-16");
    }
    private static void testDNSQuery() throws IOException {
        DNSQuery dnsQuery = new DNSQuery("www.baidu.com", "202.120.224.26");
        dnsQuery.printAnswer();
    }
    private static void testHttp() throws IOException {
        URLParser urlParser = new URLParser("berkeley.edu");
        HttpClient httpClient = new HttpClient(urlParser.getUrl());
    }
}
