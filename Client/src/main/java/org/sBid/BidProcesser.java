package org.sBid;

import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//进行竞标
public class BidProcesser {
    //所需信息
    private final Account account;//账户
    private final String tenderName;//标的发起方
    private final String bidCode;//标的编号
    private final String index;//竞标编号
    private final String amount;//竞标金额
    private final String Table_SBid_Name;//标的发起方的标的表的表名
    private final String Table_Register_Name;//标的注册表的表名
    private final String bidFilesPath;//此次竞标文件根目录
    private final String amopFilePath;//此次竞标文件根目录
    private final String coreFilePath;//此次竞标文件根目录
    private final SBid contract;//合约
    Parameters parameters;//竞标参数
    Register register;//自己的注册信息
    private final int counts;//竞标人数
    private int totalRound;//竞标轮数
    boolean bigMe;//自己是否为编号较大的一方
    private Communicate publish;//发布AMOP
    private Communicate subscribe;//订阅AMOP
    private boolean autorun = false;

//  账号文件夹结构
//  .
//  ├── 账号A
//  │   ├── 投标1 bidFilesPath
//  │   │   ├── amop临时文件夹 amopFilePath
//  │   │   │   └── tmp1.txt
//  │   │   ├── core临时文件夹 coreFilePath
//  │   │   │   └── tmp.txt
//  │   │   └── parameters.txt
//  │   ├── 投标2
//  │   └── 投标历史文件.txt
//  └── 账号B

    public BidProcesser(Account account, String tenderName, String bidCode, String index, String amount) {
        this.account = account;
        this.tenderName = tenderName;
        this.bidCode = bidCode;
        this.index = index;
        this.amount = amount;
        this.contract = account.getContract();
        Table_SBid_Name = "Tender_" + Global.sha1(tenderName);
        Table_Register_Name = "Reg_" + bidCode;
        //创建文件夹
        this.bidFilesPath = account.getFilesPath() + Table_SBid_Name + "_" + bidCode + File.separator;
        Global.createFolder(bidFilesPath);
        amopFilePath = bidFilesPath + "amopFile" + File.separator;
        Global.createFolder(amopFilePath);
        coreFilePath = bidFilesPath + "coreFile" + File.separator;

        parameters = new Parameters(contract, Table_SBid_Name);
        parameters.setName(bidCode);
        parameters.read();
        this.counts = parameters.getCounts().intValue();
        //计算轮数
        if (counts < 2) {
            totalRound = 0;
        } else if (counts == 2)
            totalRound = 1;
        else {
            totalRound = (int) (Math.log(counts) / Math.log(2));
            if (Math.pow(2, totalRound) != counts) {
                ++totalRound;
            }
        }
        register = new Register(contract, Table_Register_Name, index, amopFilePath);
        register.read();
    }

    public boolean ready(long timestamp, boolean autorun) {
        long now = (new Date()).getTime();
        while (timestamp - now > 1000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            now = (new Date()).getTime();
        }
        System.out.println("Start at " + new Date());
        this.autorun = autorun;
        return sBid();
    }

