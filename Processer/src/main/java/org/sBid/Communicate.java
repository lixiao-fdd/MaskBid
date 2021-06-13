package org.sBid;

import static org.sBid.FileToByteArrayHelper.byteCat;
import static org.sBid.FileToByteArrayHelper.getFileByteArray;
import static org.sBid.FileToByteArrayHelper.intToByteArray;

import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.amop.Amop;
import org.fisco.bcos.sdk.amop.AmopMsgOut;
import org.fisco.bcos.sdk.amop.topic.TopicType;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.Peers;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Communicate {
    private String topicName;
    private String content;
    private String fileName;
    private Boolean isBroadcast = false;
    private BcosSDK sdk;
    private String indexOp = "";
    private String nameOp = "";
    private Socket client;
    private int buffSize = 1024;
    private int headSize = 32;
    private Writer writer;
    private Reader reader;
    private OutputStream ost;
    private InputStream ist;
    private final Amop amop;

    public Communicate(String topicName, BcosSDK sdk) {
        this.topicName = topicName;
        this.sdk = sdk;
        amop = sdk.getAmop();
    }

    //publish word
    public void publish() {
        AmopMsgOut out = new AmopMsgOut();
        out.setType(TopicType.NORMAL_TOPIC);
        assert (content.length() != 0);
        out.setContent(content.getBytes());
        System.out.println("bytes: " + Arrays.toString(content.getBytes()));
        out.setTimeout(6000);
        out.setTopic(topicName);
        AmopResponseCB cb = new AmopResponseCB(indexOp, nameOp, fileName);

        if (isBroadcast) {
            // send out amop message by broad cast
            amop.broadcastAmopMsg(out);
            System.out.println(
                    "Step 1: Send out msg by broadcast, topic:"
                            + out.getTopic()
                            + " content:"
                            + new String(out.getContent()));
        } else {
            // send out amop message
            amop.sendAmopMsg(out, cb);
            System.out.println(
                    "Step 1: Send out msg, topic:"
                            + out.getTopic()
                            + " content:"
                            + new String(out.getContent()));
        }
    }

    //publish file
    public void publishFile(String fileNameShow) {
        Global.signalAMOPSend = false;
        Global.signalAMOPSendRes = false;
        Global.signalAMOPSendOK = false;
        Global.signalAMOPSendERR = false;
        try {
            if (!subscribed(sdk, topicName)) {
                System.out.println("No subscriber, exist.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Global.waitFile(fileName);
//        System.out.println("Start transmission");
//        System.out.println("==================");
        int flag = -128;
        byte[] byteflag = intToByteArray(flag);
//        assert (fileName.length()!=0);
//        if(fileName.length()==0){
//            System.err.println(fileName.length());
//            System.exit(1);
//        }
        int filelength = fileName.length();
        byte[] bytelength = intToByteArray(filelength);
        byte[] bytefilename = fileName.getBytes();
        byte[] contentfile = getFileByteArray(new File(fileName));
        byte[] content = byteCat(byteCat(byteCat(byteflag, bytelength), bytefilename), contentfile);


        AmopMsgOut out = new AmopMsgOut();
        out.setType(TopicType.NORMAL_TOPIC);
        out.setContent(content);
        out.setTimeout(6000);
        out.setTopic(topicName);
        AmopResponseCB cb = new AmopResponseCB(indexOp, nameOp, fileNameShow);
        if (isBroadcast) {
            amop.broadcastAmopMsg(out);
        } else {
            amop.sendAmopMsg(out, cb);
        }
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        System.out.println("Published file: " + fileName + " [" + df.format(LocalDateTime.now()) + "]");
        Global.signalAMOPSend = true;
    }

    //receiver
    public void subscribe(String filePath, String index, String name) {
        // Set callback
        org.fisco.bcos.sdk.amop.AmopCallback cb = new AmopCB(filePath, index, name, true);
        // Set a default callback
        amop.setCallback(cb);
        // Subscriber a normal topic
        amop.subscribeTopic(topicName, cb);
        System.out.println("Subscribed: " + topicName);
    }

    public boolean sendAmopFile(String fileNameSend) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String fileNameShow = fileNameSend.substring(fileNameSend.lastIndexOf(File.separator) + 1);
        this.fileName = fileNameSend;
        publishFile(fileNameShow);
        while (!Global.signalAMOPSendOK) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (Global.signalAMOPSendERR) {
                System.err.println("Sending the file " + fileNameShow + " ERROR, exit");
                return false;
            } else if (Global.signalAMOPSend && Global.signalAMOPSendRes && !Global.signalAMOPSendOK) {
                System.err.println("Sending the file " + fileNameShow + " failed and will be resent");
                publishFile(fileNameShow);
            }
        }
        System.out.println("Send FILE " + fileNameShow);

        return true;
    }

    public boolean recvAmopFile(String fileNameRevc) {
        Global.signalAMOPRecev = false;
        String fileNameShow = fileNameRevc.substring(fileNameRevc.lastIndexOf(File.separator) + 1);
        String waitStr = "Wait AMOP " + fileNameShow;
        String spaceStr = " ".repeat(waitStr.length() + 3);
        int aniIndex = 0;
        File fileChk = new File(fileNameRevc);
        while (!fileChk.exists()) {
            try {
                aniIndex = (aniIndex + 1) % 4;
                Thread.sleep(100);
                System.out.print("\r" + waitStr + Global.waitAnim.get(aniIndex));
//                if(Global.signalAMOPRecev)
//                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
//        System.out.println(fileNameShow + " exist: " + fileChk.exists() + " | signalAMOPRecev: " + Global.signalAMOPRecev);
        System.out.print("\r" + spaceStr + "\r");
        System.out.println("Recv FILE " + fileNameShow);
        Global.signalAMOPRecev = false;
        return true;
    }

    public boolean sendCoreFile(String fileName) {
        StringBuilder contents = new StringBuilder();
        if(!Global.readFile(fileName, contents))
            return false;
        try {
            String length = String.valueOf(contents.toString().length());
            String pack = "SIZE" + length + "SIZE";
            char[] chars = new char[headSize];
            char[] packChars = pack.toCharArray();
            System.arraycopy(packChars, 0, chars, 0, packChars.length);
            writer.write(chars);
            writer.flush();

            writer.write(contents.toString());
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean recvCoreFile(String fileName) {
        try {
            char[] chars = new char[headSize];
            if ((reader.read(chars)) == -1) {
                System.err.println("Head error");
                return false;
            }
            StringBuilder builder = new StringBuilder(String.valueOf(chars));
            int start = builder.indexOf("SIZE");
            int end = builder.indexOf("SIZE", start + 1);
            String size = builder.substring(start + 4, end);

            int fileSize = Integer.parseInt(size), readSize;

            builder.setLength(0);
            char[] recvBuff = new char[buffSize];
            while (fileSize > 0) {
                if (fileSize < buffSize) {
                    recvBuff = new char[fileSize];
                }
                if ((readSize = reader.read(recvBuff)) == -1) { //接收数据
                    System.err.println("Recv error");
                    return false;
                }
                builder.append(recvBuff);
                fileSize -= readSize;
            }

            Global.writeFile(fileName, builder.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean subscribed(BcosSDK sdk, String topicName) throws InterruptedException {
        Client client = sdk.getClient(1);
        Peers peers = client.getPeers();
        String waitStr = "Wait subscription";
        String spaceStr = " ".repeat(waitStr.length() + 3);
        int i = 0;
        while (true) {
            i = (i + 1) % 4;
            for (Peers.PeerInfo info : peers.getPeers()) {
                for (String tp : info.getTopic()) {
                    if (tp.equals(topicName)) {
                        System.out.print(spaceStr + "\r");
                        return true;
                    }
                }
            }
            System.out.print(waitStr + Global.waitAnim.get((i) % 4) + "\r");
            Thread.sleep(100);
            peers = client.getPeers();
        }
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setBroadcast(Boolean broadcast) {
        isBroadcast = broadcast;
    }

    public void setSdk(BcosSDK sdk) {
        this.sdk = sdk;
    }

    public String getIndexOp() {
        return indexOp;
    }

    public void setIndexOp(String indexOp) {
        this.indexOp = indexOp;
    }

    public String getNameOp() {
        return nameOp;
    }

    public void setNameOp(String nameOp) {
        this.nameOp = nameOp;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
        try {
            ost = client.getOutputStream();
            ist = client.getInputStream();
            writer = new OutputStreamWriter(ost);
            reader = new InputStreamReader(ist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe() {
        amop.unsubscribeTopic(topicName);
        System.out.println("Unsubscribe: " + topicName);
    }
}
