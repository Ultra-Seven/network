package HTTP;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2016/12/5.
 */
public class CookieManager {
    private static Map<String, Map<String, Cookie>> cookies = new ConcurrentHashMap<>();
    private static Timer timer = new Timer(true);
    private static String cookiePath = "cookie" + File.separator + "cookie";
    private static CookieManager instance;
    private CookieManager() throws IOException, ClassNotFoundException {
        File file = new File(cookiePath);
        if (file.exists()) {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
            cookies = (Map<String, Map<String, Cookie>>) objectInputStream.readObject();
            //printCookies();
        }
    }
    public synchronized static void registerCookie(Cookie cookie) {
        Map<String, Cookie> cookieMap =cookies.get(cookie.getHost());
        if (cookieMap == null) {
            cookieMap = new HashMap<>();
        }
        cookies.put(cookie.getHost(), cookieMap);

        if (cookieMap.containsKey(cookie.getSession()))
            cookieMap.get(cookie.getSession()).cancel();
        cookieMap.put(cookie.getSession(), cookie);
        Date expire = cookie.getExpires();
        if (expire != null)
            timer.schedule(cookie, expire);
        try {
            saveCookie();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized static void saveCookie() throws IOException {
        File file = new File(cookiePath);
        if (!file.exists())
            file.createNewFile();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
        objectOutputStream.writeObject(cookies);
        objectOutputStream.close();
    }

    public static void unregisterCookie(Cookie cookie) {
        if (cookies.containsKey(cookie.getHost()))
            cookies.get(cookie.getHost()).remove(cookie.getSession());
    }

    public static CookieManager getInstance() {
        if (instance == null)
            try {
                instance = new CookieManager();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        return instance;
    }
    //print cookies to check correctness
    public void printCookies() {
        cookies.entrySet().forEach(stringMapEntry -> {
            Map<String, Cookie> map = cookies.get(stringMapEntry.getKey());
            if (map != null)
                map.entrySet().forEach(stringCookieEntry -> System.out.println("session:" + stringCookieEntry +"; sessionId:" + map.get(stringCookieEntry.getKey()).getSessionId()));
        });
    }
}
