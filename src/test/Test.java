package test;

import DNS.DNSQuery;
import HTTP.HttpClient;
import URL.TLS.TLSSocket;
import URL.URLParser;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2016/12/2.
 */
public class Test {
    public static void main(String[] args) {
        try {
            //testURL();
            //testDNSQuery();
            //testHttp();
            testTLS(new String[]{"baidu.com", "80", "/"});
        } catch (IOException | NoSuchAlgorithmException e) {
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
        URLParser urlParser = new URLParser("http://www.berkeley.edu/");
        HttpClient httpClient = new HttpClient(urlParser.getUrl());
    }
    private static void testTLS(String[] args) throws NoSuchAlgorithmException, IOException {
        if (args.length < 3) {
            System.out.println("usage: TLSSocket <host> <port> <file> [proxyHost] [proxyPort]");
            System.exit(0);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String file = args[2];

        TLSSocket tls = null;
        if (args.length == 3) {
            tls = new TLSSocket(host, port);
        }

        String out = "GET /" + file  + " HTTP/1.1\r\n"
                + "User-Agent: TLSSocket Test\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Connection: Keep-Alive\r\n"
                + "\r\n";

        tls.getTlsOutputStream().write(out.getBytes());
        byte[] buf = new byte[4096];
        while (true) {
            int count = tls.getTlsInputStream().read(buf);
            if (count < 0) {
                break;
            }
            System.out.print(new String(buf, 0, count));
        }
    }
}
