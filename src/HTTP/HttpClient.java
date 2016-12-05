package HTTP;

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
    public HttpClient(URL url) throws IOException {
        this.url = url;
        socket = new Socket();
        sendRequestGet();
    }
    public void sendRequestGet() throws IOException {
        String path = url.getPath();
        String host = url.getHost();
        int port = url.getPort();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        socket.connect(socketAddress);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        //request line
        bufferedWriter.write("GET " + path + " HTTP/1.1\r\n");
        //header line
        bufferedWriter.write("Host: " + host + "\r\n");
        bufferedWriter.write("Connection: keep-alive\r\n");
        bufferedWriter.write("Upgrade-Insecure-Requests: 1\r\n");
        bufferedWriter.write(Browser.getBrowserType(Browser.CHROME));
        bufferedWriter.write("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
        bufferedWriter.write("Accept-Encoding: gzip, deflate, sdch, br\r\n");
        bufferedWriter.write("Accept-Language: zh-CN,zh;q=0.8\r\n");


        bufferedWriter.write("\r\n");
        bufferedWriter.flush();

        Response response = new Response();
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

        int length = response.getContentLength();
        if(length == 0) { // we're done here
            return ;
        }

        OutputStream outputStream = new ByteArrayOutputStream();;


        int index;
        if("chunked".equals(response.getTransferCoding())) {
            index = length = 0;
            int hex;
            String strHex;

            do {
                strHex = readLine(inputStream).trim();
                hex = Integer.parseInt(strHex, 16);
                length += readEntity(inputStream, outputStream, hex);
                if(hex > 0) {
                    readLine(inputStream); // there's a blank line before the content
                }

            } while(hex > 0 && index++ < MAX_CHUNKS);
        }
        else {
            readEntity(inputStream, outputStream, length);
            readByte = ((ByteArrayOutputStream)outputStream).toByteArray();
        }

        String entity = getResponseBodyAsString(response);
        System.out.println(entity);
    }
    //read the entity of http response
    private int readEntity(InputStream inputStream, OutputStream outputStream, int length) throws IOException {
        int size = BUFFER_SIZE;
        byte[] buf = new byte[BUFFER_SIZE];

        int index = 0;
        int readLength;
        while(index < length && (readLength = inputStream.read(buf, 0, size)) > 0) {
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
                    response.setContentLength(Integer.parseInt(lines[1]));
                    break;
                case "Content-Type":
                    response.setContentType(lines[1]);
                    break;
                case "Server":
                    response.setServer(lines[1]);
                    break;
                case "Set-Cookie":
                    response.addCookie(lines[1]);
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
    private void handleResponse(BufferedReader bufferedReader, Response response) throws IOException {
        if (response.getStatusCode() == 302) {
            if (response.getLocation() != null) {
                String location = response.getLocation();
                URLParser urlParser = new URLParser(location);
                new HttpClient(urlParser.getUrl());
            }
        }
        else if (response.getStatusCode() == 301) {

        }
    }
    public byte[] getResponseBody(Response response) throws IOException {

        String encoding = response.getContentEncoding();
        if(encoding == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(readByte);;

        //if the content is compressed by giz
        if("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } //zip compressed
        else if("deflate".equals(encoding)) {
            in = new ZipInputStream(in);
        }
        int size;
        byte[] buf = new byte[1024];
        while((size = in.read(buf)) > 0) {
            outputStream.write(buf, 0, size);
        }
        in.close();
        return outputStream.toByteArray();
    }

    private String getResponseBodyAsString(Response response) throws IOException, FileNotFoundException {
        return new String(getResponseBody(response));
    }

    public InputStream getResponseBodyAsStream(Response response) throws FileNotFoundException, IOException {
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
}
