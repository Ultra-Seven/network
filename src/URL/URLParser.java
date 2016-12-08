package URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/12/1.
 */
public class URLParser {
    private URL url;
    private String rawUrl;
    //constructor
    public URLParser(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url or charset is null");
        }
        this.url = new URL(url);
        this.rawUrl = url;

        //set url protocol
        this.url.setProtocol(getProtocol());
        //System.out.println("Protocol:" + this.url.getProtocol());
        //set host name
        String host = getHostName();
        if (host == null)
            host = getIP();
        this.url.setHost(host);
        //System.out.println("Host:" + this.url.getHost());
        //set port
        this.url.setPort(getPort());
        //System.out.println("Port:" + this.url.getPort());
        //set path
        this.url.setPath(getPath());
        //System.out.println("Path:" + this.url.getPath());
        //set parameters
        this.url.setParameters(getParameter());
        this.url.getParameters().entrySet().forEach(stringListEntry -> {
            System.out.println("Key:" + stringListEntry.getKey() + "\t\tValue:" + stringListEntry.getValue().get(0));
        });
        //set fragment
        this.url.setFragment(getFragment());
        //System.out.println("Fragment:" + this.url.getFragment());
    }

    private String getProtocol() {
        return getMatchedString("(?:(ssh|ftp|https?)://)");
    }
    private String getHostName() {
        return getMatchedString("(?:[a-z0-9](?:[-a-z0-9]*[a-z0-9])?\\.)+(?:com|edu|gov|net|org|biz|in(?:t|fo)|(?:[a-z]{2}))");
    }
    private String getIP() {
        return getMatchedString("^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})){3}$");
    }
    private int getPort() {
        String port = getMatchedString("(?::(\\d{1,5}))");
        if (port != null) {
            port = port.substring(1);
            return Integer.parseInt(port);
        }
        else
            return 80;
    }
    private String getPath() {
        String path = getMatchedString("(/(\\w+(-\\w+)*))*(\\.?(\\w)*)");
        if (path.equals("")) {
            path = "/";
        }
        return path;
    }
    private HashMap<String, List<String>> getParameter() {
        HashMap<String, List<String>> list = new HashMap<>();
        if (rawUrl.length() > 0 && rawUrl.charAt(0) == '?') {
            rawUrl = rawUrl.substring(1);
            Pattern pattern = Pattern.compile("(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)");
            Matcher matcher = pattern.matcher(rawUrl);
            if (matcher.find()) {
                String match = matcher.group();
                String[] query = match.split("&");
                for (String aQuery : query) {
                    String[] parameters = aQuery.split("=");
                    List<String> value = new ArrayList<>();
                    value.addAll(Arrays.asList(parameters).subList(1, parameters.length));
                    list.putIfAbsent(parameters[0], value);
                }
                rawUrl = rawUrl.replace(match, "");
            }
        }
        return list;
    }
    private String getFragment() {
        if (rawUrl.length() > 0 && rawUrl.charAt(0) == '#') {
            rawUrl = rawUrl.substring(1);
            return rawUrl;
        }
        return null;
    }
    private String getMatchedString(String regx) {
        String match = null;
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(rawUrl);
        if (matcher.find()) {
            match = matcher.group();
            rawUrl = rawUrl.replace(match, "");
        }
        return match;
    }
    private boolean isValid() {
        return true;
    }

    public URL getUrl() {
        return this.url;
    }
}
