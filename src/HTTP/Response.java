package HTTP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/4.
 */
public class Response {
    private String version;
    private int statusCode;
    private String expression;
    private String date;
    private String connection = "Close";
    private long contentLength = -1;
    private String contentType;
    private String server;
    private List<String> cookies = new ArrayList<>();
    private String entity = "";
    private String location;
    private String transferCoding;
    private String contentEncoding;
    private String range;
    private byte[] bytes;
    private String header = "";
    private String Etag;
    public Response() {

    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void addCookie(String cookie) {
        cookies.add(cookie);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTransferCoding() {
        return transferCoding;
    }

    public void setTransferCoding(String transferCoding) {
        this.transferCoding = transferCoding;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getEtag() {
        return Etag;
    }

    public void setEtag(String etag) {
        Etag = etag;
    }
}
