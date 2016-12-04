package HTTP;

/**
 * Created by Administrator on 2016/12/4.
 */
public enum Browser {
    CHROME;
    public static String getBrowserType(Browser browser) {
        switch (browser) {
            case CHROME: return "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36\r\n";
            default: return "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36\r\n";
        }
    }
}
