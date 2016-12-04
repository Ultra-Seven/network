package DNS;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/3.
 */
public class DNSQuery {
    private final static int PORT = 53;
    private final static int BUF_SIZE = 8192;
    private String domain;
    private String dnsServer;
    private List<String> ipList = new ArrayList<>();
    public DNSQuery(String domain, String dnsServer) throws IOException {
        this.domain = domain;
        this.dnsServer = dnsServer;
        DatagramSocket datagramSocket = new DatagramSocket(8080);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUF_SIZE);
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);
        //make DNS Message
        sendMessage(output, domain);
        InetAddress dns = InetAddress.getByName(dnsServer);
        DatagramPacket sendSocket = new DatagramPacket(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size(), dns, PORT);

        //send DNS query
        datagramSocket.send(sendSocket);
        //receive answer
        byte[] buf = new byte[BUF_SIZE];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        DataInputStream inputStream = new DataInputStream(byteArrayInputStream);
        DatagramPacket answer = new DatagramPacket(buf, BUF_SIZE);
        datagramSocket.receive(answer);

        //parse answer message
        receiveMessage(inputStream, ipList);
        //close socket
        datagramSocket.close();
    }
    private void sendMessage(DataOutputStream dataOutputStream, String domain) throws IOException {
        //identification
        dataOutputStream.writeShort(1);
        //flags
        dataOutputStream.writeShort(0x100);
        //the number of questions
        dataOutputStream.writeShort(1);
        //the number of answers
        dataOutputStream.writeShort(0);
        //the number of authority
        dataOutputStream.writeShort(0);
        //the number of additional RRs
        dataOutputStream.writeShort(0);
        String[] parts = domain.split("\\.");
        for (String part : parts) {
            dataOutputStream.writeByte((byte) (part.length()));
            dataOutputStream.write(part.getBytes());
        }
        dataOutputStream.writeByte(0);

        //query type : A
        dataOutputStream.writeShort(1);
        //query class : IN
        dataOutputStream.writeShort(1);
        dataOutputStream.flush();
    }

    private void receiveMessage(DataInputStream inputStream, List<String> list) throws IOException {
        //transaction id(2) + flags(2) + num of query(2) = 6
        inputStream.skip(6);
        //the number of answers
        short answers = inputStream.readShort();
        //authority(2) + additional(2) = 4
        inputStream.skip(4);
        //skip domain name
        skipDomain(inputStream);
        //query type(2) + query class(2) = 4
        inputStream.skip(4);
        //answer records
        for (int i = 0; i < answers; i++) {
            inputStream.mark(1);
            byte head = inputStream.readByte();
            //compress domain name
            if ((head & 0xc0) == 0xc0) {
                inputStream.skip(1);
            }
            else {
                inputStream.reset();
                skipDomain(inputStream);
            }
            short queryType = inputStream.readShort();
            //query class(2) + ttl(4)= 6
            inputStream.skip(6);
            //length of ip
            short length = inputStream.readShort();
            //ip
            if (queryType == 1 && length == 4) {
                int address = inputStream.readInt();
                list.add(getIp(address));
            } else {
                inputStream.skip(length);
            }
        }
    }
    private void skipDomain(DataInputStream inputStream) throws IOException {
        while (true) {
            byte length = inputStream.readByte();
            if (length != 0) {
                inputStream.skip(length);
            }
            else
                break;
        }
    }
    private String getIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }

    public void printAnswer() {
        System.out.println("domain:" + domain + ";dns host:" + dnsServer);
        ipList.forEach(s -> System.out.println("ip:" + s));
    }
}
