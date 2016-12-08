package HTTP;

import HTTP.Download.DownloadFile;
import HTTP.Download.MultiThreadsDownload;
import URL.URL;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.ParseException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import URL.URLParser;
/**
 * Created by Administrator on 2016/12/4.
 */
public class HttpClient {
    private static final int MAX_LINE_LENGTH = 65536;
    private static final int MAX_CHUNKS = 10;
    private static final int BUFFER_SIZE = 1 << 10;
    private byte[] readByte = new byte[MAX_LINE_LENGTH];
    private Socket socket;
    private URL url;
    private Response response;
    private boolean multiThread = true;
    public HttpClient(URL url) throws IOException {
        this.multiThread = false;
        this.url = url;
        socket = new Socket();
        sendRequestGet("");
    }
    public HttpClient(URL url, String addString, boolean multiThread) throws IOException {
        this.multiThread = multiThread;
        if (addString != null) {
            this.url = url;
            socket = new Socket();
            sendRequestGet(addString);
        }
        else {
            System.out.println("Wrong added string");
        }
    }
    private void sendRequestGet(String addString) throws IOException {
        String path = url.getPath();
        String host = url.getHost();
        int port = url.getPort();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        socket.connect(socketAddress);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        String request = "";
        //request line
        request = request + "GET " + path + " HTTP/1.1\r\n";
        //header line
        request = request + getRequestString(addString);

        request = request + "\r\n";
        System.out.println(request);
        bufferedWriter.write(request);
        bufferedWriter.flush();

        response = new Response();
        parseResponse(socket.getInputStream(), response);


        //close
        bufferedWriter.close();
        socket.close();
    }
    private void parseResponse(InputStream inputStream, Response response) throws IOException {
        String line;
        String statusLine = readLine(inputStream);
        String[] lines = statusLine.split(" ");
        //set protocol version
        response.setVersion(lines[0]);
        //set status code
        response.setStatusCode(Integer.parseInt(lines[1]));
        //set expression
        String expression = "";
        for (int i = 2; i < lines.length; i++) {
            expression = (i == lines.length - 1) ? lines[i] : lines[i] + " ";
        }
        response.setExpression(expression);

        while(true) {
            line = readLine(inputStream);
            if(line != null) {
                if(line.indexOf(':') == -1) {
                    throw new IOException("Invalid HTTP response header: " + line);
                }
                parseHeadLine(line, response);
            }
            else
                break;
        }
        int threshold = 1000;//10 * (1 << 20);
        long length = response.getContentLength();
        if(length == 0) { // we're done here
            System.out.println("The content length is zero!");
        }
        else if (length > threshold) {
            String path = url.getPath();
            String fileName = url.getHost() + "_" + path.substring(path.lastIndexOf("/") + 1) + ".temp";
            int split = (int) (length / threshold) + 1;//(int) (length / MAX_LINE_LENGTH);
            DownloadFile downloadFile = new DownloadFile(url.getUrl(), fileName, split, length);
            MultiThreadsDownload down = new MultiThreadsDownload(downloadFile, response);
            new Thread(down).start();
            System.out.println("The content length is too large. Multiple threads are running!");
        }
        else {
            OutputStream outputStream = new ByteArrayOutputStream();
            long index;
            if ("chunked".equals(response.getTransferCoding())) {
                index = length = 0;
                int hex;
                String strHex;
                do {
                    strHex = readLine(inputStream).trim();
                    hex = Integer.parseInt(strHex, 16);
                    length += readEntity(inputStream, outputStream, hex);
                    if (hex > 0) {
                        readLine(inputStream); // there's a blank line before the content
                    }

                } while (hex > 0 && index++ < MAX_CHUNKS);
            } else {
                readEntity(inputStream, outputStream, length);
                readByte = ((ByteArrayOutputStream) outputStream).toByteArray();
            }
            byte[] bytes = getResponseBody(response);
            response.setBytes(bytes);
            String entity = getResponseBodyAsString(response);
            response.setEntity(entity);
            System.out.println(response.getRange());
            System.out.println(Arrays.toString(bytes));
            handleResponse(response);
        }
    }
    //read the entity of http response
    private int readEntity(InputStream inputStream, OutputStream outputStream, long length) throws IOException {
        long size = BUFFER_SIZE;
        byte[] buf = new byte[BUFFER_SIZE];

        int index = 0;
        int readLength;
        while(index < length && (readLength = inputStream.read(buf, 0, (int) size)) > 0) {
            //write input stream data into buffer
            outputStream.write(buf, 0, readLength);
            //set size
            size = Math.min(BUFFER_SIZE, length - (index += readLength));
        }
        return index;
    }
    //parse head line and set properties of the response
    private void parseHeadLine(String headLine, Response response) throws IOException {
        System.out.println(headLine);
        if (headLine != null) {
            String[] lines = headLine.split(": ");
            switch (lines[0]) {
                case "Connection":
                    response.setConnection(lines[1]);
                    break;
                case "Date":
                    response.setDate(lines[1]);
                    break;
                case "Content-Length":
                    response.setContentLength(Long.parseLong(lines[1]));
                    break;
                case "Content-Type":
                    response.setContentType(lines[1]);
                    break;
                case "Server":
                    response.setServer(lines[1]);
                    break;
                case "Set-Cookie":
                    response.addCookie(lines[1]);
                    handleCookie(lines[1]);
                    break;
                case "Location":
                    response.setLocation(lines[1]);
                    break;
                case "Transfer-Encoding":
                    response.setTransferCoding(lines[1]);
                    break;
                case "X-Powered-By":
                    break;
                case "Cache-Control":
                    break;
                case "Expires":
                    break;
                case "Vary":
                    break;
                case "Content-Encoding":
                    response.setContentEncoding(lines[1]);
                    break;
                case "Content-Range":
                    response.setRange(lines[1]);
                    break;
                case "Keep-Alive":
                    break;
                case "":
                    break;
                default:
                    break;
            }
        }
    }
    //if the headline is about a cookie
    private void handleCookie(String value) {
        try {
            Cookie cookie = Cookie.getParseCookie(value);
            String domain = cookie.getValue("domain") != null ? cookie.getValue("domain") : url.getHost();
            cookie.setHost(domain);
            CookieManager.registerCookie(cookie);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
    //handle response
    private void handleResponse(Response response) throws IOException {
        if (response.getStatusCode() == 302 || response.getStatusCode() == 301) {
            String cookies = "";
            for (int i = 0; i < response.getCookies().size(); i++) {
                cookies = cookies + "Set-Cookie: " + response.getCookies().get(i) + "\r\n";
            }
            if (response.getLocation() != null) {
                String location = response.getLocation();
                URLParser urlParser = new URLParser(location);
                new HttpClient(urlParser.getUrl(), cookies, multiThread);
            }
        }
    }
    private synchronized byte[] getResponseBody(Response response) throws IOException {
        String encoding = response.getContentEncoding();
        if(encoding == null) {
            return null;
        }
        InputStream in = new ByteArrayInputStream(readByte);;

        //if the content is compressed by giz
        if("gzip".equals(encoding) && !multiThread) {
            in = new GZIPInputStream(in);
        } //zip compressed
        else if("deflate".equals(encoding) && !multiThread) {
            in = new ZipInputStream(in);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int size;
        byte[] buf = new byte[1024];
        while((size = in.read(buf)) > 0) {
            outputStream.write(buf, 0, size);
        }
        in.close();
        return outputStream.toByteArray();
    }

    private String getResponseBodyAsString(Response response) throws IOException {
        return new String(getResponseBody(response));
    }

    public InputStream getResponseBodyAsStream(Response response) throws IOException {
        String encoding = response.getContentEncoding();
        InputStream in = new ByteArrayInputStream(readByte);

        if("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } else
        if("deflate".equals(encoding)) {
            in = new ZipInputStream(in);
        }
        return in;

    }
    private String readLine(InputStream in) throws IOException {
        StringBuilder stringBuffer = new StringBuilder();
        int i = 0;
        char c;
        while((c = (char) (in.read() & 0xFF)) != '\n' && i++ < MAX_LINE_LENGTH) {
            if(c == '\r') { continue; }
            stringBuffer.append(c);
        }
        if(i > 1) {
            return stringBuffer.toString();
        }
        else {
            return null;
        }
    }
    private String getRequestString(String addString) {
        String request = "";
        request = request + "Host: " +  url.getHost() + "\r\n";
        request = request + "Connection: keep-alive\r\n";
        request = request + "Upgrade-Insecure-Requests: 1\r\n";
        request = request + Browser.getBrowserType(Browser.CHROME);
        request = request + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n";
        request = request + "Accept-Encoding: gzip, deflate, sdch, br\r\n";
        request = request + "Accept-Language: zh-CN,zh;q=0.8\r\n";
        request = request + addString;
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public byte[] getReadByte() {
        return readByte;
    }
}
