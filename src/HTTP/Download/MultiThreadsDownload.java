package HTTP.Download;

import HTTP.HttpClient;
import HTTP.Response;
import URL.URLParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by Administrator on 2016/12/7.
 */
public class MultiThreadsDownload implements Runnable{
    private DownloadFile downloadFile;
    //record thr position of each file
    private long[] startPositions;
    private long[] endPositions;
    private static final int SLEEP_SECONDS = 500;
    private CountDownLatch latch;
    private Response response;
    //threads
    List<ThreadDownload> threadDownloads = new ArrayList<>();

    public MultiThreadsDownload(DownloadFile downloadFile, Response response) throws IOException {
        this.downloadFile = downloadFile;
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
            endPositions[i] = (i == len - 1) ? size : subLength + subSize;
        }
        //the start of threads
        String path = DownloadFile.PATH + File.separator + downloadFile.getFileName();
        for (int i = 0; i < startPositions.length; i++) {
            threadDownloads.add(new ThreadDownload(downloadFile.getUrl(), path, startPositions[i], endPositions[i]));
            threadDownloads.get(i).run();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            byte[] readByte = new byte[(int) size];

//            int offset = 0;
//            int numRead = 0;
//            while (offset < readByte.length && (numRead = fileInputStream.read(readByte, offset, readByte.length - offset)) >= 0) {
//                offset += numRead;
//            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (ThreadDownload threadDownload : threadDownloads) {
                if (threadDownload.entity != null) {
                    outputStream.write(threadDownload.entity);
                }
            }
            InputStream inputStream = getResponseBodyAsStream(response, outputStream.toByteArray());
            System.out.println("Entity:" + Arrays.toString(outputStream.toByteArray()));
            int s;
            byte[] buf = new byte[100];
            //FIXME
            while((s = inputStream.read()) > 0) {
                byte singleByte = (byte) s;
                char character = (char) singleByte;
                System.out.print(character);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Download is over!");
    }

    private InputStream getResponseBodyAsStream(Response response, byte[] readByte) throws IOException {
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
    class ThreadDownload extends Thread {
        private String url;
        private long startPosition;
        private long endPosition;
        private String filePath;
        private Response response;
        private byte[] entity;
        ThreadDownload(String url, String filePath, long startPosition, long endPosition) {
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
            System.out.println("Download " + startPosition + " - " + endPosition + " start!");
            String range = "Range: bytes=" + startPosition + "-" + endPosition + "\r\n";
            URLParser urlParser = new URLParser(url);
            HttpClient client = null;
            try {
                client = new HttpClient(urlParser.getUrl(), range, true);
            } catch (IOException e) {
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
                response = client.getResponse();
            }
            System.out.println("Download " + startPosition + " - " + endPosition + " finish!");
            latch.countDown();
        }
    }
}