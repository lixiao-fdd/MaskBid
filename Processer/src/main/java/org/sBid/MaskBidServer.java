package org.sBid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.sBid.FileToByteArrayHelper.byteCat;
import static org.sBid.FileToByteArrayHelper.getFileByteArray;

@Controller
public class MaskBidServer {
    private String rootPath;
    private final SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final String fileFolder = "./src";
    private SBidBC sBidBC = null;
    private String role;
    private String name;
    private String bidCode;
    private String tenderTableName;
    private String auditTenderName = "";
    private String auditBidCode = "";
    private int auditTestLogRound = 5;
    private int auditTestRound = 0;
    private String mbkPath;

    //登录 上传密钥文件
    @ResponseBody
    @PostMapping("/signin")
    public String signin(@RequestParam("file") MultipartFile file) {
        try {
            File directory = new File("");
            rootPath = directory.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");
        JSONObject data = new JSONObject();
        if (file.isEmpty()) {
            json.put("code", 1);
            json.put("msg", "文件上传失败");
        } else {
            String fileName = file.getOriginalFilename();
            data.put("fileName", fileName);
            String filePath = "./" + fileName;
            File dest = new File(filePath);
            try {
                FileUtils.copyInputStreamToFile(file.getInputStream(), dest);
                if (!dest.exists()) {
                    json.put("code", 2);
                    json.put("msg", "文件转储失败");
                } else {
                    StringBuilder fileContent = new StringBuilder();
                    Global.readFile(filePath, fileContent);
                    System.out.println(dest.getAbsolutePath());
                    sBidBC = new SBidBC(fileContent.toString(), data);
                    if (dest.delete()) {
                        System.out.println(dest.getAbsolutePath() + "删除成功");
                    } else {
                        System.out.println(dest.getAbsolutePath() + "删除失败");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                json.put("code", 3);
                json.put("msg", "文件处理失败");
            }
        }
        json.put("data", data);
        return json.toJSONString();
    }

    //注册
    // todo: 同名就不可以加入了
    @ResponseBody
    @GetMapping(value = "/signup")
    public ResponseEntity<byte[]> fileDownload(@RequestParam("newAccountName") String newAccountName, @RequestParam("newAccountRole") String newAccountRole) throws IOException {
        try {
            File directory = new File("");
            rootPath = directory.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder mbkFileContent = new StringBuilder();
        sBidBC = new SBidBC(newAccountName, newAccountRole, mbkFileContent);
//        mbkPath = mbkFileContent.toString();
//        File file = new File(mbkPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.parse("attachement;filename=" + URLEncoder.encode(((newAccountRole.compareTo("0") == 0) ? "Tender" : "Bidder") + "_" + newAccountName + ".mbk", StandardCharsets.UTF_8)));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<byte[]>(mbkFileContent.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    //加载表格
    @ResponseBody
    @RequestMapping(value = "/table")
    public String getTable(@RequestHeader(value = "tableType") String tableType) {
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");

        switch (tableType) {
            //进行中的标的
            case "BidOngoing" -> {
                assert sBidBC != null;
                sBidBC.readBidTable(json, "ongoing");
            }
            //已完成的标的
            case "BidFinished" -> {
                assert sBidBC != null;
                sBidBC.readBidTable(json, "finish");
            }
            //投标者进行中的标的
            case "bidderBidOngoing" -> {
                assert sBidBC != null;
                sBidBC.readBidderBidTable(json, "ongoing");
            }
            //投标者已完成的标的
            case "bidderBidFinish" -> {
                assert sBidBC != null;
                sBidBC.readBidderBidTable(json, "finish");
            }
            //标的投标信息
            case "BidRegInfo" -> {
                assert sBidBC != null;
                sBidBC.getRegInfo(json);
            }
            //审计标的对象的投标信息
            case "BidAuditRegInfo" -> {
                Random random = new Random();
                int count = 4;
                ArrayList<JSONObject> dataList = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    JSONObject item = new JSONObject();
                    int num = random.nextInt(64);
                    item.put("bidderIndex", "No." + (i + 1));
                    item.put("bidderName", "投标者" + num);
                    item.put("bidderPk", Global.sha1("投标者" + num));
                    item.put("bidderResults", "投标结果");
                    item.put("auditResults", "等待结束");
                    dataList.add(item);
                }
                json.put("count", count);
                json.put("data", dataList);
            }
            default -> {
                json.replace("code", 1);
                json.replace("msg", "unknown type: " + tableType);
                json.put("count", 0);
                json.put("data", "");
            }
        }
        return json.toJSONString();
    }

    //加载指定招标方已完成的标的列表
    @ResponseBody
    @RequestMapping(value = "/searchFinish")
    public String searchTableFinish(@RequestHeader(value = "tenderName") String tenderName) {
        tenderName = URLDecoder.decode(tenderName, StandardCharsets.UTF_8);
        //列出指定招标方的所有标的
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");
        Random random = new Random();
        int count = 10;
        ArrayList<JSONObject> dataList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            JSONObject item = new JSONObject();
            int num = random.nextInt(100);
            item.put("bidName", tenderName + "_Bid" + i);
            item.put("bidCode", Global.sha1(tenderName + "_" + num));
            item.put("bidCounts", num);
            dataList.add(item);
        }
        json.put("count", count);
        json.put("data", dataList);
        return json.toJSONString();
    }

    //加载指定招标方进行中的标的列表
    @ResponseBody
    @RequestMapping(value = "/searchOngoing")
    public String searchTableOngoing(@RequestHeader(value = "tenderName") String tenderName) {
        tenderName = URLDecoder.decode(tenderName, StandardCharsets.UTF_8);
        //列出指定招标方的所有标的
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");
        sBidBC.loadTenderBidList(tenderName, json);
        return json.toJSONString();
    }

    //json请求
    @ResponseBody
    @RequestMapping(value = "/data", method = RequestMethod.POST, produces = "application/json")
    public String resposeJson(@RequestBody JSONObject recvJson) {
        String act = recvJson.getString("act");
        JSONObject recvJsonData = recvJson.getJSONObject("data");
        JSONObject json = new JSONObject();
        json.put("act", act);
        json.put("result", true);
        JSONObject data = new JSONObject();
        switch (act) {
            //判断是否已经登陆
            case "cookies" -> {
                if (sBidBC != null) {
                    json.put("code", 0);
                    data.put("accountRole", sBidBC.getRole());
                } else
                    json.put("code", 1);
            }
            //退出登录，（无输入）（无输出）
            //todo: 删除临时文件
            case "logout" -> {
                sBidBC = null;
            }
            //加载账户信息（无输入）（账户名，账户地址，账户公钥）
            case "listAccountInfo" -> {
                assert sBidBC != null;
                data.put("accountName", Global.baseDecode(sBidBC.getRealName()));
                data.put("accountRole", (sBidBC.getRole().compareTo("0") == 0) ? "招标方" : "投标方");
                data.put("accountAddress", sBidBC.getAccount().getAddress());
                data.put("accountPk", sBidBC.getAccount().getPk());

            }
            //发布新的标的（标的名称，标的内容，标的开始时间，标的持续时间，标的持续时间单位）（标的编号）
            case "postNewBid" -> {
                String newBidName = recvJsonData.getString("newBidName");
                int newBidDuration = recvJsonData.getIntValue("newBidDuration");
                String newBidDurationUnit = recvJsonData.getString("newBidDurationUnit");
                String newBidContent = recvJsonData.getString("newBidContent");
                String newBidDateStart = recvJsonData.getString("newBidDateStart");
                String newBidDateEnd = Global.dateCaculate(newBidDateStart, newBidDuration, newBidDurationUnit);

                assert sBidBC != null;
                if (sBidBC.postNewBid(newBidName, newBidContent, newBidDateStart, newBidDateEnd)) {
                    data.put("postStatus", true);
                    data.put("newBidDateEnd", newBidDateEnd);
                    data.put("bidCode", sBidBC.getsBid_name());
                } else
                    data.put("postStatus", false);
            }
            //加载标的详细信息（招标者名称，标的编号）（标的名称，标的内容，标的编号，投标人数，开始时间，结束时间，标的状态，中标金额）
            case "showBidDetail" -> {
                String tenderName = recvJsonData.getString("tenderName");
                String bidCode = recvJsonData.getString("bidCode");
                sBidBC.loadBidDetail(tenderName, bidCode, 0, data);
            }
            //加载标的（招标者名称，标的编号）（招标者名称，标的名称，标的内容，标的编号，开始时间，结束时间）
            case "loadBid" -> {
                String bidTenderName = recvJsonData.getString("tenderName");
                String bidBidCode = recvJsonData.getString("bidCode");
                sBidBC.loadBidDetail(bidTenderName, bidBidCode, 1, data);
            }
            //发布投标金额（招标者名称，标的编号，投标金额）（投标结果）
            case "postBidAmount" -> {
                String bidTenderName = recvJsonData.getString("tenderName");
                String bidBidCode = recvJsonData.getString("bidCode");
                String bidAmount = recvJsonData.getString("bidAmount");
                deleteTempDir();
                data.put("postBidStatus", sBidBC.postAmount(bidTenderName, bidBidCode, bidAmount));
            }
            //准备投标（招标者名称，标的编号）（招标者名称，标的编号，竞标结果）
            case "prepareBid" -> {
                String bidTenderName = recvJsonData.getString("tenderName");
                String bidBidCode = recvJsonData.getString("bidCode");

                data.put("bidTenderName", bidTenderName);
                data.put("bidBidCode", bidBidCode);
                data.put("bidResult", true);
            }
            //准备审计（招标者名称，标的编号）（标的名称，标的内容，标的编号，投标人数，审计结果）
            case "prepareAudit" -> {
                auditTenderName = recvJson.getJSONObject("data").getString("tenderName");
                auditBidCode = recvJson.getJSONObject("data").getString("bidCode");

                Random random = new Random();
                int num = random.nextInt(10);

                data.put("auditInfoBidName", "auditInfoBidName");
                data.put("auditInfoBidContent", "auditInfoBidContent");
                data.put("auditInfoBidCode", "auditInfoBidCode");
                data.put("auditInfoBidCounts", num);
                data.put("auditInfoBidResult", "等待结束");
            }
            //开始审计（是否为开始审计）（审计是否结束，(如果结束)审计结果，日志）
            case "startAudit" -> {
                boolean auditStart = recvJson.getBooleanValue("auditStart");
                if (auditStart) {
                }
                //开始审计
                //fake
                StringBuilder stringBuilder = new StringBuilder();
                if (auditTestRound++ < auditTestLogRound) {
                    Random random = new Random();
                    int num = random.nextInt(10);
                    for (int i = 0; i < num; i++) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date();
                        stringBuilder.append(sdf.format(date)).append(" INFO ").append(random.nextInt(1000)).append(" --- Everything ok.").append("\n");
                        try {
                            Thread.sleep(random.nextInt(100));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    data.put("finishStatus", false);
                } else {
                    data.put("auditResult", true);//应为数组
                    data.put("finishStatus", true);
                    auditTestRound = 0;
                }
                data.put("log", stringBuilder);

            }
            default -> {
                System.err.println("unknown act: " + act);
                json.replace("result", false);
            }
        }
        json.put("data", data);
        return json.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/json", method = RequestMethod.POST, produces = "application/json")
    public String respose(@RequestBody JSONObject recvJson) {
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

    public void returnEmptyJson(JSONObject json) {
        int count = 0;
        ArrayList<JSONObject> dataList = new ArrayList<>();
        json.put("count", count);
        json.put("data", dataList);
    }
}
