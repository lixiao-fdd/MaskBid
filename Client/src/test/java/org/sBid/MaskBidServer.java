package org.sBid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.sBid.FileToByteArrayHelper.byteCat;
import static org.sBid.FileToByteArrayHelper.getFileByteArray;

@Controller
public class MaskBidServer {
    //    private final int port;
    private String rootPath;
    private final SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final String fileFolder = "./src";
    private SBidBC sBidBC;
    private String role;
    private String name;
    private String bidCode;
    private String tenderTableName;

    @ResponseBody
    @RequestMapping(value = "/json", method = RequestMethod.POST, produces = "application/json")
    public String GodModReset(@RequestBody JSONObject recvJson) {
        System.out.println("recv");
        File directory = new File("");
        try {
            rootPath = directory.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            e.printStackTrace();
        }
        String act = recvJson.getString("act");
        System.out.println("ACT: " + act + " - " + recvJson.toJSONString());
        JSONObject json = new JSONObject();
        json.put("act", act);

        switch (act) {
            //列出已保存的账号
            case "listAccount" -> {
                File dir = new File(rootPath);
                ArrayList<String> dirList = new ArrayList<>();
                listAccounts(dir, dirList);
                json.put("accountList", dirList);
            }

            //创建账号
            case "createAccount" -> {
                name = recvJson.getString("name");
                sBidBC = new SBidBC(name);
                sBidBC.createAccount();
                File dir = new File(rootPath);
                ArrayList<String> dirList = new ArrayList<>();
                listAccounts(dir, dirList);
                json.put("accountList", dirList);
            }

            //导入账户
            case "postSk" -> {
                String accountName = recvJson.getString("name");
                String accountSkBase64 = recvJson.getString("sk");

                sBidBC = new SBidBC(accountName);
                if (sBidBC.loadAccount(Global.baseDecode(accountSkBase64))) {
                    File dir = new File(rootPath);
                    ArrayList<String> dirList = new ArrayList<>();
                    listAccounts(dir, dirList);
                    json.put("accountList", dirList);
                } else
                    json.put("status", false);
            }

            //删除所有账户
            case "deleteAllAccount" -> {
                File dir = new File(rootPath);
                deleteAllAccounts(dir);
                ArrayList<String> dirList = new ArrayList<>();
                listAccounts(dir, dirList);
                json.put("accountList", dirList);
            }

            //登录
            case "login" -> {
                String name = recvJson.getString("name");
                System.out.println("account Name: " + name);
                sBidBC = new SBidBC(name);
                boolean result = sBidBC.loadAccount();
                System.out.println("login: " + result);
                json.put("result", result);
                json.put("role", name.substring(name.lastIndexOf(":") + 1));
            }

            //加载账户信息
            case "listAccountInfo" -> {
                String fullNamebase64 = sBidBC.getRealName();
                String fullName = Global.baseDecode(fullNamebase64);
                json.put("name", fullName.substring(0, fullName.lastIndexOf(":")));
                json.put("address", sBidBC.getAccount().getAddress());
                json.put("pk", sBidBC.getAccount().getPk());
                json.put("role", sBidBC.getRole());
            }

            //设置投标对象
            case "setBid" -> {
                String tenderName = recvJson.getString("tenderName");
                String bidCode = recvJson.getString("bidCode");
                String tenderTableName = "Tender_" + Global.sha1(tenderName + ":0");//招标机构在招标机构注册表中的名称
                if (!sBidBC.parametersRead(tenderTableName, bidCode)) {
                    json.put("result", false);
                    break;
                }
                json.put("result", true);
                Parameters parameters = sBidBC.getParameters();
                json.put("tenderName", tenderName);//采购单位
                json.put("bidCode", bidCode);//项目编号
                json.put("dateStart", parameters.getDateStart());//发布日期
                json.put("dateEnd", parameters.getDateEnd());//截止日期
                json.put("counts", parameters.getCounts().intValue());//竞标人数
                json.put("bidName", Global.baseDecode(parameters.getBidName()));//招标名称

                //报价状态
                String index = sBidBC.getIndex(sBidBC.getRealName());
                boolean registAlready = index.compareTo("-1") != 0;//todo 有问题
                json.put("registAlready", registAlready);
                if (registAlready) {
                    json.put("amount", sBidBC.getAmount());
                }
                json.put("finishAlready", sBidBC.getBidFinishStatus());
                String status = (sBidBC.getBidFinishStatus()) ? "已结束" : "进行中";
                json.put("status", status);
            }

            //设置投标参数
            case "setBidContent" -> {
                String name = recvJson.getString("name");
                String amount = recvJson.getString("amount");
                String tenderName = recvJson.getString("tenderName");
                bidCode = recvJson.getString("bidCode");
                System.out.println("name: " + name);
                System.out.println("amount: " + amount);
                System.out.println("tenderName: " + tenderName);
                System.out.println("bidCode: " + bidCode);
                tenderTableName = "Tender_" + Global.sha1(tenderName + ":0");//招标机构在招标机构注册表中的名称
                sBidBC.parametersRead(tenderTableName, bidCode);
                sBidBC.setAmount(amount);
                //链上注册
                deleteTempDir();
                StringBuilder status = new StringBuilder();
                sBidBC.registration(status);
                json.put("registResult", status);
                sBidBC.getRegistrationInfo(json);
            }

            //刷新竞标信息
            case "refreshBidContent" -> {
                sBidBC.parametersRead(tenderTableName, bidCode);
                sBidBC.getRegistrationInfo(json);
                json.put("registAlready", sBidBC.getIndex(sBidBC.getRealName()).compareTo("-1") != 0);
                json.put("finishAlready", sBidBC.getBidFinishStatus());
                System.out.println(json.toJSONString());
            }

            //重置竞标
            case "resetBidContent" -> {
                sBidBC.withdrawRegistration();
            }

            //开始竞标 todo 应更改为准备竞标
            case "prepareBid" -> {
                json.put("result", sBidBC.sBid());
                json.put("name", Global.baseDecode(sBidBC.getRealName()));
            }

            //开始审计
            case "startAudit" -> {
                JSONArray checkedList = recvJson.getJSONArray("checkedList");
                Object[] array = checkedList.toArray();
                ArrayList<Pair<String, Boolean>> resultsList = new ArrayList<>();
                for (Object o : array) {
                    String verifyIndex = String.valueOf(o);
                    Boolean verifyResult = sBidBC.verify(verifyIndex);
                    Pair<String, Boolean> temp = Pair.of(verifyIndex, verifyResult);
                    resultsList.add(temp);
                }
                json.put("result", resultsList);
            }

            //加载注册表的内容
            case "loadRegList" -> {
                String verifyTenderName = recvJson.getString("tenderName");
                String verifyBidCode = recvJson.getString("bidCode");
                String tenderTableName = "Tender_" + Global.sha1(verifyTenderName + ":0");//招标机构在招标机构注册表中的名称
                if (sBidBC.parametersRead(tenderTableName, verifyBidCode) && sBidBC.getBidFinishStatus()) {
                    sBidBC.getRegistrationInfo(json);
                    json.put("status", true);
                    break;
                }
                json.put("status", false);
            }

            //招标者创建新的招标
            case "createNewBid" -> {
                String nameNewBid = recvJson.getString("name");
                String countsNewBid = recvJson.getString("counts");
                String dateStartNewBid = recvJson.getString("dateStart");
                String dateEndNewBid = recvJson.getString("dateEnd");
                String bidNameNewBid = recvJson.getString("bidName");
                sBidBC.parametersInit(bidNameNewBid, countsNewBid, dateStartNewBid.replace("T", "_"), dateEndNewBid.replace("T", "_"));
                json.put("bidCode", sBidBC.getsBid_name());
            }

            //加载招标表的内容
            case "listOldBids" -> {
                String tenderName = recvJson.getString("tenderName");
                boolean status = sBidBC.readBidTable(json);
                json.put("status", status);
            }

            //查询招标详细信息
            case "showBidDetail" -> {
                String tenderName = recvJson.getString("tenderName");
                String bidCode = recvJson.getString("bidCode");
                boolean status = sBidBC.readBidDetail(json, bidCode);
                json.put("status", status);
            }

            //重置所有
            case "GodModReset" -> {
                String tenderName = recvJson.getString("tenderNameReset");
                sBidBC = new SBidBC(tenderName + ":0");
                sBidBC.createAccount();
                sBidBC.loadAccount();
                sBidBC.allDel();
            }//todo c++程序执行完后删除临时文件；

            default -> {
                System.err.println("unknown act: " + act);
                json.put("result", false);
            }
        }

        System.out.println("response: " + json.toJSONString());
        return json.toJSONString();
    }

