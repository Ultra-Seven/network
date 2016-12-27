

import HTTP.HttpClient;
import URL.URLParser;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by Administrator on 2016/12/11.
 */
public class LabGet {
    public static void main(String[] args) {
        if (args.length == 1) {
            URLParser urlParser = new URLParser(args[0]);
            HttpClient client = null;
            try {
                client = new HttpClient(urlParser.getUrl());
                client.sendRequestGet("");

//                System.out.println(Arrays.toString(client.getReadByte()));
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                if (urlParser.getUrl().getIps().size() > 0)
                    urlParser.getUrl().getIps().forEach(System.err::println);
                if (client!= null && client.getResponse() != null)
                    System.err.println(client.getResponse().getHeader());
            }
        }
    }
}
