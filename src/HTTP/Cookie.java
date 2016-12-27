package HTTP;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Administrator on 2016/12/5.
 */
public class Cookie extends TimerTask implements Serializable {
    private static final String[] DEFAULT_PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM d HH:mm:ss yyyy",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd-MMM-yyyy HH:mm:ss Z"
    };
    private Map<String, String> cookiePairs;
    private String session;
    private String sessionId;
    private Date expires;
    private String host;
    public Cookie() {
        cookiePairs = new HashMap<>();
    }
    static Cookie getParseCookie(String cookieValue) throws ParseException {
        Cookie cookie = new Cookie();
        String[] subValue = cookieValue.split("; ");
        int index = 0;
        for (String aSubValue : subValue) {
            String[] sub = aSubValue.split("=");
            String key = sub[0];
            String value = sub.length > 1 ? sub[1] : "NIL";
            if (index++ == 0 && sub.length > 1) {
                cookie.session = sub[0];
                cookie.sessionId = sub[1];

            }
            cookie.cookiePairs.put(key, value);
        }
        String expireTime = cookie.cookiePairs.get("expires");
        if(expireTime != null) {
            for(String pattern : DEFAULT_PATTERNS) {
                SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
                try {
                    cookie.expires = fmt.parse(expireTime);
                }catch (ParseException ignored) {

                }

            }
        }
        return cookie;
    }
    @Override
    public void run() {
        CookieManager.unregisterCookie(this);
    }
    public String getValue(String key) {
        return cookiePairs.get(key);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, String> getCookiePairs() {
        return cookiePairs;
    }

    public Date getExpires() {
        return expires;
    }
}
