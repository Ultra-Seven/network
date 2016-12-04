package HTTP;

import URL.URL;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by Administrator on 2016/12/4.
 */
public class HttpClient {
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

        BufferedInputStream streamReader = new BufferedInputStream(socket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));

        Response response = new Response();
        parseResponse(bufferedReader, response);
        //close
        bufferedReader.close();
        bufferedWriter.close();
        socket.close();
    }
    private void parseResponse(BufferedReader bufferedReader, Response response) throws IOException {
        String line;
        String statusLine = bufferedReader.readLine();
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
        while((line = bufferedReader.readLine()) != null) {
            parseHeadLineAndEntity(line, response);
        }

    }
    private void parseHeadLineAndEntity(String headLine, Response response) {
        //System.out.println(headLine);
        String[] lines = headLine.split(": ");
        switch (lines[0]) {
            case "Connection" : response.setConnection(lines[1]);break;
            case "Date" : response.setDate(lines[1]);break;
            case "Content-Length" : response.setContentLength(Integer.parseInt(lines[1]));break;
            case "Content-Type" : response.setContentType(lines[1]);break;
            case "Server" : response.setServer(lines[1]);break;
            case "Set-Cookie" :response.addCookie(lines[1]);break;
            case "" : break;
            default:response.addEntity(headLine);break;
        }
    }
}
