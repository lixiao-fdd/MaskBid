package org.sBid;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditProcesser {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final String INFO = "INFO ";
    private final String WARN = "WARN ";
    private final String OK = " 审计通过\n";
    private final String ER = " 审计失败\n";
    private final Account account;//账户
    private final String tenderName;//要审计的招标方名称
    private final String bidCode;//要审计的标的名称
    private String bidFilesPath;//标的根目录
    private String bidVerifyFilesPath;//审计文件存放目录
    private final String Table_SBid_Name;//标的发起方的标的表的表名
    private final String Table_Register_Name;//标的注册表的表名
    private final SBid contract;//合约
    private final int counts;//竞标人数
    private int totalRound;//竞标轮数
    private ArrayList<Boolean> auditResult = new ArrayList<>();
    private ArrayList<String> output = new ArrayList<>();
    private int outputIndex = 0;
    private boolean isFinish = false;

    Parameters parameters;//竞标参数

    public AuditProcesser(Account account, String tenderName, String bidCode) {
        this.account = account;
        this.tenderName = tenderName;
        this.bidCode = bidCode;
        this.Table_SBid_Name = "Tender_" + Global.sha1(tenderName);
        Table_Register_Name = "Reg_" + bidCode;
        this.contract = account.getContract();
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
        //创建文件夹
        bidFilesPath = account.getFilesPath() + Table_SBid_Name + "_" + bidCode + File.separator;
        Global.createFolder(bidFilesPath);
        bidVerifyFilesPath = bidFilesPath + "Verify" + File.separator;
        Global.createFolder(bidVerifyFilesPath);
        System.out.println("Audit\ncounts: " + counts + " | rounds: " + totalRound);
        //从链上下载parameters
        Global.writeFile(bidFilesPath + "parameters.txt", parameters.getP().toString() + "\n" + parameters.getQ().toString() + "\n" + parameters.getH().toString() + "\n" + parameters.getG().toString());
    }

    public boolean verify() {
        System.out.println("Start audit " + bidCode + " of the " + Table_SBid_Name);
        output.add(getTime() + INFO + "开始审计\n" + getTime() + INFO + "共有 " + counts + " 个审计对象\n");
        boolean result = true;
        for (int i = 1; i <= counts; ++i) {
            System.out.println("Start audit Bidder " + i);
            output.add(getTime() + INFO + "开始审计：投标方 No." + i + "\n");
            boolean indexResult = run(String.valueOf(i));
            result &= indexResult;
            auditResult.add(indexResult);
            output.add(getTime() + (indexResult ? INFO : WARN) + "完成审计：投标方 No." + i + (indexResult ? OK : ER));
        }
        System.out.println("Finish audit");
        isFinish = true;
        return result;
    }

    private boolean run(String verifyIndex) {
        boolean result;
        int lastFinishRoundVerify = 0;
        int outputIndex = 0;
        output.add(getTime() + INFO + "审计流程：正在从区块链上获取承诺材料\n");
        for (int round = 1; round <= totalRound; round++) {
            System.out.println("Round " + round);
            String fileZipName = "fileZip_" + Table_SBid_Name + "_C" + bidCode + "_I" + verifyIndex + "_R" + round + ".txt";
            String sigFileName = "fileZip_" + Table_SBid_Name + "_C" + bidCode + "_I" + verifyIndex + "_R" + round + ".sig";
            Register verifyObject = new Register(contract, parameters.getTable_register_name(), verifyIndex, "");
            verifyObject.read();
            Files files = new Files(contract, verifyObject.getTable_files_name(), "");
            if (!files.read(fileZipName)) {
                continue;
            }
            String zipFile = files.getFile_content();
            Global.writeFile(bidVerifyFilesPath + fileZipName, zipFile);
            if (outputIndex < 1)
                output.add(getTime() + INFO + "审计流程：成功获取承诺材料\n");
            System.out.println("Download file " + fileZipName + " success");

            if (!files.read(sigFileName)) {//没有签名文件，验证失败
                output.add(getTime() + WARN + "审计流程：承诺材料签名缺失！\n");
                System.err.println("Download signature failed");
                return false;
            }
            Global.writeFile(bidVerifyFilesPath + sigFileName, files.getFile_content());
            System.out.println("Download file " + sigFileName + " success");
            if (!Global.verifySignature(files.getFile_content(), verifyObject.getAccountPk(), zipFile)) {
                output.add(getTime() + WARN + "审计流程：承诺材料签名验证失败！\n");
                System.err.println("Verify signature failed");//验签失败
                return false;
            }

            if (outputIndex < 1)
                output.add(getTime() + INFO + "审计流程：正在验证承诺材料\n");
            //启动核心
            String exePath = account.getRootPath() + "sBid";
            File rootDir = new File(bidFilesPath);
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
                String resultFileName = bidVerifyFilesPath + "verify-R" + round + ".txt";
                communicate.recvCoreFile(resultFileName);
                communicate.getClient().close();
                StringBuilder resultStr = new StringBuilder();
                if (!Global.readFile(resultFileName, resultStr)) {
                    output.add(getTime() + WARN + "审计流程：审计核心出现错误，结果不一定准确！\n");
                    return false;
                }
                result = (resultStr.toString().compareTo("PASS") == 0);
                if (!result)//验证结果
                {
                    output.add(getTime() + WARN + "审计流程：承诺验证失败！\n");
                    return false;
                }
                exitCode = process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                output.add(getTime() + WARN + "审计流程：审计核心出现错误，结果不一定准确！\n");
                System.exit(1);
            }
            if (exitCode != 0) {
                System.err.println("Core error, exit (-v)");
                output.add(getTime() + WARN + "审计流程：审计核心出现错误，结果不一定准确！\n");
                System.exit(-1);
            } else {
                System.out.println("Core finish");
            }
            output.add(getTime() + INFO + "审计流程：验证进度 " + (round * 100 / totalRound) + "%\n");
            lastFinishRoundVerify = round;
            ++outputIndex;
        }
        System.out.println("Audit finish");
        return true;
    }

    private String getTime() {
        return sdf.format(new Date(System.currentTimeMillis())) + " ---- ";
    }

    public boolean isFinish() {
        return isFinish;
    }

    public ArrayList<Boolean> getAuditResult() {
        return auditResult;
    }

    public ArrayList<String> getOutput() {
        return output;
    }

    public void getProgress(StringBuilder progress) {
        int outputSize = output.size();
        for (; outputIndex < outputSize; outputIndex++)
            progress.append(output.get(outputIndex));
    }
}