    //竞标
    public boolean sBid() {
        StopWatch clock = new StopWatch("sBidBC");
        boolean loseFlag = false;
        //单轮竞标
        for (int round = 1; round <= totalRound; round++) {
//4.Connect  Opponent
            System.out.println("\n+++++ sBid round " + round + " +++++");

            ArrayList<Register> register_table = new ArrayList<>();
            Register opponent = new Register();
            boolean byeFlag = Global.loadRegInfo(register_table, counts, index, opponent, contract, Table_Register_Name, round);
            if (byeFlag) {
                System.out.println("Draw a bye");
                register.read();
                String updateResult = (round + 1) + "," + register.getLastFinishRound();
                register.update(updateResult);
                continue;
            }
            System.out.println("Opponent: " + opponent.getIndex() + "_" + opponent.getName());
            bigMe = Integer.parseInt(index) > Integer.parseInt(opponent.getIndex());

            // Creat amop connect
            clock.start("Round " + round + ": Connect peer");
            String publishTopic = bidCode + "_" + index + "_" + account.getPk();
            String subscribeTopic = bidCode + "_" + opponent.getIndex() + "_" + opponent.getAccountPk();
            System.out.println("publishTopic: " + publishTopic);
            publish = new Communicate(publishTopic, account.getBcosSDK());
            subscribe = new Communicate(subscribeTopic, account.getBcosSDK());

            publish.setIndexOp(opponent.getIndex());
            publish.setNameOp(opponent.getAccountPk());
            subscribe.subscribe(amopFilePath, index, account.getPk());

            String fileNameShak = bidFilesPath + "parameters.txt";

            // Shake hand
            if (bigMe) {
                publish.sendAmopFile(fileNameShak);
                subscribe.recvAmopFile(fileNameShak);
            } else {
                subscribe.recvAmopFile(fileNameShak);
                publish.sendAmopFile(fileNameShak);
            }
            System.out.println("Peer is connected");
            //start core -b
            clock.stop();
            clock.start("Round " + round + ": Core initialization");
            register.read();
            //启动核心
            String exePath = account.getRootPath() + "sBid";
            File rootDir = new File(bidFilesPath);
            List<String> params = new ArrayList<>();
            params.add(exePath);
            params.add("-b");
            params.add(index);
            params.add(opponent.getIndex());
            params.add(String.valueOf(round));
            params.add(register.getLastFinishRound());
            params.add(opponent.getLastFinishRound());
            params.add("0");//0:The lowest price wins; 1：The highest price wins
            ProcessBuilder processBuilder = new ProcessBuilder(params);
            processBuilder.directory(rootDir);
            Process process = null;
            int exitCode = -1;
            try {
                process = processBuilder.start();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            //connect core
            String host = "127.0.0.1";
            int port = 17000;//todo:端口动态变化
            port += Integer.parseInt(index);
            Socket client = null;
            try {
                Thread.sleep(500);
                client = new Socket(host, port);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if (client.isConnected()) {
                System.out.println("Core start");
            }
            publish.setClient(client);
            subscribe.setClient(client);
            //file up link
            ArrayList<String> fileList = new ArrayList<>();
//5.Pk exchange
            clock.stop();
            clock.start("Round " + round + ": sBid");
            String fileName = amopFilePath + "pk" + opponent.getIndex() + ".txt";
            Global.writeFile(fileName, opponent.getPublic_key() + "\n");
            publish.sendCoreFile(fileName);

            fileList.add(amopFilePath + "pk" + register.getIndex() + ".txt");
//6.Create cipher
            String fileNameSend = amopFilePath + "ciphertext" + index + "-R" + round + ".txt";
            String fileNameRecv = amopFilePath + "ciphertext" + opponent.getIndex() + "-R" + round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//7.Prove cipher
            fileNameSend = amopFilePath + "proveCipher" + index + "-R" + round + ".txt";
            fileNameRecv = amopFilePath + "proveCipher" + opponent.getIndex() + "-R" + round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//8.Prove cipher consistency
            if (round > 1) {//轮空可能没有前一轮的
                String provecipherFileNameSend = amopFilePath + "proveConsistency" + index + "-R" + round + ".txt";
                String provecipherFileNameRecv = amopFilePath + "proveConsistency" + opponent.getIndex() + "-R" + round + ".txt";
                boolean proveMe = register.getLastFinishRound().compareTo("0") != 0;
                boolean proveOp = opponent.getLastFinishRound().compareTo("0") != 0;
                if (proveMe) {
                    // core generate prove and recv prove file
                    fileList.add(provecipherFileNameSend);
                    publish.recvCoreFile(provecipherFileNameSend);
                }
                if (proveMe && proveOp) {
                    // Staggered reception
                    if (bigMe) {
                        publish.sendAmopFile(provecipherFileNameSend);
                        subscribe.recvAmopFile(provecipherFileNameRecv);
                    } else {
                        subscribe.recvAmopFile(provecipherFileNameRecv);
                        publish.sendAmopFile(provecipherFileNameSend);
                    }
                } else if (proveMe) {
                    publish.sendAmopFile(provecipherFileNameSend);
                } else if (proveOp) {
                    subscribe.recvAmopFile(provecipherFileNameRecv);
                }
                if (proveOp) {
                    //Download ciphertext generated by the opponent last Bid participation from the chain
                    String ciphertextLastFinishRoundFileName = "ciphertext" + opponent.getIndex() + "-R" + opponent.getLastFinishRound() + ".txt";
                    String ciphertextLastFinishRoundFilePath = amopFilePath + ciphertextLastFinishRoundFileName;
                    //fileZip_招标机构名_招标编号_Index_round.txt
                    String fileZipName = "fileZip_" + Table_SBid_Name + "_C" + bidCode + "_I" + opponent.getIndex() + "_R" + opponent.getLastFinishRound() + ".txt";
                    Files files = new Files(contract, opponent.getTable_files_name(), amopFilePath);
                    files.read(fileZipName);
                    Global.unzipFile(amopFilePath, files.getFile_content());
                    System.out.println("Download file " + fileZipName + " from " + opponent.getTable_files_name());
                    //send two file to core to verify
                    publish.sendCoreFile(ciphertextLastFinishRoundFilePath);
                    publish.sendCoreFile(provecipherFileNameRecv);
                }
            }
//9.Compare (BIG INDEX)
            if (bigMe) {
                fileNameSend = amopFilePath + "cipherCR" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = amopFilePath + "cipherCR" + opponent.getIndex() + "-R" + round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, bigMe);
            }
//10.Prove compare (BIG INDEX)
            if (bigMe) {
                fileNameSend = amopFilePath + "proveCompare" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = amopFilePath + "proveCompare" + opponent.getIndex() + "-R" + round + ".txt";
                amopSingle(fileNameRecv, bigMe);
            }
//11.Shuffle 1  (SMALL INDEX)
            if (!bigMe) {
                fileNameSend = amopFilePath + "cipherSR" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, !bigMe);
            } else {
                fileNameRecv = amopFilePath + "cipherSR" + opponent.getIndex() + "-R" + round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, !bigMe);
            }
//12.Shuffle 2 (BIG INDEX)
            if (bigMe) {
                fileNameSend = amopFilePath + "cipherSR" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = amopFilePath + "cipherSR" + opponent.getIndex() + "-R" + round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, bigMe);
            }
//13.Prove shuffle 1  (SMALL INDEX)
            if (!bigMe) {
                fileNameSend = amopFilePath + "proveShuffle" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, !bigMe);
            } else {
                fileNameRecv = amopFilePath + "proveShuffle" + opponent.getIndex() + "-R" + round + ".txt";
                amopSingle(fileNameRecv, !bigMe);
            }
