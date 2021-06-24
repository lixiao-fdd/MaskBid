package org.sBid;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceiptsDecoder;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceiptsInfo;
import org.fisco.bcos.sdk.client.protocol.response.TotalTransactionCount;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    private String Table_Tender_Name = "tenderTest30";
    private String contractAddress = "0x4a3234be1003513b8c1765bc283f3740efd8f9de";
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

    //旧 构造函数
    public SBidBC(String accountName) {
        role = accountName.substring(accountName.lastIndexOf(":") + 1);
        this.accountName = Global.sha1(accountName);
        this.realName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
//        this.accountName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
        if (role.compareTo("0") == 0) {//招标者的招标表名
            Table_SBid_Name = "Tender_" + this.accountName;
        }
    }

    //注册
    public SBidBC(String newAccountName, String newAccountRole, StringBuilder mbkFile) {
        this.role = newAccountRole;
        this.accountName = Global.sha1(newAccountName);
        this.realName = Base64.getEncoder().encodeToString(newAccountName.getBytes(StandardCharsets.UTF_8));
        String accountSk = createAccount();
        loadAccount(accountSk);
        if (this.role.compareTo("0") == 0) {//招标者的招标表名
            Table_SBid_Name = "Tender_" + this.accountName;
            tenderReg();
        }
        mbkFile.append(Base64.getEncoder().encodeToString((newAccountName + ":" + newAccountRole + "-" + accountSk).getBytes(StandardCharsets.UTF_8)));
    }

    //登录
    public SBidBC(String fileContentBase64, JSONObject data) {
        String fileContent = Global.baseDecode(fileContentBase64);
        String accountName = fileContent.substring(0, fileContent.lastIndexOf(":"));
        String temp = fileContent.substring(fileContent.lastIndexOf(":") + 1);
        String accountRole = temp.substring(0, temp.lastIndexOf("-"));
        String accountSK = temp.substring(temp.lastIndexOf("-") + 1);

        this.accountName = Global.sha1(accountName);
        this.realName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
        this.role = accountRole;
        data.put("loginResult", loadAccount(accountSK));
        data.put("accountRole", accountRole);
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

    //注册合法性认证账户 登录
    public SBidBC(String accountName, String accountSK) {
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
    /*竞拍者流程：
     * 1、启动软件，登录，读取账号
     * 2、连接节点
     * 3、确定竞拍对象
     * 4、输入金额
     * 5、读取竞拍参数
     * 6、竞拍注册
     * 7、等待开始
     * 8、开始竞拍流程
     * */

    //生成公私钥对
    public String createAccount() {
        Account accountTemp = new Account(accountName, contractAddress);
        return accountTemp.createAccount();
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

    //旧 加载账号
    public boolean loadAccount() {
        account = new Account(accountName, contractAddress);
        System.out.println("Node is connecting\r");
        if (!account.connect()) {
            return false;
        }
        System.out.println("Node is connected ");
        if (!account.loadAccount()) {
            return false;
        }
        //get sBid contract
        contract = account.getContract();
        return true;
    }

    //初始化竞标参数
    //key : name, field : counts, p, q, h, g, !【winner】, !【sk】, 【dateStart】, 【dateEnd】, 【bidName】, table_register_name
    //name和table_register_name由bidName生成，pqhg调用c++程序生成

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
                if (readBidTable(json, "ongoing", tenderSearch)) {
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

    //旧 读取招标者的招标表内容
    public boolean readBidTable(JSONObject json) {
        tenderReg();
        tender.read();
        Table_SBid_Name = tender.getTable_sBid_name();
        Parameters bidTable = new Parameters(contract, Table_SBid_Name);
        int bidCounts = tender.getCounts();
        ArrayList<JSONObject> BidTableList = new ArrayList<>();
        for (int i = 0; i < bidCounts; i++) {
            String bidCodeTemp = Global.sha1(Table_SBid_Name + (i + 1));
            bidTable.setName(bidCodeTemp);
            if (!bidTable.read()) {
                continue;
            }
            JSONObject BidTableItems = new JSONObject();
            BidTableItems.put("bidCode", bidTable.getName());
            BidTableItems.put("bidName", Global.baseDecode(bidTable.getBidName()));
            BidTableItems.put("bidable", Boolean.toString(bidTable.getWinner().compareTo("") == 0));
            BidTableList.add(BidTableItems);
        }
        json.put("BidTableList", BidTableList);
        return true;
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

    public String loadBidEndDate(String bidDetailTenderNameTemp, String bidDetailBidCodeTemp) {
        Parameters parametersTemp = new Parameters(contract, "Tender_" + Global.sha1(bidDetailTenderNameTemp));
        parametersTemp.setName(bidDetailBidCodeTemp);
        parametersTemp.read();
        return parametersTemp.getDateEnd();
    }

    //旧 读取某次招标的详细信息
    public boolean readBidDetail(JSONObject json, String bidCode) {
        parameters = new Parameters(contract, Table_SBid_Name);
        parameters.setName(bidCode);
        parameters.read();
        //招标基础信息
        JSONObject bidInfo = new JSONObject();
        bidInfo.put("bidCode", parameters.getName());
        bidInfo.put("dateStart", parameters.getDateStart());
        bidInfo.put("dateEnd", parameters.getDateEnd());
        bidInfo.put("bidable", Boolean.toString(parameters.getWinner().compareTo("") == 0));
        bidInfo.put("bidCounts", parameters.getCounts());
        bidInfo.put("bidName", Global.baseDecode(parameters.getBidName()));
        json.put("bidInfo", bidInfo);
        //招标注册表
        getRegistrationInfo(json);
        //如果招标已结束，显示最终金额
        boolean isBidFinished = getBidFinishStatus();
        json.put("isBidFinished", isBidFinished);
        if (isBidFinished) {
            //读取sk和winner，并读取winner的cipher_Amount，进行解密
            String winner = parameters.getWinner();
            Register winnerRegInfo = new Register(contract, parameters.getTable_register_name(), winner, "");
            winnerRegInfo.read();
            String winnerCipherAmount = winnerRegInfo.getCipher_amount();//为空
            String sk = parameters.getSk();
            String mod = parameters.getP().toString();

            createDecryptFileFolder();
            //调用c++进行解密
            //启动核心
            String exePath = account.getRootPath() + "sBid";
            File rootDir = new File(account.getRootPath());
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
                communicate.recvCoreFile(bidDecryptFilesPath + "plaintextAmount" + winner + ".txt");
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
            if (!Global.readFile(bidDecryptFilesPath + "plaintextAmount" + winner + ".txt", amount))
                return false;
            json.put("amount", amount);
        } else {
            //若全都在准备状态则可选择开始
        }
        return true;
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

    //旧 新建一个招标
    public boolean parametersInit(String bidName, String counts, String dateStart, String dateEnd) {
        //生成bidName(base64)
        String bidNameBase64 = Base64.getEncoder().encodeToString(bidName.getBytes(StandardCharsets.UTF_8));//base64处理后的招标名，可恢复
        //生成name(sha1)
        int bidCounts = tender.getCounts();
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
//        //生成发布日期
//        Date date = new Date();
//        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd_hh:mm");
//        String dateStart = ft.format(date);
        //创建招标表
        Parameters parameters = new Parameters(contract, Table_SBid_Name);
        String[] paras_value = {String.valueOf(counts), p, q, h, g, dateStart, dateEnd, bidNameBase64};
        parameters.setName(sBid_name);
        if (parameters.insert(paras_value)) {
            //招标机构招标数加一
            tender.add();
            bidCounts = tender.getCounts();
            System.out.println("Created a new Bid");
        }
        System.out.println("@".repeat(30));
        System.out.println("Bid counts 2: " + bidCounts);
        System.out.println("TenderName: " + Table_SBid_Name);
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

    public void allDel() {
        //删除tender注册表
        tender = new Tender(contract, Table_Tender_Name, accountName);
        tender.read();
        Table_SBid_Name = tender.getTable_sBid_name();
        Parameters bidTable = new Parameters(contract, Table_SBid_Name);
        int bidCounts = tender.getCounts();
        for (int i = 0; i < bidCounts; i++) {
            String bidCodeTemp = Global.sha1(Table_SBid_Name + (i + 1));
            bidTable.setName(bidCodeTemp);
            if (!bidTable.read()) {
                continue;
            }
            System.out.println("Reset register " + bidTable.getTable_register_name());
            for (int j = 1; j <= bidTable.getCounts().intValue(); j++) {
                Register register = new Register(contract, bidTable.getTable_register_name(), String.valueOf(j), "");
                if (!register.delete())
                    continue;
                System.out.print("No." + j + " ");
            }
            System.out.println(" OK");
            bidTable.delete();
            tender.delete();//delete
            System.out.println("reset Tender");
        }
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

    //设置金额
    public void setAmount(String amount) {
        this.amount = amount;
        System.out.println("Amount: " + amount);
    }

    //获取金额
    public String getAmount() {
        bidFilesPath = account.getFilesPath() + Table_SBid_Name + "_" + sBid_name + File.separator;
        bidIndexFilesPath = bidFilesPath + "index_" + index + File.separator;
        String fileName = bidIndexFilesPath + "plaintext_int" + getIndex(realName) + ".txt";
        StringBuilder stringBuilder = new StringBuilder();
        if (!Global.readFile(fileName, stringBuilder))
            return "unknown";
        return stringBuilder.toString();
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

    //旧 遍历竞标注册表，返回第一个未被占用的编号
    private String getIndex() {
        int i = 1;
        for (; i <= counts; i++) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                break;
            if (register.getName().compareTo(realName) == 0)
                return String.valueOf(-2);
        }
        if (i > counts)
            return String.valueOf(-1);
        index = String.valueOf(i);
        return index;
    }

    //遍历竞标注册表，找到对应名字的编号
    public String getIndex(String bidderName) {
        for (int i = 1; i <= counts; i++) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                continue;
            if (register.getName().compareTo(bidderName) == 0) {
                index = String.valueOf(i);
                return index;
            }
        }
        return String.valueOf(-1);
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
        int newBlockCount;
        if (topBlock.compareTo(new BigInteger("0")) == 0)
            newBlockCount = 10;
        else
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

    //旧 读取注册表
    public void getRegistrationInfo(JSONObject json) {
        counts = parameters.getCounts().intValue();
        ArrayList<List<String>> info = new ArrayList<>();
        System.out.println("getRegistrationInfo: " + parameters.getTable_register_name());
        for (int i = 0; i <= counts; i++) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (!register.read())
                continue;
            List<String> temp = List.of(register.getIndex(), Global.baseDecode(register.getName()), register.getAccountPk(), register.getResult());
            info.add(temp);
        }
        json.put("registrationInfo", info);
    }

    //读取注册表
    public int getRegistrationCounts() {
        int j = 0;
        for (int i = 0; i <= counts; i++) {
            Register register = new Register(contract, parameters.getTable_register_name(), String.valueOf(i), "");
            if (register.read()) {
                ++j;
            }
        }
        return j;
    }

    //旧 链上注册
    public boolean registration(StringBuilder status) {
        index = getIndex();
        if (index.compareTo("-1") == 0) {
            status.append("投标人数已满");
            return false;
        } else if (index.compareTo("-2") == 0) {
            status.append("重复投标");
            return false;
        }
        createBidIndexFileFolder();
        Global.writeFile(bidIndexFilesPath + "plaintext_int" + index + ".txt", amount);

        //启动核心
        String exePath = account.getRootPath() + "sBid";
        File rootDir = new File(account.getRootPath());
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
            status.append("投标失败，请重试");
            return false;
        }
        status.append("投标成功");
        return true;
    }

    //撤回注册
    public void withdrawRegistration() {
        index = getIndex(realName);
        System.out.println("delete: " + index);
        if (index.compareTo("-1") == 0) {
            System.out.println("撤回失败");
            return;
        }
        register = new Register(contract, parameters.getTable_register_name(), index, bidIndexFilesPath);
        register.delete();

        System.out.println("撤回成功");
    }

    //竞标
    public boolean sBid() {
        StopWatch clock = new StopWatch("sBidBC");
        if (register == null)
            register = new Register(contract, parameters.getTable_register_name(), index, bidIndexFilesPath);
        boolean loseFlag = false;
        //单轮竞标
        for (Global.round = 1; Global.round <= totalRound; Global.round++) {
//4.Connect  Opponent
            System.out.println("\n+++++ sBid round " + Global.round + " +++++");

            ArrayList<Register> register_table = new ArrayList<>();
            Register opponent = new Register();
            boolean byeFlag = Global.readRegInfo(register_table, parameters.getCounts().intValue(), index, opponent, contract, parameters.getTable_register_name());
            if (byeFlag) {
                System.out.println("Draw a bye");
                register.read();
                String updateResult = (Global.round + 1) + "," + register.getLastFinishRound();
                register.update(updateResult);
//                System.out.println("updateResult: " + updateResult);
//                register.update(String.valueOf(Global.round));
                continue;
            }
            System.out.println("Opponent: " + opponent.getIndex() + "_" + opponent.getName());
            bigMe = Integer.parseInt(index) > Integer.parseInt(opponent.getIndex());
//            bigMe = index.compareTo(opponent.getIndex()) > 0;

            // Creat amop connect
            clock.start("Round " + Global.round + ": Connect peer");
            String publishTopic = index + "_" + realName + "_" + account.getPk();
            String subscribeTopic = opponent.getIndex() + "_" + opponent.getName() + "_" + opponent.getAccountPk();
            System.out.println("publishTopic: " + publishTopic);
            publish = new Communicate(publishTopic, account.getBcosSDK());
            subscribe = new Communicate(subscribeTopic, account.getBcosSDK());


            publish.setIndexOp(opponent.getIndex());
            publish.setNameOp(opponent.getName());
            subscribe.subscribe(bidIndexFilesPath, index, realName);

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
            clock.start("Round " + Global.round + ": Core initialization");
            register.read();
            //启动核心
            String exePath = account.getRootPath() + "sBid";
            File rootDir = new File(account.getRootPath());
            List<String> params = new ArrayList<>();
            params.add(exePath);
            params.add("-b");
            params.add(index);
            params.add(opponent.getIndex());
            params.add(String.valueOf(Global.round));
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
            int port = 17000;
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
            clock.start("Round " + Global.round + ": sBid");
            String fileName = bidIndexFilesPath + "pk" + opponent.getIndex() + ".txt";
            Global.writeFile(fileName, opponent.getPublic_key() + "\n");
            publish.sendCoreFile(fileName);

            fileList.add(bidIndexFilesPath + "pk" + register.getIndex() + ".txt");
//6.Create cipher
            String fileNameSend = bidIndexFilesPath + "ciphertext" + index + "-R" + Global.round + ".txt";
            String fileNameRecv = bidIndexFilesPath + "ciphertext" + opponent.getIndex() + "-R" + Global.round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//7.Prove cipher
            fileNameSend = bidIndexFilesPath + "proveCipher" + index + "-R" + Global.round + ".txt";
            fileNameRecv = bidIndexFilesPath + "proveCipher" + opponent.getIndex() + "-R" + Global.round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//8.Prove cipher consistency
            if (Global.round > 1) {//轮空可能没有前一轮的
                String provecipherFileNameSend = bidIndexFilesPath + "proveConsistency" + index + "-R" + Global.round + ".txt";
                String provecipherFileNameRecv = bidIndexFilesPath + "proveConsistency" + opponent.getIndex() + "-R" + Global.round + ".txt";
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
                    String ciphertextLastFinishRoundFilePath = bidIndexFilesPath + ciphertextLastFinishRoundFileName;
//                    files.read(ciphertextLastFinishRoundFileName);
                    String fileZipName = "fileZip_" + Table_SBid_Name + "_B" + sBid_name + "_I" + opponent.getIndex() + "_R" + opponent.getLastFinishRound() + ".txt";//fileZip_招标机构名_招标名_Index_round.txt
//                    String fileZipName = "fileZip" + opponent.getIndex() + "-R" + opponent.getLastFinishRound() + ".txt";
//                    System.out.println("Downloading file " + fileZipName);
                    Files files = new Files(contract, opponent.getTable_files_name(), bidIndexFilesPath);
                    files.read(fileZipName);
                    Global.unzipFile(bidIndexFilesPath, files.getFile_content());
                    System.out.println("Download file " + fileZipName + " from " + opponent.getTable_files_name());
//                    Global.writeFile(bidIndexFilesPath + ciphertextLastFinishRoundFileName, files.getFile_content());
                    //send two file to core to verify
                    publish.sendCoreFile(ciphertextLastFinishRoundFilePath);
                    publish.sendCoreFile(provecipherFileNameRecv);
                }
            }
//9.Compare (BIG INDEX)
            if (bigMe) {
                fileNameSend = bidIndexFilesPath + "cipherCR" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "cipherCR" + opponent.getIndex() + "-R" + Global.round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, bigMe);
            }
//10.Prove compare (BIG INDEX)
            if (bigMe) {
                fileNameSend = bidIndexFilesPath + "proveCompare" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "proveCompare" + opponent.getIndex() + "-R" + Global.round + ".txt";
                amopSingle(fileNameRecv, bigMe);
            }
//11.Shuffle 1  (SMALL INDEX)
            if (!bigMe) {
                fileNameSend = bidIndexFilesPath + "cipherSR" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, !bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "cipherSR" + opponent.getIndex() + "-R" + Global.round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, !bigMe);
            }
//12.Shuffle 2 (BIG INDEX)
            if (bigMe) {
                fileNameSend = bidIndexFilesPath + "cipherSR" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "cipherSR" + opponent.getIndex() + "-R" + Global.round + ".txt";
                fileList.add(fileNameRecv);
                amopSingle(fileNameRecv, bigMe);
            }
//13.Prove shuffle 1  (SMALL INDEX)
            if (!bigMe) {
                fileNameSend = bidIndexFilesPath + "proveShuffle" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, !bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "proveShuffle" + opponent.getIndex() + "-R" + Global.round + ".txt";
                amopSingle(fileNameRecv, !bigMe);
            }
//14.Prove Shuffle 2 (BIG INDEX)
            if (bigMe) {
                fileNameSend = bidIndexFilesPath + "proveShuffle" + index + "-R" + Global.round + ".txt";
                fileList.add(fileNameSend);
                amopSingle(fileNameSend, bigMe);
            } else {
                fileNameRecv = bidIndexFilesPath + "proveShuffle" + opponent.getIndex() + "-R" + Global.round + ".txt";
                amopSingle(fileNameRecv, bigMe);
            }
//15.Create dk
            fileNameSend = bidIndexFilesPath + "dk" + index + "-R" + Global.round + ".txt";
            fileNameRecv = bidIndexFilesPath + "dk" + opponent.getIndex() + "-R" + Global.round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
//16.Prove dk
            fileNameSend = bidIndexFilesPath + "proveDecrypt" + index + "-R" + Global.round + ".txt";
            fileNameRecv = bidIndexFilesPath + "proveDecrypt" + opponent.getIndex() + "-R" + Global.round + ".txt";
            fileList.add(fileNameSend);
            amop(fileNameSend, fileNameRecv);
            exitCode = -1;
            try {
                exitCode = process.waitFor();
            } catch (java.lang.InterruptedException e) {
                e.printStackTrace();
            }
//17.Update reesult
            fileNameSend = bidIndexFilesPath + "ans" + index + "-R" + Global.round + ".txt";
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
            clock.start("Round " + Global.round + ": Upload");
            Files files = new Files(contract, register.getTable_files_name(), bidIndexFilesPath);
            StringBuilder fileZip = new StringBuilder();
            Global.zipFile(fileList, fileZip);
            fileZip.append("opponentIndex-R").append(Global.round).append(".txt#").append(opponent.getIndex()).append("@");
            //证明文件
            String fileZipName = "fileZip_" + Table_SBid_Name + "_B" + sBid_name + "_I" + index + "_R" + Global.round + ".txt";//fileZip_招标机构名_招标名_Index_round.txt
            String fileZipPath = bidIndexFilesPath + fileZipName;
            Global.writeFile(fileZipPath, fileZip.toString());
            //签名
            String sigFileName = "fileZip_" + Table_SBid_Name + "_B" + sBid_name + "_I" + index + "_R" + Global.round + ".sig";
            String sigFilePath = bidIndexFilesPath + sigFileName;
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
                String updateResult = (Global.round + 1) + "," + Global.round;
                register.update(updateResult);
            }
            if (loseFlag) {
                System.out.println("\n+++++ You lose +++++\n");
                System.out.println("Opponent: " + opponent.getIndex() + "_" + opponent.getName());
                break;
            } else if (Global.round == totalRound) {
                System.out.println("\n+++++ You win +++++\n");
                register.update("win");
                StringBuilder sk = new StringBuilder();
                if (!Global.readFile(bidIndexFilesPath + "sk" + index + ".txt", sk))
                    return false;
                parameters.update(index, sk.toString());
            }
        }
        System.out.println("Amount: " + amount);
        Global.readResult(parameters.getCounts().intValue(), contract, parameters.getTable_register_name());
        System.out.println("\n" + clock.prettyPrint());
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

    //审计
    public boolean verify(String verifyIndex) {
        boolean result;
        createVerifyFileFolder();
        int lastFinishRoundVerify = 0;
        for (int round = 1; round <= totalRound; round++) {
            System.out.println("Table_SBid_Name: " + Table_SBid_Name);
            String fileZipName = "fileZip_" + Table_SBid_Name + "_B" + sBid_name + "_I" + verifyIndex + "_R" + round + ".txt";//fileZip_招标机构名_招标名_Index_round.txt
            String sigFileName = "fileZip_" + Table_SBid_Name + "_B" + sBid_name + "_I" + verifyIndex + "_R" + round + ".sig";

            Register verifyObject = new Register(contract, parameters.getTable_register_name(), verifyIndex, "");
            verifyObject.read();
            Files files = new Files(contract, verifyObject.getTable_files_name(), "");
            System.out.println("Downloading file\n" + fileZipName + "\nfrom\n" + verifyObject.getTable_files_name());
            if (!files.read(fileZipName))
                continue;
            String zipFile = files.getFile_content();
            Global.writeFile(bidVerifyFilesPath + fileZipName, zipFile);

            System.out.println("Download file " + fileZipName + " success");

            System.out.println("Downloading signature: " + sigFileName);
            if (!files.read(sigFileName)) {//没有签名文件，验证失败
                System.err.println("Download signature failed");
                return false;
            }
            Global.writeFile(bidVerifyFilesPath + sigFileName, files.getFile_content());
            System.out.println("Download file " + sigFileName + " success");
            if (!Global.verifySignature(files.getFile_content(), verifyObject.getAccountPk(), zipFile)) {
                System.err.println("Verify signature failed");//验签失败
                return false;
            }

            //启动核心
            String exePath = account.getRootPath() + "sBid";
            File rootDir = new File(account.getRootPath());
            List<String> params = new ArrayList<>();
            params.add(exePath);
            params.add("-v");
            params.add(verifyIndex);
            params.add(String.valueOf(round));
            params.add(String.valueOf(lastFinishRoundVerify));//上一轮竞标的round
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
            port += Integer.parseInt(verifyIndex);
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
                communicate.sendCoreFile(bidVerifyFilesPath + fileZipName);
//                communicate.recvCoreFile(bidIndexFilesPath + "pk" + index + ".txt");
//                communicate.recvCoreFile(bidIndexFilesPath + "sk" + index + ".txt");
                String resultFileName = bidIndexFilesPath + "verify-R" + round + ".txt";
                communicate.recvCoreFile(resultFileName);
                communicate.getClient().close();
                StringBuilder resultStr = new StringBuilder();
                if (!Global.readFile(resultFileName, resultStr))
                    return false;
                result = (resultStr.toString().compareTo("PASS") == 0);
                if (!result)
                    return false;
                exitCode = process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (exitCode != 0) {
                System.err.println("Core error, exit (-v)");
                System.exit(-1);
            } else {
                System.out.println("Core finish");
            }
            lastFinishRoundVerify = round;
        }
        System.out.println("Audit finish");
        return true;
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

    public boolean getBidFinishStatus() {
        String winner = parameters.getWinner();
        System.out.println("winner: " + winner);
        return winner.length() > 0;//winner项为空则竞标未结束，返回false
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
