package HTTP;

import DNS.DNSQuery;
import HTTP.Download.DownloadFile;
import HTTP.Download.MultiThreadsDownload;
import URL.TLS.TLSSocket;
import URL.URL;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import URL.URLParser;
/**
 * Created by Administrator on 2016/12/4.
 */
public class HttpClient {
    private static final int MAX_LINE_LENGTH = 65536;
    private static final int MAX_CHUNKS = 10;
    private static final int BUFFER_SIZE = 1 << 11;
    private byte[] readByte = new byte[MAX_LINE_LENGTH];
    private Socket socket;
    private URL url;
    private Response response;
    private boolean multiThread = true;
    private MultiThreadsDownload down;
    private boolean connect = true;
    public HttpClient(URL url) throws IOException, NoSuchAlgorithmException {
        this.multiThread = false;
        this.url = url;
        DNSQuery dnsQuery = new DNSQuery(url.getHost(), "202.120.224.26");
        this.url.setIps(dnsQuery.getIpList());
        if (this.url.getIps().size() == 0 || this.url.getPort() == 0)
            throw new IOException();
        if ("https://".equals(url.getProtocol())) {
            socket = new TLSSocket(url.getHost(), 443);
        }
        else {
            socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(dnsQuery.getIp(), url.getPort());
            socket.connect(socketAddress, 5000);
        }
    }
    public HttpClient(URL url, Socket socket) throws IOException {
        this.multiThread = false;
        this.url = url;
        this.socket = socket;
    }
    public HttpClient(URL url, String addString, boolean multiThread) throws IOException, NoSuchAlgorithmException {
        if (addString != null) {
            this.multiThread = multiThread;
            this.url = url;
            if (this.url.getIps().size() == 0 || this.url.getPort() == 0)
                throw new IOException();
            if ("https://".equals(url.getProtocol())) {
                socket = new TLSSocket(url.getHost(), 443);
            }
            else {
                socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(url.getIps().get(0), url.getPort());
                socket.connect(socketAddress, 5000);
            }
            sendRequestGet(addString);
        }
        else
            throw new IOException("wrong add strings");
    }

