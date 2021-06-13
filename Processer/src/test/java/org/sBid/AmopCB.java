package org.sBid;

import static org.fisco.bcos.sdk.utils.ByteUtils.byteArrayToInt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.fisco.bcos.sdk.amop.topic.AmopMsgIn;
import org.fisco.bcos.sdk.model.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmopCB extends org.fisco.bcos.sdk.amop.AmopCallback {
    private static final Logger logger = LoggerFactory.getLogger(AmopCB.class);
    private final boolean saveFlag;
    private final String filePath;
    private final String index;
    private final String name;

    public AmopCB(String filePath, String index, String name, boolean flag) {
        this.filePath = filePath;
        this.saveFlag = flag;
        this.index = index;
        this.name = name;
    }

    @Override
    public byte[] receiveAmopMsg(AmopMsgIn msg) {
        if (msg.getContent().length > 8) {
            byte[] content = msg.getContent();
            byte[] byteflag = subbytes(content, 0, 4);
            int flag = byteArrayToInt(byteflag);
            if (flag == -128) {
                // Received a file.
                byte[] bytelength = subbytes(content, 4, 4);
                int length = byteArrayToInt(bytelength);
                byte[] bytefilename = subbytes(content, 8, length);
                String filename = new String(bytefilename);
                String fileName = filename.substring(filename.lastIndexOf(File.separator) + 1);

                int contentlength = content.length - 8 - filename.length();
                byte[] fileContent = subbytes(content, 8 + filename.length(), contentlength);
                String fileContentString = new String(fileContent, StandardCharsets.UTF_8);
//                System.out.println("\n" + fileName + "\n" + fileContentString);
//                getFileFromBytes(fileContent, filePath + fileName);
                Random ran = new Random();
                String ranNum = String.valueOf(ran.nextInt(100));
                Global.writeFile(filePath + "temp" + ranNum + ".txt", fileContentString);
                if (!new File(filePath + "temp" + ranNum + ".txt").renameTo(new File(filePath + fileName))) {
                    System.err.println("Rename failed");
                    System.exit(-1);
                }
                String response = index + ":" + name + ":" + fileName;
                byte[] responseData = response.getBytes();
                if (msg.getType() == (short) MsgType.AMOP_REQUEST.getType()) {
                    Global.signalAMOPRecev = true;
//                    System.out.println("Response: " + response);
                }
                return responseData;
            }
        }

        String response = index + ":" + name;
        byte[] responseData = response.getBytes();
        // Print receive amop message
        System.out.println("Recv message:" + new String(msg.getContent()));
        if (msg.getType() == (short) MsgType.AMOP_REQUEST.getType()) {
            System.out.println("Response:" + new String(responseData));
        }
        // Response to the message sender
        return responseData;
    }

    public static byte[] subbytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static void getFileFromBytes(byte[] b, String outputFile) {
        File ret = null;
        BufferedOutputStream stream = null;
        FileOutputStream fstream = null;
        try {
            ret = new File(outputFile);
            fstream = new FileOutputStream(ret);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
        } catch (Exception e) {
            logger.error(" write exception, message: {}", e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.error(" close exception, message: {}", e.getMessage());
                }
            }

            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e) {
                    logger.error(" close exception, message: {}", e.getMessage());
                }
            }
        }
    }
}