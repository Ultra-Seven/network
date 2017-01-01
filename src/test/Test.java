package test;

import DNS.DNSQuery;
import HTTP.HttpClient;
import URL.TLS.TLSSocket;
import URL.URLParser;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * Created by Administrator on 2016/12/2.
 */
public class Test {
    public static void main(String[] args) {
        try {
            //testURL();
            //testDNSQuery();
            //testHttp();
            testTLS(new String[]{"mirrors.tuna.tsinghua.edu.cn", "443", "/"});
            //testGzip();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    private static void testURL() {
        URLParser urlParser = new URLParser("http://pj-test.htcnet.moe:8033/test_url_parsing?data1=}don'tforgeturlencode{&data2=345%");
    }
    private static void testDNSQuery() throws IOException {
        DNSQuery dnsQuery = new DNSQuery("pj-test.htcnet.moe", "202.120.224.26");
        dnsQuery.printAnswer();
    }
    private static void testHttp() throws IOException, NoSuchAlgorithmException {
        URLParser urlParser = new URLParser("http://www.berkeley.edu/");
        HttpClient httpClient = new HttpClient(urlParser.getUrl());
    }
    private static void testGzip() throws IOException {
        byte[] bytes = new byte[100];
        File file = new File("bytes");
        InputStream inputStream = new FileInputStream(file);
        InputStream in = new ByteArrayInputStream(bytes);
        in = new GZIPInputStream(in);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int size;
        byte[] buf = new byte[1024];
        while((size = in.read(buf)) > 0) {
            outputStream.write(buf, 0, size);
        }
        in.close();
        System.out.println(Arrays.toString(outputStream.toByteArray()));
    }
    private static void testTLS(String[] args) throws NoSuchAlgorithmException, IOException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String file = args[2];

        TLSSocket tls = null;
        if (args.length == 3) {
            tls = new TLSSocket(host, port);
        }
        String out = "GET /" + file  + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Connection: Keep-Alive\r\n"
                + "\r\n";
        if (tls != null) {
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
}
