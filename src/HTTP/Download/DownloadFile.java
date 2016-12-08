package HTTP.Download;

/**
 * Created by Administrator on 2016/12/6.
 */
public class DownloadFile {
    private String url;
    private String fileName;
    private int split;
    private long fileSize;
    public final static String PATH = "file";
    private final static int SPLIT_NUM = 5;

    public DownloadFile(String url, String fileName, int split, long fileSize) {
        this.url = url;
        this.fileName = fileName;
        this.split = split;
        this.fileSize = fileSize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getSplit() {
        return split;
    }

    public void setSplit(int split) {
        this.split = split;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
