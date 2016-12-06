package HTTP.Download;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/7.
 */
public class MultiThreadsDownload implements Runnable {
    private DownloadFile downloadFile;
    //record thr position of each file
    private long[] startPosition;
    private long[] endPosition;
    private static final int SLEEP_SECONDS = 500;
    //threads
    List<ThreadDownload> threadDownloads = new ArrayList<>();
    @Override
    public void run() {

    }
}
class ThreadDownload extends Thread {

}