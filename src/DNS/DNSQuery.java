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
        DatagramSocket datagramSocket2 = new DatagramSocket(8081);
        datagramSocket.setSoTimeout(5000);
        datagramSocket2.setSoTimeout(5000);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUF_SIZE);
        ByteArrayOutputStream byteArrayOutputStream6 = new ByteArrayOutputStream(BUF_SIZE);
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);
        DataOutputStream output6 = new DataOutputStream(byteArrayOutputStream6);
        //make DNS Message
        sendIpv4Message(output, domain);
        sendIpv6Message(output6, domain);
        InetAddress dns = InetAddress.getByName(dnsServer);
        DatagramPacket sendSocket = new DatagramPacket(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size(), dns, PORT);
        DatagramPacket sendSocket6 = new DatagramPacket(byteArrayOutputStream6.toByteArray(), byteArrayOutputStream6.size(), dns, PORT);

        //send DNS query
        datagramSocket.send(sendSocket);
        datagramSocket2.send(sendSocket6);
        //receive answer
        byte[] buf = new byte[BUF_SIZE];
        byte[] buf2 = new byte[BUF_SIZE];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        DataInputStream inputStream = new DataInputStream(byteArrayInputStream);
        ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(buf2);
        DataInputStream inputStream2 = new DataInputStream(byteArrayInputStream2);
        DatagramPacket answer = new DatagramPacket(buf, BUF_SIZE);
        DatagramPacket answer2 = new DatagramPacket(buf2, BUF_SIZE);
        datagramSocket.receive(answer);
        datagramSocket2.receive(answer2);

        //parse answer message
        receiveMessage(inputStream, ipList);
        receiveMessage(inputStream2, ipList);
        //close socket
        datagramSocket.close();
        datagramSocket2.close();
    }

    private void sendIpv6Message(DataOutputStream dataOutputStream, String domain) throws IOException {
        //identification
        dataOutputStream.writeShort(100);
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

        //query type : AAAA
        dataOutputStream.writeShort(28);
        //query class : IN
        dataOutputStream.writeShort(1);
        dataOutputStream.flush();
    }

    private void sendIpv4Message(DataOutputStream dataOutputStream, String domain) throws IOException {
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
        //TODO:analyze flags
        //transaction id(2) + flags(2) + num of query(2) = 6
        inputStream.skip(2);
        byte flag1 = inputStream.readByte();
        byte flag2 = inputStream.readByte();
        if ((flag2 & 0x0F) != 0x00) {
            System.out.println("wrong");
            return;
        }
        inputStream.skip(2);
        //the number of answers
        short answers = inputStream.readShort();
        //authority(2) + additional(2) = 4
        inputStream.skip(4);
        //skip domain name
        skipDomain(inputStream);
        //query type(2) + query class(2) = 4
        short type = inputStream.readShort();
        inputStream.skip(2);
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
            if (queryType == type && queryType == 1 && length == 4) {
                int address = inputStream.readInt();
                list.add(getIp(address));
            }else if (queryType == type && queryType == 0x1c && length == 16) {
                short addr1 = inputStream.readShort();
                short addr2 = inputStream.readShort();
                short addr3 = inputStream.readShort();
                short addr4 = inputStream.readShort();
                short addr5 = inputStream.readShort();
                short addr6 = inputStream.readShort();
                short addr7 = inputStream.readShort();
                short addr8 = inputStream.readShort();
                String ipv6 = getIp(addr1, addr2, addr3, addr4, addr5, addr6, addr7, addr8);
                list.add(ipv6);
            }
            else {
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
            //i don't know why it happens
            if ((length & 0xc0) == 0xc0) {
                inputStream.skip(1);
                break;
            }
        }
    }
    private String getIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }
    private String getIp(short add1, short add2, short add3, short add4, short add5, short add6, short add7, short add8) {
        short[] segments = {add1, add2, add3, add4, add5, add6, add7, add8};
        ArrayList<String> ip = new ArrayList<>();
        for (short segment : segments) {
            String hex = Integer.toHexString(segment & 0xffff) ;
            if ("0".equals(hex) && ip.size() > 0 && "0".equals(ip.get(ip.size() - 1))) {
                continue;
            }
            ip.add(hex);
        }
        String ret = "";
        for (int i = 0; i < ip.size(); i++) {
            String s = ip.get(i);
            ret = ("0".equals(s)) ? ret : ret + s;
            if (i != ip.size() - 1)
                ret += ":";
        }
        return ret;
    }
    private String getHex(int decimal) {
        if (decimal == 0)
            return "0";
        else
            return  Integer.toHexString(decimal);
    }
    public void printAnswer() {
        System.out.println("domain:" + domain + ";dns host:" + dnsServer);
        ipList.forEach(s -> System.out.println("ip:" + s));
    }
    public String getIp() {
        if (ipList.size() > 0)
            return ipList.get(0);
        else
            return null;
    }

    public List<String> getIpList() {
        return ipList;
    }
}