    public void sendRequestGet(String addString) throws IOException {
        String path = url.getPath();
        String request = "";
        //query parameter
        if (url.getQuery() != null)
            path += ("?" + url.getQuery());
        //request line
        request = request + "GET " + path + " HTTP/1.1\r\n";
        //header line
        request = request + getRequestString(addString);

        request = request + "\r\n";
        System.err.print(request);

        if ("https://".equals(url.getProtocol())) {
            TLSSocket tlsSocket = null;
            try {
                tlsSocket = new TLSSocket(url.getHost(), 443);
                this.socket = tlsSocket;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            tlsSocket.getTlsOutputStream().write(request.getBytes());
            InputStream inputStream = tlsSocket.getTlsInputStream();
            response = new Response();
            parseResponse(inputStream, response);
//            System.out.println(Arrays.toString(response.getBytes()));
            if (!multiThread && connect) {
                if (down != null) {
                    try {
                        down.join();
                    } catch (InterruptedException ignored) {

                    }
                }
                String utf8 = new String(response.getBytes(), "UTF-8");
//                while (utf8.charAt(0) == '\n') {
//                    utf8 = utf8.substring(1);
//                }
                System.out.print(utf8);
            }
        }
        else {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(request);
            bufferedWriter.flush();
            response = new Response();
            parseResponse(socket.getInputStream(), response);

            //close
            bufferedWriter.close();
//            System.out.println(Arrays.toString(readByte));
            if (!multiThread && connect) {
                if (down != null) {
                    try {
                        down.join();
                    } catch (InterruptedException ignored) {
                    }
                }
                String utf8 = new String(response.getBytes(), "UTF-8");
                System.out.print(utf8);
            }
        }
        socket.close();
    }
    private void parseResponse(InputStream inputStream, Response response) throws IOException {
        String line;
        String statusLine = readLine(inputStream);
        String header = "" +statusLine + "\r\n";
//        System.out.println(statusLine);
        String[] lines = statusLine.split(" ");
        if (lines.length >= 3) {
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
            response.setHeader(statusLine);
            //check status
            if (lines[1].charAt(0) == '4' || lines[1].charAt(0) == '5') {
                throw new IOException("invalid status");
            }
            while (true) {
                line = readLine(inputStream);
                if (line != null) {
//                    System.out.println(line);
                    header += line + "\r\n";
                    if (line.indexOf(':') == -1) {
                        throw new IOException("Invalid HTTP response header: " + line);
                    }
                    parseHeadLine(line, response);
                } else
                    break;
            }

            response.setHeader(header);
            int threshold = 10 * (1 << 20);
            long length = response.getContentLength();
            if (length < 0 && length != -1) {
                throw new IOException("wrong length");
            }
            if (length == 0) {
                System.out.println("The content length is zero!");
            } else if (length > threshold) {
                OutputStream outputStream = new ByteArrayOutputStream();
                if (length == -1) {
                    length = inputStream.available();
                }
                //broken header
                if (length != readEntity(inputStream, outputStream, length)) {
                    throw new IOException("wrong length");
                }
                String path = url.getPath();
                String fileName = url.getHost() + "_" + path.substring(path.lastIndexOf("/") + 1) + ".temp";
                int split = (int) (length / threshold) + 1;//(int) (length / MAX_LINE_LENGTH);
                this.connect = false;
                DownloadFile downloadFile = new DownloadFile(url.getUrl(), fileName, split, length);
                down = new MultiThreadsDownload(downloadFile, response, this.url);
                down.start();
            } else {
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
                        if (hex >= 0) {
                            readLine(inputStream);
                        }
                        else if (hex < 0) {
                            throw new IOException();
                        }
                    } while (hex >= 0 && index++ < MAX_CHUNKS);
                    response.setContentLength(length);
                } else {
                    if (length == -1) {
                        length = inputStream.available();
                    }
                    readEntity(inputStream, outputStream, length);
                }
                while (inputStream.available() > 0) {
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer, 0, buffer.length);
                    outputStream.write(buffer);
                }
                readByte = ((ByteArrayOutputStream) outputStream).toByteArray();
                byte[] bytes = getResponseBody(response);
                response.setBytes(bytes);
                String entity = getResponseBodyAsString(response);
                response.setEntity(entity);
                handleResponse(response);
            }
        }
        else
            throw new IOException("wrong status line");
    }
    //read the entity of http response
    private int readEntity(InputStream inputStream, OutputStream outputStream, long length) throws IOException {
        long size = length;
        byte[] buf = new byte[BUFFER_SIZE];
        long index = 0;
        int readLength;
        while(index < length && (readLength = inputStream.read(buf, 0, (int) Math.min(size, BUFFER_SIZE))) > 0) {
            //write input stream data into buffer
            outputStream.write(buf, 0, readLength);
            //set size
            size = Math.min(BUFFER_SIZE, length - (index += readLength));
        }
        return (int) index;
    }
    //parse head line and set properties of the response
    private void parseHeadLine(String headLine, Response response) throws IOException {
//        System.out.println(headLine);
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
                case "Etag":
                    response.setEtag(lines[1]);
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
                URL newUrl = urlParser.getUrl();
                this.connect = false;
                DNSQuery dnsQuery = new DNSQuery(newUrl.getHost(), "202.120.224.26");
                newUrl.setIps(dnsQuery.getIpList());
                //send redirect
                HttpClient client = null;
                try {
                    client = new HttpClient(urlParser.getUrl(), cookies, multiThread);

                } catch (NoSuchAlgorithmException ignored) {
                } finally {
                    if (urlParser.getUrl().getIps().size() > 0)
                        urlParser.getUrl().getIps().forEach(System.err::println);
                    if (client!= null && client.getResponse() != null)
                        System.err.println(client.getResponse().getHeader());
                }
            }
        }
    }
    private synchronized byte[] getResponseBody(Response response) throws IOException {
        String encoding = response.getContentEncoding();
        if(encoding == null) {
            return readByte;
        }
        InputStream in = new ByteArrayInputStream(readByte);
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
        return new String(response.getBytes(), "UTF-8");
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
        request = request + "Host: " +  url.getHost();
        if (url.getPort() != 80) {
            request = request + ":" + url.getPort();
        }
        request = request + "\r\n";
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