    public boolean listAccounts(File dir, ArrayList<String> dirList) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                File temp = new File(dir, child);
                if (temp.exists() && temp.isDirectory() && temp.getName().contains("Files_")) {
                    String filePath = temp.getAbsolutePath() + File.separator + "name.txt";
                    StringBuilder realName = new StringBuilder();
                    if (!Global.readFile(filePath, realName))
                        return false;
                    System.out.println(filePath + "\n" + realName);
                    byte[] base64decodedBytes = Base64.getDecoder().decode(realName.toString().replace("\n", ""));
                    dirList.add(new String(base64decodedBytes));
                }
            }
        }
        return true;
    }

    public boolean deleteTempDir() {
        File dir = new File(rootPath);
        String[] children = dir.list();
        for (String child : children) {
            File temp = new File(dir, child);
            if (temp.exists() && temp.isDirectory() && temp.getName().contains("files_")) {
                String filePath = temp.getAbsolutePath();
                boolean bol = FileUtils.deleteQuietly(temp);
                System.out.println("Delete " + filePath + ": " + bol);
            }
        }
        return true;
    }

    public void deleteAllAccounts(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                File temp = new File(dir, children[i]);
                if (temp.exists() && temp.isDirectory() && temp.getName().contains("Files_")) {
                    String filePath = temp.getAbsolutePath();
                    boolean bol = FileUtils.deleteQuietly(temp);
                    System.out.println("Delete " + filePath + ": " + bol);
                }
            }
        }
    }

}
