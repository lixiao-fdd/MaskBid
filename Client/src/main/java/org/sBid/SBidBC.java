package org.sBid;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


public class SBidBC {
    private final String accountName;
    private final String realName;
    private final String role;
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
    private Tender tender;
    private Parameters parameters;
    private Communicate publish;
    private Communicate subscribe;
    private boolean bidStopFlag = false;

    public SBidBC(String accountName) {
        role = accountName.substring(accountName.lastIndexOf(":") + 1);
        this.accountName = Global.sha1(accountName);
        this.realName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
//        this.accountName = Base64.getEncoder().encodeToString(accountName.getBytes(StandardCharsets.UTF_8));
        if (role.compareTo("0") == 0) {//招标者的招标表名
            //todo 表名有限制，只能有$, _, @三个符号，且长度小于64，因此将招标者名称经sha1后作为表名，长度为40，key的长度不能超过255
            //todo 由招标者名称生成招标者的招标表名，由招标名称生成招标表的key
            Table_SBid_Name = "Tender_" + this.accountName;
        }
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

    //创建账号
    public void createAccount() {
        //软件启动时及切换用户时调用
        account = new Account(accountName, contractAddress);
        account.createAccount();
        Global.writeFile(account.getFilesPath() + "name.txt", realName);
    }

    //加载账号
    public boolean loadAccount() {
        //软件启动时及切换用户时调用
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
        Global.writeFile(account.getFilesPath() + "name.txt", realName);
        return true;
    }

    //初始化竞标参数
    //key : name, field : counts, p, q, h, g, !【winner】, !【sk】, 【dateStart】, 【dateEnd】, 【bidName】, table_register_name
    //name和table_register_name由bidName生成，pqhg调用c++程序生成

    //创建新的招标者项
    public void tenderReg() {
        tender = new Tender(contract, Table_Tender_Name, accountName);
        if (!tender.read()) {
            tender.insert();//delete
            System.out.println("Tender " + accountName + " has registered successfully");
        } else {
            System.out.println("Tender " + accountName + " already exists");
        }
    }

    //读取招标者的招标表内容
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
            int port = 18000;
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
    public boolean parametersInit(String bidName, String counts, String dateStart, String dateEnd) {

        //生成bidName(base64)
        String bidNameBase64 = Base64.getEncoder().encodeToString(bidName.getBytes(StandardCharsets.UTF_8));//base64处理后的招标名，可恢复
        //生成name(sha1)
        //邀请招标
//        String fullNamePlain = bidName + dateStart;
//        sBid_name = Global.sha1(fullNamePlain);//key:用于查找的招标名
        //公开招标
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
        int port = 18000;
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
            System.out.println("Created a new bid");
        }
        System.out.println("@".repeat(30));
        System.out.println("bid counts 2: " + bidCounts);
        System.out.println("TenderName: " + Table_SBid_Name);
        System.out.println("BidCode: " + sBid_name);
        System.out.println("@".repeat(30));
        return true;
    }

    public void allDel() {
        //删除tender注册表
        tender = new Tender(contract, Table_Tender_Name, accountName);
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
            System.out.println("Reset register " + bidCodeTemp);
            for (int j = 1; j <= bidTable.getCounts().intValue(); j++) {
                Register register = new Register(contract, bidTable.getTable_register_name(), String.valueOf(i), "");
                if (!register.delete())
                    break;
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
        System.out.println("*".repeat(30));
        System.out.println("* TenderTableName: " + tenderTableName);
        System.out.println("* BidCode: " + bidCode);
        System.out.println("*".repeat(30));
        this.sBid_name = bidCode;//本次招标在招标机构招标表中的名称
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
        counts = parameters.getCounts().intValue();
        System.out.println(counts + " participants");
        //计算总轮数
        if (counts <= 2) {
            totalRound = 1;
        } else {
            totalRound = (int) Math.sqrt(counts);
            if (Math.pow(totalRound, 2) != counts) {
                ++totalRound;
            }
        }
        System.out.println(totalRound + " rounds");
        return true;
    }

    //创建投标文件夹
    private void createBidFileFolder() {
        bidFilesPath = account.getFilesPath() + Table_SBid_Name + "_" + sBid_name + File.separator;
        createFolder(bidFilesPath);
    }

    //创建投标Index文件夹
    private void createBidIndexFileFolder() {
        bidIndexFilesPath = bidFilesPath + "index_" + index + File.separator;
        createFolder(bidIndexFilesPath);
    }

    //创建verify文件夹
    private void createVerifyFileFolder() {
        bidVerifyFilesPath = bidFilesPath + "Verify" + File.separator;
        createFolder(bidVerifyFilesPath);
    }

    //创建decrypt文件夹
    private void createDecryptFileFolder() {
        bidDecryptFilesPath = bidFilesPath + "Decrypt" + File.separator;
        createFolder(bidDecryptFilesPath);
    }

    //创建文件夹
    private void createFolder(String path) {
        Path filesDir = Paths.get(path);
        try {
            java.nio.file.Files.createDirectory(filesDir);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            System.err.println("Failed to create " + filesDir);
            e.printStackTrace();
        }
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
    public void getRegistrationInfo(JSONObject json) {
        counts = parameters.getCounts().intValue();
        ArrayList<List<String>> info = new ArrayList<>();
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

    //链上注册
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
        int port = 18000;
        port += Integer.parseInt(index) * 100;
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
            bigMe = index.compareTo(opponent.getIndex()) > 0;

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
            int port = 18000;
            port = 18000;
            port += Integer.parseInt(index) * 100 + Global.round;
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
                    //Download ciphertext generated by the opponent last bid participation from the chain

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
            clock.stop();
            clock.start("Round " + Global.round + ": Update reesult");
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
            clock.start("Round " + Global.round + ": Files upload");
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
//        System.out.println("\n" + clock.prettyPrint());

        return !loseFlag;
    }

    private void amop(String fileNameSend, String fileNameRecv) {
        if (bigMe) {
            //core recv and amop send
            publish.recvCoreFile(fileNameSend);
            publish.sendAmopFile(fileNameSend);
            //amop recv and core send
            subscribe.recvAmopFile(fileNameRecv);
            publish.sendCoreFile(fileNameRecv);
        } else {
            subscribe.recvAmopFile(fileNameRecv);
            publish.sendCoreFile(fileNameRecv);
            publish.recvCoreFile(fileNameSend);
            publish.sendAmopFile(fileNameSend);
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
            int port = 18000;
            port += Integer.parseInt(verifyIndex) * 100 + round;
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
        return parameters.getWinner().length() != 0;//winner项为空则竞标未结束，返回false
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
}