//14.Prove Shuffle 2 (BIG INDEX)
            if (bigMe) {
                fileNameSend = amopFilePath + "proveShuffle" + index + "-R" + round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = amopFilePath + "proveShuffle" + opponent.getIndex() + "-R" + round + ".txt";
                amopSingle(fileNameRecv, bigMe);
            }
//15.Create dk
            fileNameSend = amopFilePath + "dk" + index + "-R" + round + ".txt";
            fileNameRecv = amopFilePath + "dk" + opponent.getIndex() + "-R" + round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//16.Prove dk
            fileNameSend = amopFilePath + "proveDecrypt" + index + "-R" + round + ".txt";
            fileNameRecv = amopFilePath + "proveDecrypt" + opponent.getIndex() + "-R" + round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
            exitCode = -1;
            try {
                exitCode = process.waitFor();
            } catch (java.lang.InterruptedException e) {
                e.printStackTrace();
            }
//17.Update reesult
            fileNameSend = amopFilePath + "ans" + index + "-R" + round + ".txt";
            publish.recvCoreFile(fileNameSend);
            fileList.add(fileNameSend);
            if (exitCode != 0) {
                System.err.println("Core error, exit (-b)");
                System.exit(-1);
            } else {
                System.out.println("Core finish");
            }
            ArrayList<String> ans = new ArrayList<>();
            if (!Global.readFile(fileNameSend, ans))
                return false;

