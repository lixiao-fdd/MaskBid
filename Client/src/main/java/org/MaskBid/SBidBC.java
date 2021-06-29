package org.MaskBid;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceiptsDecoder;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceiptsInfo;
import org.fisco.bcos.sdk.client.protocol.response.TotalTransactionCount;
import org.fisco.bcos.sdk.model.TransactionReceipt;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SBidBC {
    private String accountName;
    private String realName;
    private String role;
    private String amount;
    private String index;
    private int counts = 2;
    private int totalRound = 0;
    private boolean bigMe;
    private String bidFilesPath;//竞标文件存放于此
    private String bidIndexFilesPath;//每轮竞标的文件存放于此
    private String bidVerifyFilesPath;//用于审计的文件存放于此
    private String bidDecryptFilesPath;//解密后的明文金额文件存放于此
    private String Table_Tender_Name;
    private String contractAddress;
    private String Table_SBid_Name = "";//招标机构名hash
    private String sBid_name = "";//招标名hash
    private SBid contract;
    private Account account;
    private Register register;
    private Tender tender = null;
    private Parameters parameters;
    private Communicate publish;
    private Communicate subscribe;
    private boolean bidStopFlag = false;
    private String bidDetailTenderName;
    private String bidDetailBidCode;
    private String bidTenderName;
    private String bidBidCode;
    private JSONObject storeJson = null;
    private final SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    private BigInteger topBlock = new BigInteger("0");

    //登录
    public SBidBC(String fileContentBase64, JSONObject data, String Table_Tender_Name, String contractAddress) {
        this.Table_Tender_Name = Table_Tender_Name;
        this.contractAddress = contractAddress;
        String fileContent = Global.baseDecode(fileContentBase64);
        String accountName = fileContent.substring(0, fileContent.lastIndexOf(":"));
        String temp = fileContent.substring(fileContent.lastIndexOf(":") + 1);
        String accountRole = temp.substring(0, temp.lastIndexOf("-"));
        String accountSK = temp.substring(temp.lastIndexOf("-") + 1);

        this.accountName = Global.sha1(accountName);
        this.realName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
        this.role = accountRole;
        boolean loginResult = loadAccount(accountSK);
        data.put("loginResult", loginResult);
        data.put("accountRole", accountRole);
        if (loginResult) {
            if (this.role.compareTo("0") == 0) {//招标者的招标表名
                Table_SBid_Name = "Tender_" + this.accountName;
                tenderReg();
            } else {//投标者加载历史投标表
                String jsonFilePath = account.getFilesPath() + "store.txt";
                File jsonFile = new File(jsonFilePath);
                if (!jsonFile.exists()) {
                    //将招标方名称，标的编号，加密私钥四个值，利用账号私钥作为密钥进行AES加密，然后存到本账号的存储账本中，条目名称为私钥的哈希
                    String Table_BidderFile_Name = "Bidder_" + Global.sha1(account.getPk());
                    String storeFileName = Global.sha1(account.getSk());
                    Files files = new Files(contract, Table_BidderFile_Name, "");
                    if (files.read(storeFileName)) {
                        //aes解密
                        String decrypted = null;
                        try {
                            decrypted = Global.decrypt(account.getSk(), files.getFile_content());
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                        }
                        storeJson = JSON.parseObject(decrypted);
                        System.out.println(storeJson);
                    }
                }
            }
        }
    }

    //注册合法性认证账户 登录
    public SBidBC(String accountName, String accountSK, String Table_Tender_Name, String contractAddress) {
        String accountNameAdmin = Global.sha1(accountName);
        //软件启动时及切换用户时调用
        Account accountAdmin = new Account(accountNameAdmin, contractAddress);
        if (!accountAdmin.connect()) {
            System.err.println("Node connect failed\r");
        }
        if (!accountAdmin.loadAccount(accountSK)) {
            System.err.println("LoadAccount failed");
        }
        contract = accountAdmin.getContract();
        Tender tenderAdmin = new Tender(contract, Table_Tender_Name, accountNameAdmin);
        if (!tenderAdmin.read()) {
            tenderAdmin.insert();
            System.out.println("tenderAdmin " + accountName + " has registered successfully");
        } else {
            System.out.println("tenderAdmin " + accountName + " already exists");
        }
    }

    //注册合法性认证
    public boolean isAccountExist(String newAccountName) {
        String accountNameTest = Global.sha1(newAccountName);
        Tender tenderTest = new Tender(contract, Table_Tender_Name, accountNameTest);
        return !tenderTest.read();
    }

    //加载账号
    public boolean loadAccount(String sk) {
        //软件启动时及切换用户时调用
        account = new Account(accountName, contractAddress);
        if (!account.connect()) {
            System.err.println("Node connect failed\r");
            return false;
        }
        if (!account.loadAccount(sk)) {
            System.err.println("LoadAccount failed");
            return false;
        }
        contract = account.getContract();
        return true;
    }

    //创建新的招标者项
    public void tenderReg() {
        tender = new Tender(contract, Table_Tender_Name, accountName);
        if (!tender.read()) {
            tender.insert();
            System.out.println("Tender " + accountName + " has registered successfully");
        } else {
            System.out.println("Tender " + accountName + " already exists");
        }
    }

    //读取招标者的标的表
    public boolean readBidTable(JSONObject json, String status, Tender tenderSearch) {
        if (!tenderSearch.read()) {
            return false;
        }
        String Table_SBid_Name_Temp = tenderSearch.getTable_sBid_name();
        System.out.println("Read bid table : Table_SBid_Name: " + Table_SBid_Name_Temp);
        Parameters bidTable = new Parameters(contract, Table_SBid_Name_Temp);
        int bidCounts = tenderSearch.getCounts();
        ArrayList<JSONObject> BidTableList = new ArrayList<>();
        for (int i = 0; i < bidCounts; i++) {
            String bidCodeTemp = Global.sha1(Table_SBid_Name_Temp + (i + 1));

            bidTable.setName(bidCodeTemp);
            if (bidTable.read()) {
                String dataEnd = bidTable.getDateEnd();
                String dataStart = bidTable.getDateStart();
                switch (status) {
                    case "ongoing" -> {
                        if (!Global.dateNowAfter(dataEnd)) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(bidTable.getBidName());
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", bidTable.getName());
                            BidTableItems.put("bidDate", bidTable.getDateEnd());
                            BidTableList.add(BidTableItems);
                        }
                    }
                    case "finish" -> {
                        if (Global.dateNowAfter(Global.dateCaculate(dataEnd, 1, "minutes"))) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(bidTable.getBidName());
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", bidTable.getName());
                            BidTableItems.put("bidCounts", bidTable.getCounts());
                            BidTableList.add(BidTableItems);
                        }
                    }
                    case "audit" -> {
                        if (Global.dateNowAfter(dataEnd) && bidTable.getCounts().intValue() > 1) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(bidTable.getBidName());
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", bidTable.getName());
                            BidTableItems.put("bidCounts", bidTable.getCounts());
                            BidTableList.add(BidTableItems);
                        }
                    }
                    case "bidder" -> {
                        if (!Global.dateNowAfter(dataEnd) && Global.dateNowAfter(dataStart)) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(bidTable.getBidName());
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", bidTable.getName());
                            BidTableItems.put("bidDate", bidTable.getDateEnd());
                            BidTableList.add(BidTableItems);
                        }
                    }
                }

            }
        }
        Collections.reverse(BidTableList);
        json.put("data", BidTableList);
        json.put("count", BidTableList.size());
        return true;
    }

    //投标者 读取已投标信息
    public boolean readBidderBidTable(JSONObject json, String status) {
        //读取json
        if (storeJson == null) {
            json.put("data", "");
            json.put("count", 0);
            return true;
        }
        JSONArray storeItemList = storeJson.getJSONArray("store");
        ArrayList<JSONObject> BidTableList = new ArrayList<>();
        for (Object storeItem : storeItemList) {
            JSONObject item = (JSONObject) storeItem;
            String bidTenderName = item.getString("tenderName");
            String bidBidCode = item.getString("bidCode");
            String bidAmount = item.getString("amount");
            parameters = new Parameters(contract, "Tender_" + Global.sha1(bidTenderName));
            parameters.setName(bidBidCode);
            if (parameters.read()) {
                String bidDate = parameters.getDateEnd();
                switch (status) {
                    case "ongoing" -> {
                        if (!Global.dateNowAfter(bidDate)) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(parameters.getBidName());
                            BidTableItems.put("tenderName", bidTenderName);
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", parameters.getName());
                            BidTableItems.put("bidDate", parameters.getDateEnd());
                            BidTableItems.put("bidAmount", bidAmount);
                            BidTableList.add(BidTableItems);
                        }
                    }
                    case "finish" -> {
                        if (Global.dateNowAfter(Global.dateCaculate(bidDate, 1, "minutes"))) {
                            JSONObject BidTableItems = new JSONObject();
                            String nameWithContent = Global.baseDecode(parameters.getBidName());
                            BidTableItems.put("tenderName", bidTenderName);
                            BidTableItems.put("bidName", nameWithContent.substring(0, nameWithContent.indexOf("\n")));
                            BidTableItems.put("bidCode", parameters.getName());
                            BidTableItems.put("bidAmount", bidAmount);
                            String winner = parameters.getWinner();
                            String myIndex = getMyIndex();
                            if (winner.compareTo(myIndex) == 0)
                                BidTableItems.put("bidResult", "中标");
                            else if (winner.compareTo("") == 0)
                                BidTableItems.put("bidResult", "流标");
                            else
                                BidTableItems.put("bidResult", "失败");
                            BidTableList.add(BidTableItems);
                        }
                    }
                }
            }
        }
        Collections.reverse(BidTableList);
        json.put("data", BidTableList);
        json.put("count", BidTableList.size());
        return true;
    }

    //投标者 读取指定招标者的标的表
    public boolean loadTenderBidList(String tenderName, JSONObject json, String act) {
        Tender tenderSearch = new Tender(contract, Table_Tender_Name, Global.sha1(tenderName));
        switch (act) {
            case "bidder" -> {
                if (readBidTable(json, "bidder", tenderSearch)) {
                    return true;
                }
            }
            case "audit" -> {
                if (readBidTable(json, "audit", tenderSearch)) {
                    return true;
                }
            }
            default -> {
                System.err.println("unknown: " + act);
            }
        }
        json.replace("code", 1);
        json.replace("msg", "招标方\"" + tenderName + "\"不存在");
        json.put("count", 0);
        json.put("data", "");
        return false;
    }

    //读取某次招标的详细信息
    public boolean loadBidDetail(String bidDetailTenderName, String bidDetailBidCode, int searchRole, JSONObject data) {
        this.bidDetailTenderName = bidDetailTenderName;
        this.bidDetailBidCode = bidDetailBidCode;
        parameters = new Parameters(contract, "Tender_" + Global.sha1(bidDetailTenderName));
        parameters.setName(bidDetailBidCode);
        parameters.read();
        //招标基础信息

        data.put("bidCode", parameters.getName());
        StringBuilder bidNameSB = new StringBuilder();
        StringBuilder bidContentSB = new StringBuilder();
        Global.getBidNameContent(parameters.getBidName(), bidNameSB, bidContentSB);
        switch (searchRole) {
            case 0 -> {//招标者
                data.put("bidName", bidNameSB.toString());
                data.put("bidContent", bidContentSB.toString());
                data.put("bidCounts", parameters.getCounts());
                data.put("bidDateStart", parameters.getDateStart());
                data.put("bidDateEnd", parameters.getDateEnd());
                boolean isBidFinish = Global.dateNowAfter(Global.dateCaculate(parameters.getDateEnd(), 1, "minutes"));
                data.put("bidStatus", isBidFinish ? "已结束" : "进行中");
                if (isBidFinish) {//已结束
                    if (parameters.getCounts().intValue() > 1) {
                        //未流标
                        //读取sk和winner，并读取winner的cipher_Amount，进行解密
                        String winner = parameters.getWinner();
                        Register winnerRegInfo = new Register(contract, parameters.getTable_register_name(), winner, "");
                        winnerRegInfo.read();
                        String winnerCipherAmount = winnerRegInfo.getCipher_amount();//为空
                        String sk = parameters.getSk();
                        String mod = parameters.getP().toString();
                        if (sk.length() == 0 || winnerCipherAmount.length() == 0) {
                            data.put("bidAmount", "流标");
                            data.replace("bidStatus", "流标");
                        } else {
                            createBidFileFolder();
                            createDecryptFileFolder();
                            //调用c++进行解密
                            //启动核心
                            String exePath = account.getRootPath() + "sBid";
                            File rootDir = new File(bidFilesPath);
                            List<String> params = new ArrayList<>();
                            params.add(exePath);
                            params.add("-d");
                            params.add(winnerCipherAmount);
                            params.add(sk);
                            params.add(mod);
                            System.out.println(params);
                            ProcessBuilder processBuilder = new ProcessBuilder(params);
                            processBuilder.directory(rootDir);
                            int exitCode = -1;
                            Process process = null;
                            try {
                                process = processBuilder.start();
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            //String host = "192.168.1.121";
                            String host = "127.0.0.1";
                            int port = 17000;
                            Socket client = null;
                            try {
                                Thread.sleep(500);
                                client = new Socket(host, port);
                                if (client.isConnected()) {
                                    System.out.println("Core start");
                                }
                                Communicate communicate = new Communicate("", account.getBcosSDK());
                                communicate.setClient(client);
                                communicate.recvCoreFile(bidDecryptFilesPath + "plaintextAmount.txt");
                                communicate.getClient().close();
                                exitCode = process.waitFor();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            if (exitCode != 0) {
                                System.err.println("Core error, exit (-d)");
                                System.exit(-1);
                            } else {
                                System.out.println("Core finish");
                            }
                            StringBuilder amount = new StringBuilder();
                            if (!Global.readFile(bidDecryptFilesPath + "plaintextAmount.txt", amount))
                                return false;
                            data.put("bidAmount", amount);
                        }
                    } else {
                        data.put("bidAmount", "流标");
                        data.replace("bidStatus", "流标");
                    }
                } else
                    data.put("bidAmount", "等待中");
            }
            case 1 -> {//投标者
                data.put("bidInfoTenderName", bidDetailTenderName);
                data.put("bidInfoBidCode", bidDetailBidCode);
                data.put("bidInfoBidName", bidNameSB.toString());
                data.put("bidInfoBidContent", bidContentSB.toString());
                data.put("bidInfoBidDateStart", parameters.getDateStart());
                data.put("bidInfoBidDateEnd", parameters.getDateEnd());
            }
            case 2 -> {//审计
                if (parameters.getCounts().intValue() > 1 && Global.dateNowAfter(Global.dateCaculate(parameters.getDateEnd(), 1, "minutes"))) {
                    data.put("auditInfoTenderName", bidDetailTenderName);
                    data.put("auditInfoBidName", bidNameSB.toString());
                    data.put("auditInfoBidContent", bidContentSB.toString());
                    data.put("auditInfoBidCode", bidDetailBidCode);
                    data.put("auditInfoBidCounts", parameters.getCounts());
                    data.put("auditInfoBidResult", "正在审计");
                    data.put("auditable", true);
                } else
                    data.put("auditable", false);
            }
        }
        return true;
    }

    //加载竞标开始时间
    public String loadBidEndDate(String bidDetailTenderNameTemp, String bidDetailBidCodeTemp) {
        Parameters parametersTemp = new Parameters(contract, "Tender_" + Global.sha1(bidDetailTenderNameTemp));
        parametersTemp.setName(bidDetailBidCodeTemp);
        parametersTemp.read();
        return parametersTemp.getDateEnd();
    }

    //新建一个招标
    public boolean postNewBid(String bidName, String content, String dateStart, String dateEnd) {
        //将招标名称与招标内容打包(base64)
        String bidBase64 = Base64.getEncoder().encodeToString((bidName + "\n" + content).getBytes(StandardCharsets.UTF_8));
        //生成bidCode(sha1)
        assert tender != null;
        int bidCounts = tender.getCounts();
        Table_SBid_Name = tender.getTable_sBid_name();
        sBid_name = Global.sha1(Table_SBid_Name + (bidCounts + 1));
        //创建文件夹
        createBidFileFolder();
        //调用c++生成parameters.txt
        //启动核心
        String exePath = account.getRootPath() + "sBid";
        File rootDir = new File(account.getRootPath());
        List<String> params = new ArrayList<>();
        params.add(exePath);
        params.add("-g");
        ProcessBuilder processBuilder = new ProcessBuilder(params);
        processBuilder.directory(rootDir);
        int exitCode = -1;
        Process process = null;
        try {
            process = processBuilder.start();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //String host = "192.168.1.121";
        String host = "127.0.0.1";
        int port = 17000;
        Socket client = null;
        try {
            Thread.sleep(500);
            client = new Socket(host, port);
            if (client.isConnected()) {
                System.out.println("Core start");
            }
            Communicate communicate = new Communicate("", account.getBcosSDK());
            communicate.setClient(client);
            communicate.recvCoreFile(bidFilesPath + "parameters.txt");
            communicate.getClient().close();
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Core error, exit (-g)");
            System.exit(1);
        }
        if (exitCode != 0) {
            System.err.println("Core error, exit (-g)");
            System.exit(-1);
        } else {
            System.out.println("Core finish");
        }

        // Load parameters.txt
        String parametersFilePath = bidFilesPath + "parameters.txt";
        File parametersFile = new File(parametersFilePath);
        ArrayList<String> fileContent = new ArrayList<>();
        if (!Global.readFile(parametersFilePath, fileContent))
            return false;
        String p = fileContent.get(0);
        String q = fileContent.get(1);
        String h = fileContent.get(2);
        String g = fileContent.get(3);
        //创建招标表
        Parameters parameters = new Parameters(contract, Table_SBid_Name);
        String[] paras_value = {"0", p, q, h, g, dateStart, dateEnd, bidBase64};
        parameters.setName(sBid_name);
        if (parameters.insert(paras_value)) {
            //招标机构招标数加一
            tender.add();
            bidCounts = tender.getCounts();
            System.out.println("Created a new Bid");
        }
        System.out.println("@".repeat(30));
        System.out.println("TenderName: " + Table_SBid_Name);
        System.out.println("Bid counts: " + bidCounts);
        System.out.println("BidCode: " + sBid_name);
        System.out.println("@".repeat(30));
        return true;
    }

    //投标者 提交投标金额
    public boolean postAmount(String bidTenderName, String bidBidCode, String bidAmount) {
        this.bidTenderName = bidTenderName;
        this.bidBidCode = bidBidCode;
        parametersRead("Tender_" + Global.sha1(bidTenderName), bidBidCode);
        index = getEmptyIndex();
        if (index.compareTo("-1") == 0) {
            return false;
        }
        createBidIndexFileFolder();
        Global.writeFile(bidIndexFilesPath + "plaintext_int" + index + ".txt", bidAmount);
        //启动核心
        String exePath = account.getRootPath() + "sBid";
        File rootDir = new File(bidFilesPath);
        List<String> params = new ArrayList<>();
        params.add(exePath);
        params.add("-r");
        params.add(index);
        ProcessBuilder processBuilder = new ProcessBuilder(params);
        processBuilder.directory(rootDir);
        int exitCode = -1;
        Process process = null;
        try {
            process = processBuilder.start();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //String host = "192.168.1.121";
        String host = "127.0.0.1";
        int port = 17000;
        port += Integer.parseInt(index);
        Socket client = null;
        try {
            Thread.sleep(500);
            client = new Socket(host, port);
            if (client.isConnected()) {
                System.out.println("Core start");
            }
            Communicate communicate = new Communicate("", account.getBcosSDK());
            communicate.setClient(client);
            communicate.sendCoreFile(bidFilesPath + "parameters.txt");
            communicate.sendCoreFile(bidIndexFilesPath + "plaintext_int" + index + ".txt");
            communicate.recvCoreFile(bidIndexFilesPath + "pk" + index + ".txt");
            communicate.recvCoreFile(bidIndexFilesPath + "sk" + index + ".txt");
            communicate.recvCoreFile(bidIndexFilesPath + "cipherAmount" + index + ".txt");
            communicate.getClient().close();
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (exitCode != 0) {
            System.err.println("Core error, exit (-r)");
            System.exit(-1);
        } else {
            System.out.println("Core finish");
        }
        register = new Register(contract, parameters.getTable_register_name(), index, bidIndexFilesPath);
        if (register.insert(realName, account.getPk())) {
            System.out.println("Registrate successes");
        } else {
            System.out.println("Registrate failed");
            return false;
        }

        JSONArray storeItemList = null;
        if (storeJson == null) {
            storeJson = new JSONObject();
            storeItemList = new JSONArray();
            storeJson.put("store", storeItemList);
        } else {
            storeItemList = storeJson.getJSONArray("store");
        }
        //新的条目
        JSONObject storeItem = new JSONObject();
        storeItem.put("tenderName", bidTenderName);
        storeItem.put("bidCode", bidBidCode);
        storeItem.put("amount", bidAmount);
        StringBuilder skSB = new StringBuilder();
        Global.readFile(bidIndexFilesPath + "sk" + index + ".txt", skSB);
        storeItem.put("sk", skSB.toString());
        //插入json并写入文件
        storeItemList.add(storeItem);
        storeJson.replace("store", storeItemList);

        //更新链上数据
        String Table_BidderFile_Name = "Bidder_" + Global.sha1(account.getPk());
        String storeFileName = Global.sha1(account.getSk());
        String storeFilePath = account.getFilesPath() + storeFileName;
        //todo:加密
        String hexStr = null;
        try {
            hexStr = Global.encrypt(account.getSk(), storeJson.toJSONString());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        Global.writeFile(storeFilePath, hexStr);
        //上传
        Files files = new Files(contract, Table_BidderFile_Name, account.getFilesPath());
        files.delete(storeFileName);
        files.insert(storeFileName);
        File tempFile = new File(storeFilePath);
        if (!tempFile.delete()) {
            System.out.println(tempFile.getAbsolutePath() + " 删除失败");
        }
        //todo:将招标方名称，标的编号，加密私钥四个值，利用账号私钥作为密钥进行AES加密，然后存到本账号的存储账本中，条目名称为私钥的哈希
        return true;
    }

    //读取竞标参数
    public boolean parametersRead(String tenderTableName, String bidCode) {
        //创建文件夹
//        System.out.println("*".repeat(30));
//        System.out.println("* TenderTableName: " + tenderTableName);
//        System.out.println("* BidCode: " + bidCode);
//        System.out.println("*".repeat(30));
        this.sBid_name = bidCode;//本次招标在招标机构招标表中的名称
        this.Table_SBid_Name = tenderTableName;
        parameters = new Parameters(contract, tenderTableName);
        parameters.setName(bidCode);
        if (!parameters.read()) {
            System.err.println("Bidding does not exist");
            return false;
        }
        ArrayList<String> content = new ArrayList<>();
        content.add(parameters.getP().toString());
        content.add(parameters.getQ().toString());
        content.add(parameters.getH().toString());
        content.add(parameters.getG().toString());
        createBidFileFolder();
        Global.writeFile(bidFilesPath + "parameters.txt", content);
//        counts = parameters.getCounts().intValue();
        return true;
    }

    //创建投标文件夹
    private void createBidFileFolder() {
        bidFilesPath = account.getFilesPath() + Table_SBid_Name + "_" + sBid_name + File.separator;
        Global.createFolder(bidFilesPath);
    }

    //创建amop文件夹
    private void createBidIndexFileFolder() {
        bidIndexFilesPath = bidFilesPath + "amopFile" + File.separator;
        Global.createFolder(bidIndexFilesPath);
    }

    //创建verify文件夹
    private void createVerifyFileFolder() {
        bidVerifyFilesPath = bidFilesPath + "Verify" + File.separator;
        Global.createFolder(bidVerifyFilesPath);
    }

    //创建decrypt文件夹
    private void createDecryptFileFolder() {
        bidDecryptFilesPath = bidFilesPath + "Decrypt" + File.separator;
        Global.createFolder(bidDecryptFilesPath);
    }

    //遍历竞标注册表，返回第一个未被占用的编号
    public String getEmptyIndex() {
        int i = 1;
        while (true) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                break;
            if (register.getName().compareTo(realName) == 0)
                return "-1";
            ++i;
        }
        return String.valueOf(i);
    }

    //遍历竞标注册表，返回自己的编号
    public String getMyIndex() {
        int i = 1;
        while (true) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                return "-1";
            if (register.getName().compareTo(realName) == 0)
                break;
            ++i;
        }
        return String.valueOf(i);
    }

    //读取注册表
    public void getRegInfo(JSONObject json) {
        parameters = new Parameters(contract, "Tender_" + Global.sha1(bidDetailTenderName));
        parameters.setName(bidDetailBidCode);
        parameters.read();
        counts = parameters.getCounts().intValue();
        boolean isBidFinish = Global.dateNowAfter(parameters.getDateEnd());
        ArrayList<JSONObject> info = new ArrayList<>();
        for (int i = 0; i <= counts; i++) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                continue;
            JSONObject infoItem = new JSONObject();
            infoItem.put("bidderIndex", register.getIndex());
            infoItem.put("bidderName", Global.baseDecode(register.getName()));
            infoItem.put("bidderPk", register.getAccountPk());
            if (isBidFinish)
                infoItem.put("bidderResults", (register.getResult().compareTo("win") == 0) ? "中标" : "失败");
            else
                infoItem.put("bidderResults", "等待竞标");
            info.add(infoItem);
        }
        json.put("count", counts);
        json.put("data", info);
    }

    //区块链浏览器
    public void getChainInfo(JSONObject json) {
        Client client = account.getClient();
        TotalTransactionCount.TransactionCountInfo totalTransactionCount = client.getTotalTransactionCount().getTotalTransactionCount();
        // 获取最新区块高度
        String blockNumberStr = totalTransactionCount.getBlockNumber();
        BigInteger blockNumber = new BigInteger(blockNumberStr.substring(2), 16);
        json.put("blockNumber", blockNumber);
        // 获取上链的交易总量
        String txSum = totalTransactionCount.getTxSum();
        json.put("txSum", Integer.parseInt(txSum.substring(2), 16));
        // 获取上链执行异常的交易总量
        String failedTxSum = totalTransactionCount.getFailedTxSum();
        json.put("failedTxSum", Integer.parseInt(failedTxSum.substring(2), 16));
        // 获取节点数量
        json.put("nodeCounts", client.getPeers().getPeers().size() + 1);
        // 获取顶部新产生的区块
        ArrayList<JSONObject> blockList = new ArrayList<>();
        ArrayList<JSONObject> transList = new ArrayList<>();
        int blockCount = blockNumber.intValue();
        int newBlockCount;
        if (topBlock.compareTo(new BigInteger("0")) == 0 && blockCount > 10) {
            newBlockCount = 10;
        } else
            newBlockCount = blockNumber.subtract(topBlock).intValue();
        for (int i = 0; i < newBlockCount; i++) {
            JSONObject jsonItem = new JSONObject();
            //区块
            BcosBlock.Block blockItem = client.getBlockByNumber(blockNumber.subtract(BigInteger.valueOf(i)), true).getBlock();
            String timestampStr = blockItem.getTimestamp();
            long timestamp = Long.parseLong(timestampStr.substring(2), 16);
            //区块高度
            jsonItem.put("number", blockItem.getNumber());
            //生成时间
            String blockDate = ft.format(new Date(timestamp));
            jsonItem.put("date", blockDate);
            //区块哈希
            jsonItem.put("hash", blockItem.getHash());
            //交易数量
            jsonItem.put("transCounts", blockItem.getTransactions().size());

            // 获取最新区块高度的所有交易回执信息
            BcosTransactionReceiptsDecoder bcosTransactionReceiptsDecoder =
                    client.getBatchReceiptsByBlockNumberAndRange(
                            blockItem.getNumber(), "0", "-1");
            // 解码交易回执信息
            BcosTransactionReceiptsInfo.TransactionReceiptsInfo receiptsInfo = bcosTransactionReceiptsDecoder.decodeTransactionReceiptsInfo();
            // 获取交易回执列表
            List<TransactionReceipt> receiptList = receiptsInfo.getTransactionReceipts();
            for (TransactionReceipt x : receiptList) {
                JSONObject transItem = new JSONObject();
                transItem.put("from", x.getFrom());
                transItem.put("to", x.getTo());
                transItem.put("hash", x.getTransactionHash());
                transItem.put("date", blockDate);
                transList.add(transItem);
            }
            blockList.add(jsonItem);
        }

        json.put("topTrans", transList);
        json.put("topBlock", blockList);
        topBlock = blockNumber;
    }

    public String getName() {
        return accountName;
    }

    public String getRealName() {
        return realName;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public Account getAccount() {
        return account;
    }

    public Register getRegister() {
        return register;
    }

    public void setTable_SBid_Name(String table_SBid_Name) {
        Table_SBid_Name = table_SBid_Name;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public boolean getBidFinishStatus(String auditTenderName, String auditBidCode) {
        Parameters parametersTemp = new Parameters(contract, "Tender_" + Global.sha1(auditTenderName));
        parametersTemp.setName(auditBidCode);
        parametersTemp.read();
        return parametersTemp.getWinner().length() > 0;//winner项为空则竞标未结束，返回false
    }

    public String getsBid_name() {
        return sBid_name;
    }

    public void stopBid() {
        bidStopFlag = true;
    }

    public void startBid() {
        bidStopFlag = false;
    }

    public String getRole() {
        return role;
    }

    public String getBidAmount(String bidCode) {
        //读取json
        StringBuilder jsonFileSB = new StringBuilder();
        String jsonFilePath = account.getFilesPath() + "store.txt";
        File jsonFile = new File(jsonFilePath);
        JSONObject storeFile = null;
        JSONArray storeItemList = null;
        if (jsonFile.exists()) {
            Global.readFile(account.getFilesPath() + "store.txt", jsonFileSB);
            storeFile = JSONObject.parseObject(jsonFileSB.toString());
            storeItemList = storeFile.getJSONArray("store");
            for (Object storeItem : storeItemList) {
                JSONObject item = (JSONObject) storeItem;
                String bidBidCode = item.getString("bidCode");
                if (bidBidCode.compareTo(bidCode) == 0)
                    return item.getString("amount");
            }
        }
        return null;
    }

    public String getBidRootPath(String tenderName, String bidCode) {
        return account.getFilesPath() + "Tender_" + Global.sha1(tenderName) + "_" + bidCode + File.separator;
    }

    public JSONObject getStoreJson() {
        return storeJson;
    }

    public Tender getTender() {
        return tender;
    }
}
