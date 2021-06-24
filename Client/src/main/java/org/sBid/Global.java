package org.sBid;

import org.apache.commons.codec.digest.DigestUtils;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.hash.Keccak256;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignature;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignatureResult;
import org.fisco.bcos.sdk.model.CryptoType;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Global {
    public static List<String> waitAnim = List.of(" \\", " |", " /", " -");
    public static int round;
    public static boolean signalAMOPRecev = false;
    public static boolean signalAMOPSend = false;
    public static boolean signalAMOPSendRes = false;
    public static boolean signalAMOPSendOK = false;
    public static boolean signalAMOPSendERR = false;


    //read a file
    public static boolean readFile(String filePath, ArrayList<String> content) {
        File file = new File(filePath);
        if (!file.exists())
            return false;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                content.add(str);
            }
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            System.err.println("\" " + filePath + " \"" + " Not Found, error message is " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return true;
    }

    public static boolean readFile(String filePath, StringBuilder content) {
        ArrayList<String> contentsList = new ArrayList<>();
        if (!readFile(filePath, contentsList))
            return false;
        int i = 0;
        for (; i < contentsList.size() - 1; i++)
            content.append(contentsList.get(i)).append("\n");
        content.append(contentsList.get(i));
        return true;
    }

    //write a file
    public static void writeFile(String filePath, ArrayList<String> content) {
        File file = new File(filePath);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            for (String str : content) {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            outputStream.close();
        } catch (IOException e) {
            System.err.println("\" " + filePath + " \"" + " error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void writeFile(String filePath, String content) {
        File file = new File(filePath);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write(content);
            bufferedWriter.close();
            outputStream.close();
        } catch (IOException e) {
            System.err.println("\" " + filePath + " \"" + " error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    //wait a file
    public static void waitFile(String filePath) {
        int waitIndex = 0;
        File fileName = new File(filePath);
        String waitStr = "Wait FILE " + filePath;
        String spaceStr = " ".repeat(waitStr.length() + 3);
        while (!fileName.exists()) {
            try {
                System.out.print(waitStr + Global.waitAnim.get((waitIndex++) % 4) + "\r");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print(spaceStr + "\r");
    }

    public static boolean zipFile(ArrayList<String> fileList, StringBuilder fileZip) {
        StringBuilder fileContent = new StringBuilder();
        for (String fileUploadPath : fileList) {
            String fileUploadName = fileUploadPath.substring(fileUploadPath.lastIndexOf(File.separator) + 1);
            fileContent.setLength(0);
            if (!Global.readFile(fileUploadPath, fileContent))
                return false;
            fileZip.append(fileUploadName).append("#").append(fileContent).append("@");
        }
        return true;
    }

    public static void unzipFile(String filePath, String fileZip) {
        String[] fileList = fileZip.split("@");
        for (String file : fileList) {
            if (file.length() > 1) {
                String[] temp = file.split("#");
                String fileName = temp[0];
                String fileContent = temp[1];
                Global.writeFile(filePath + fileName, fileContent);
            }
        }
    }

    //Deserialize a string
    public static void deserialize(String str_in, ArrayList<String> str_out) {
        String delimiter = ";";
        int pos_start = 0, pos_end = 0;
        while ((pos_end = str_in.indexOf(delimiter, pos_end)) != -1) {
            str_out.add(str_in.substring(pos_start, pos_end));
            pos_start = ++pos_end;
        }
    }

    //读取注册信息
    public static boolean loadRegInfo(ArrayList<Register> register_table, int counts, String index, Register
            opponent, SBid contract, String Table_register_name, int round) {
        // Storage the registration informations and confirm Opponent
        if (!register_table.isEmpty())
            register_table.clear();
        Register firstItem = null, secondItem = null, tempItem = null;
        boolean byeFlag = true;
        int animIndex = 0;
        String waitStr = "Wait No.";
        String spaceStr = " ".repeat(waitStr.length() + 3);
        for (int i = 1; i <= counts; i++) {
            tempItem = new Register(contract, Table_register_name, String.valueOf(i), "");
            if (tempItem.read()) {//若存在该条注册信息
                String result = tempItem.getResult();
                if (result.compareTo("lose") == 0) {//若该参与者已经失败，则跳过
                    register_table.add(tempItem);
                } else {//若该参与者还在竞标中
                    String status = tempItem.getStatus();
                    if (Integer.parseInt(status) >= round) {//若该参与者与自己处在同一轮（或处于更后的轮次）中
                        register_table.add(tempItem);
                        if (firstItem == null)//若配对中前一个位置为空，则直接放入
                            firstItem = tempItem;
                        else {//若配对中前一个位置不为空，则放入后一个位置，并开始判断是否为自己与对手
                            secondItem = tempItem;
                            if (firstItem.getIndex().compareTo(index) == 0) {//自己在第一个位置，则第二个为对手
                                opponent.copy(secondItem);
                                byeFlag = false;//自己出现在配对中则没有轮空
                            } else if (secondItem.getIndex().compareTo(index) == 0) {//自己在第二个位置，则第一个为对手
                                opponent.copy(firstItem);
                                byeFlag = false;
                            }
                            firstItem = null;//若自己不在该配对中，则清空配对
                        }
                    } else {//若该参与者落后于自己，则等待
                        try {
                            animIndex = (animIndex + 1) % 4;
                            System.out.print(waitStr + (i) + Global.waitAnim.get(animIndex) + "\r");
                            Thread.sleep(100);
                            --i;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {//若不存在该条注册信息，则等待
                try {
                    animIndex = (animIndex + 1) % 4;
                    System.out.print(waitStr + (i) + Global.waitAnim.get(animIndex) + "\r");
                    Thread.sleep(100);
                    --i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.print(spaceStr + "\r");
        System.out.println("+--Registration table ");
        for (Register temp : register_table) {
            System.out.format("|  %-3s | %-5s\t| %-4s\n", temp.getIndex(), temp.getName(), temp.getResult());
        }
        System.out.println("+--Registration table end ");
        return byeFlag;//返回是否轮空
    }

    // 旧 read registration infomation
    public static boolean readRegInfo(ArrayList<Register> register_table, int counts, String index, Register
            opponent, SBid contract, String Table_register_name) {
        // Storage the registration informations and confirm Opponent
        if (!register_table.isEmpty())
            register_table.clear();
        Register firstItem = null, secondItem = null, tempItem = null;
        boolean byeFlag = true;
        int animIndex = 0;
        String waitStr = "Wait No.";
        String spaceStr = " ".repeat(waitStr.length() + 3);
        for (int i = 1; i <= counts; i++) {
            tempItem = new Register(contract, Table_register_name, String.valueOf(i), "");
            if (tempItem.read()) {//若存在该条注册信息
                String result = tempItem.getResult();
                if (result.compareTo("lose") == 0) {//若该参与者已经失败，则跳过
                    register_table.add(tempItem);
                } else {//若该参与者还在竞标中
                    String status = tempItem.getStatus();
                    if (Integer.parseInt(status) >= round) {//若该参与者与自己处在同一轮（或处于更后的轮次）中
                        register_table.add(tempItem);
                        if (firstItem == null)//若配对中前一个位置为空，则直接放入
                            firstItem = tempItem;
                        else {//若配对中前一个位置不为空，则放入后一个位置，并开始判断是否为自己与对手
                            secondItem = tempItem;
                            if (firstItem.getIndex().compareTo(index) == 0) {//自己在第一个位置，则第二个为对手
                                opponent.copy(secondItem);
                                byeFlag = false;//自己出现在配对中则没有轮空
                            } else if (secondItem.getIndex().compareTo(index) == 0) {//自己在第二个位置，则第一个为对手
                                opponent.copy(firstItem);
                                byeFlag = false;
                            }
                            firstItem = null;//若自己不在该配对中，则清空配对
                        }
                    } else {//若该参与者落后于自己，则等待
                        try {
                            animIndex = (animIndex + 1) % 4;
                            System.out.print(waitStr + (i) + Global.waitAnim.get(animIndex) + "\r");
                            Thread.sleep(100);
                            --i;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {//若不存在该条注册信息，则等待
                try {
                    animIndex = (animIndex + 1) % 4;
                    System.out.print(waitStr + (i) + Global.waitAnim.get(animIndex) + "\r");
                    Thread.sleep(100);
                    --i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.print(spaceStr + "\r");
        System.out.println("+--Registration table ");
        for (Register temp : register_table) {
            System.out.format("|  %-3s | %-5s\t| %-4s\n", temp.getIndex(), temp.getName(), temp.getResult());
        }
        System.out.println("+--Registration table end ");
        return byeFlag;//返回是否轮空
    }

    public static boolean readResult(int counts, SBid contract, String Table_register_name) {
        Register tempItem;
        ArrayList<Register> register_table = new ArrayList<>();
        boolean finishFlag = false;
        while (!finishFlag) {
            for (int i = 1; i <= counts; i++) {
                tempItem = new Register(contract, Table_register_name, String.valueOf(i), "");
                if (tempItem.read()) {
                    if (tempItem.getResult().compareTo("win") == 0) {
                        finishFlag = true;
                        break;
                    }
                } else {
                    try {
                        --i;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
            if (!finishFlag) {
                try {
                    System.out.print("Waiting for finish\r");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 1; i <= counts; i++) {
            tempItem = new Register(contract, Table_register_name, String.valueOf(i), "");
            tempItem.read();
            register_table.add(tempItem);
        }
        System.out.println("+--Final result table    ");
        for (Register temp : register_table) {
            System.out.format("|  %-3s | %-5s\t| %-4s\n", temp.getIndex(), temp.getName(), temp.getResult());
        }
        System.out.println("+--Final result table end ");
        return true;
    }

    public static String baseDecode(String ciphertext) {
        byte[] base64decodedBytes = Base64.getDecoder().decode(ciphertext);
        return new String(base64decodedBytes);
    }

    public static String sha1(String plaintext) {
        return DigestUtils.sha1Hex(plaintext);
    }

    /// 生成签名
    public static String signature(String data, CryptoKeyPair cryptoKeyPair) {
        CryptoSuite cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
        String hashData = cryptoSuite.hash(data);
        return (cryptoSuite.sign(hashData, cryptoKeyPair)).convertToString();
    }

    /// 验证签名
    public static boolean verifySignature(String signature, String publicKey, String data) {
        CryptoSuite cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
        String hashData = cryptoSuite.hash(data);
        return cryptoSuite.verify(publicKey, hashData, signature);
    }

    //时间推算
    public static String dateCaculate(String date, int duration, String unit) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分");
        Date bidDateStart = new Date();
        Calendar calendar = Calendar.getInstance();
        try {
            bidDateStart = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.setTime(bidDateStart);
        switch (unit) {
            case "minutes" -> {
                //加分钟
                calendar.add(Calendar.MINUTE, duration);
            }
            case "hours" -> {
                //加小时
                calendar.add(Calendar.HOUR, duration);
            }
            case "date" -> {
                //加天数
                calendar.add(Calendar.DATE, duration);
            }
        }
        Date bidDateEnd = calendar.getTime();
        return sdf.format(bidDateEnd);
    }

    //时间比较，若当前时间大于输入时间，则返回true
    public static boolean dateNowAfter(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分");
        Date bidDateStart = new Date();
        Date dateNow = new Date();
        try {
            bidDateStart = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateNow.compareTo(bidDateStart) > 0;
    }

    //时间解析
    public static Date getDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分");
        Date bidDateStart = new Date();
        try {
            bidDateStart = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return bidDateStart;
    }

    //分离招标名陈与招标内容
    public static void getBidNameContent(String nameWithContentBase64, StringBuilder bidName, StringBuilder bidContent) {
        String nameWithContent = Global.baseDecode(nameWithContentBase64);
        bidName.append(nameWithContent, 0, nameWithContent.indexOf("\n"));
        bidContent.append(nameWithContent.substring(nameWithContent.indexOf("\n") + 1));
    }

    //创建文件夹
    public static void createFolder(String path) {
        Path filesDir = Paths.get(path);
        try {
            java.nio.file.Files.createDirectory(filesDir);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            System.err.println("Failed to create " + filesDir);
            e.printStackTrace();
        }
    }

    // AES CBC 加密:
    public static String encrypt(String keyStr, String inputStr) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(keyStr.getBytes(StandardCharsets.UTF_8));
        byte[] key = md.digest();
        byte[] input = inputStr.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        // CBC模式需要生成一个16 bytes的initialization vector:
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] iv = sr.generateSeed(16);
        IvParameterSpec ivps = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivps);
        byte[] data = cipher.doFinal(input);
        // IV不需要保密，把IV和密文一起返回:
        return Global.byteArrayToHex(join(iv, data));
    }

    // 解密:
    public static String decrypt(String keyStr, String inputHex) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(keyStr.getBytes(StandardCharsets.UTF_8));
        byte[] key = md.digest();
        // 把input分割成IV和密文:
        byte[] iv = new byte[16];
        byte[] input = Global.hexToByteArray(inputHex);
        byte[] data = new byte[input.length - 16];
        System.arraycopy(input, 0, iv, 0, 16);
        System.arraycopy(input, 16, data, 0, data.length);
        // 解密:
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivps = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivps);
        return new String(cipher.doFinal(data));
    }

    private static byte[] join(byte[] bs1, byte[] bs2) {
        byte[] r = new byte[bs1.length + bs2.length];
        System.arraycopy(bs1, 0, r, 0, bs1.length);
        System.arraycopy(bs2, 0, r, bs1.length, bs2.length);
        return r;
    }

    //byte数组转hex字符串
    public static String byteArrayToHex(byte[] bytes) {
        final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }

        return sb.toString();
    }

    //hex字符串转byte数组
    public static byte[] hexToByteArray(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            //奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            //偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }
}