//17.files upload
            //TODO: add signature
            clock.stop();
            clock.start("Round " + round + ": Upload");
            Files files = new Files(contract, register.getTable_files_name(), amopFilePath);
            StringBuilder fileZip = new StringBuilder();
            Global.zipFile(fileList, fileZip);
            fileZip.append("opponentIndex-R").append(round).append(".txt#").append(opponent.getIndex()).append("@");
            //证明文件
            String fileZipName = "fileZip_" + Table_SBid_Name + "_C" + bidCode + "_I" + index + "_R" + round + ".txt";//fileZip_招标机构名_招标名_Index_round.txt
            String fileZipPath = amopFilePath + fileZipName;
            Global.writeFile(fileZipPath, fileZip.toString());
            //签名
            String sigFileName = "fileZip_" + Table_SBid_Name + "_C" + bidCode + "_I" + index + "_R" + round + ".sig";
            String sigFilePath = amopFilePath + sigFileName;
            Global.writeFile(sigFilePath, Global.signature(fileZip.toString(), account.getCryptoKeyPair()));


            files.delete(fileZipName);//TODO: delete
            files.delete(sigFileName);

            files.insert(fileZipName);
            files.insert(sigFileName);

            System.out.println("Upload file \n" + fileZipName + "\nto\n" + register.getTable_files_name());
            clock.stop();
            subscribe.unsubscribe();

            if (ans.get(1).compareTo("WIN") != 0) {
                register.update("lose");
                loseFlag = true;
            } else {
                String updateResult = (round + 1) + "," + round;
                register.update(updateResult);
            }
            if (loseFlag) {
                System.out.println("\n+++++ You lose +++++\n");
                System.out.println("Opponent: " + opponent.getIndex() + "_" + opponent.getName());
                break;
            } else if (round == totalRound) {
                System.out.println("\n+++++ You win +++++\n");
                register.update("win");
                StringBuilder sk = new StringBuilder();
                if (!Global.readFile(amopFilePath + "sk" + index + ".txt", sk))
                    return false;
                parameters.update(index, sk.toString());
            }
        }
//        System.out.println("Amount: " + amount);
//        Global.readResult(counts, contract, Table_Register_Name);
        System.out.println("\n" + clock.prettyPrint());
        System.out.println("Auto Run: " + autorun);
        if (autorun){
            System.out.println("Auto Exit");
            System.exit(0);
        }
        return !loseFlag;
    }

    private void amop(String fileNameSend, String fileNameRecv) {
        if (bigMe) {
            //core recv and amop send
            System.out.println("recvCoreFile: " + fileNameSend);
            publish.recvCoreFile(fileNameSend);
            System.out.println("sendAmopFile: " + fileNameSend);
            publish.sendAmopFile(fileNameSend);
            //amop recv and core send
            System.out.println("recvAmopFile: " + fileNameRecv);
            subscribe.recvAmopFile(fileNameRecv);
            System.out.println("sendCoreFile: " + fileNameRecv);
            publish.sendCoreFile(fileNameRecv);
            System.out.println("AMOP OK");
        } else {
            System.out.println("recvAmopFile: " + fileNameRecv);
            subscribe.recvAmopFile(fileNameRecv);
            System.out.println("sendCoreFile: " + fileNameRecv);
            publish.sendCoreFile(fileNameRecv);
            System.out.println("recvCoreFile: " + fileNameSend);
            publish.recvCoreFile(fileNameSend);
            System.out.println("sendAmopFile: " + fileNameSend);
            publish.sendAmopFile(fileNameSend);
            System.out.println("AMOP OK");
        }
    }

    private void amopSingle(String fileName, boolean flag) {
        if (flag) {
            publish.recvCoreFile(fileName);
            publish.sendAmopFile(fileName);
        } else {
            subscribe.recvAmopFile(fileName);
            publish.sendCoreFile(fileName);
        }
    }

}
