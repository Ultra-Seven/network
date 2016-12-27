package HTTP.Download;

import HTTP.HttpClient;
import HTTP.Response;
import URL.URL;
import URL.URLParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by Administrator on 2016/12/7.
 */
public class MultiThreadsDownload extends Thread{
    private DownloadFile downloadFile;
    //record thr position of each file
    private long[] startPositions;
    private long[] endPositions;
    private static final int SLEEP_SECONDS = 500;
    private CountDownLatch latch;
    private Response response;
    private URL url;
    //threads
    private List<ThreadDownload> threadDownloads = new ArrayList<>();
    public MultiThreadsDownload(DownloadFile downloadFile, Response response, URL url) throws IOException {
        this.downloadFile = downloadFile;
        this.url = url;
        String filePath = DownloadFile.PATH + File.separator + downloadFile.getFileName() + ".temp";
        File tempFile = new File(filePath);
        this.response = response;
        startPositions = new long[downloadFile.getSplit()];
        endPositions = new long[downloadFile.getSplit()];
        latch = new CountDownLatch(downloadFile.getSplit());
    }
    @Override
    public void run() {
        long size = downloadFile.getFileSize();
        for (int i = 0, len = startPositions.length; i < len; i++) {
            int subSize = (int) (size / len);
            int subLength = i * subSize;
            startPositions[i] = subLength;
            //if the index is the last one of whole splits
            endPositions[i] = (i == len - 1) ? -1 : subLength + subSize - 1;
        }
        //the start of threads
        String path = DownloadFile.PATH + File.separator + downloadFile.getFileName();
        for (int i = 0; i < startPositions.length; i++) {
            threadDownloads.add(new ThreadDownload(url, path, startPositions[i], endPositions[i]));
            threadDownloads.get(i).run();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte last = 0;
            int index = 0;
            for (ThreadDownload threadDownload : threadDownloads) {
                if (threadDownload.entity != null) {
                    int length = threadDownload.entity.length;
//                    System.out.println(Arrays.toString(threadDownload.entity));
                    if (index == 0) {
                        outputStream.write(threadDownload.entity);
                    }
                    else
                        outputStream.write(threadDownload.entity, 0, length);
                    index++;
                }
            }
//            System.out.println(Arrays.toString(outputStream.toByteArray()));

            InputStream inputStream = getResponseBodyAsStream(response, outputStream.toByteArray());
            int len;
            byte[] buf = new byte[1024];
            outputStream = new ByteArrayOutputStream();
            while((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            response.setBytes(outputStream.toByteArray());
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getResponseBodyAsStream(Response response, byte[] readByte) throws IOException {
        String encoding = response.getContentEncoding();
        InputStream in = new ByteArrayInputStream(readByte);
        if("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } else if("deflate".equals(encoding)) {
            in = new ZipInputStream(in);
        }
        return in;
    }
    class ThreadDownload extends Thread {
        private URL url;
        private long startPosition;
        private long endPosition;
        private String filePath;
//        private Response response;
        private byte[] entity;
        ThreadDownload(URL url, String filePath, long startPosition, long endPosition) {
            this.url = url;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.filePath = filePath;
        }
        private synchronized void saveFile(String fileName, byte[] buff, int start, int length) throws IOException {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw");
            randomAccessFile.seek(this.startPosition);
            randomAccessFile.write(buff, start, length);
            randomAccessFile.close();
        }
        @Override
        public void run() {
            String end = endPosition == -1 ? "" : String.valueOf(endPosition);
            String range = "Range: bytes=" + startPosition + "-" + end + "\r\n";
            if (response.getEtag() != null) {
                range += "Etag: " + response.getEtag();
            }
            HttpClient client = null;
            try {
                client = new HttpClient(url, range, true);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Response response;
            if (client != null && (response = client.getResponse()) != null && response.getEntity() != null) {
                entity = response.getBytes();
                try {
                    saveFile(filePath, entity, 0, entity.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            latch.countDown();
        }
    }
}