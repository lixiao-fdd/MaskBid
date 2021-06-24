package org.sBid;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class MaskBidServer {
    private String rootPath;
    private final SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final String fileFolder = "./src";
    private SBidBC sBidBC = null;
    private String role;
    private String name;
    private String address;
    private String pk;
    private String sk;
    private String bidCode;
    private String tenderTableName;
    private String mbkPath;
    private String contractAddress = "0x4a3234be1003513b8c1765bc283f3740efd8f9de";
    private Thread bidManagerThread = null;
    private Thread auditThread = null;
    private BidManager bidManager = null;
    private boolean createDone = false;
    private String auditTenderName = "";
    private String auditBidCode = "";
    AuditProcesser auditProcesser;
    boolean autorun = false;

    @Autowired
    private ApplicationArguments applicationArguments;

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
                    sBidBC = new SBidBC(fileContent.toString(), data);
                    name = Global.baseDecode(sBidBC.getRealName());
                    role = sBidBC.getRole();
                    address = sBidBC.getAccount().getAddress();
                    pk = sBidBC.getAccount().getPk();
                    sk = sBidBC.getAccount().getSk();
                    if (dest.delete()) {
                        System.out.println(dest.getAbsolutePath() + " 删除成功");
                    } else {
                        System.out.println(dest.getAbsolutePath() + " 删除失败");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                json.put("code", 3);
                json.put("msg", "文件处理失败");
            }
        }
        json.put("data", data);
        createDone = true;
        return json.toJSONString();
    }

    //注册
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

        name = newAccountName;
        role = newAccountRole;
        String accountNameTemp = Global.sha1(newAccountName);
        Account accountTemp = new Account(accountNameTemp, contractAddress);
        String accountSk = accountTemp.createAccount();

        mbkFileContent.append(Base64.getEncoder().encodeToString((newAccountName + ":" + newAccountRole + "-" + accountSk).getBytes(StandardCharsets.UTF_8)));

        JSONObject data = new JSONObject();
        sBidBC = new SBidBC(mbkFileContent.toString(), data);
        address = sBidBC.getAccount().getAddress();
        pk = sBidBC.getAccount().getPk();
        sk = accountSk;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.parse("attachement;filename=" + URLEncoder.encode(((newAccountRole.compareTo("0") == 0) ? "Tender" : "Bidder") + "_" + newAccountName + ".mbk", StandardCharsets.UTF_8)));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        createDone = true;
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
                sBidBC.readBidTable(json, "ongoing", sBidBC.getTender());
            }
            //已完成的标的
            case "BidFinished" -> {
                assert sBidBC != null;
                sBidBC.readBidTable(json, "finish", sBidBC.getTender());
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
            case "BidRegInfo", "BidAuditRegInfo" -> {
                assert sBidBC != null;
                sBidBC.getRegInfo(json);
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

    //审计 加载指定招标方已完成的标的列表
    @ResponseBody
    @RequestMapping(value = "/searchFinish")
    public String searchTableFinish(@RequestHeader(value = "tenderName") String tenderName) {
        tenderName = URLDecoder.decode(tenderName, StandardCharsets.UTF_8);
        //列出指定招标方的所有标的
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");
        sBidBC.loadTenderBidList(tenderName, json, "audit");
        return json.toJSONString();
    }

    //投标者 加载指定招标方进行中的标的列表
    @ResponseBody
    @RequestMapping(value = "/searchOngoing")
    public String searchTableOngoing(@RequestHeader(value = "tenderName") String tenderName) {
        tenderName = URLDecoder.decode(tenderName, StandardCharsets.UTF_8);
        //列出指定招标方的所有标的
        JSONObject json = new JSONObject();
        json.put("code", 0);
        json.put("msg", "");
        sBidBC.loadTenderBidList(tenderName, json, "bidder");
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
            //判断注册合法性
            case "checkSignUp" -> {
                String newAccountName = recvJsonData.getString("newAccountName");
                String newAccountRole = recvJsonData.getString("newAccountRole");
                data.put("newAccountName", newAccountName);
                data.put("newAccountRole", newAccountRole);
                SBidBC sBidBCCheck = new SBidBC("这是绝对不能用的管理员账号88089", "44e6ca571cb9e08e1d4a2e490415bc66378bb4460b3b98cd10b8c9bee04fd950");
                if (newAccountRole.compareTo("0") == 0)
                    data.put("legal", sBidBCCheck.isAccountExist(newAccountName));
                else
                    data.put("legal", true);
            }

            //加载账户信息（无输入）（账户名，账户地址，账户公钥）
            //启动bidManager
            case "listAccountInfo" -> {
                while (!createDone) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                data.put("accountName", name);
                data.put("accountRole", (role.compareTo("0") == 0) ? "招标方" : "投标方");
                data.put("accountAddress", address);
                data.put("accountPk", pk);
                if (role.compareTo("0") != 0) {
                    //若是投标者则创建新线程，运行竞标管理器
                    bidManagerThread = new Thread(() -> {
                        bidManager = new BidManager(sBidBC.getAccount(), sBidBC.getStoreJson(), sBidBC.getRealName());
                        while (!Thread.interrupted()) {
                            bidManager.check();//循环执行时间检测
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        System.out.println("interrupted");
                    });
                    bidManagerThread.start(); // 启动新线程
                }
                List<String> args = applicationArguments.getOptionValues("autorun");
                if (args != null)
                    autorun = Boolean.parseBoolean(args.get(0));
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
            case "autoDown" ->{
                if (autorun) {
                    System.out.println("Auto Exit");
                    System.exit(0);
                }
            }
            //发布投标金额（招标者名称，标的编号，投标金额）（投标结果）
            case "postBidAmount" -> {
                String bidTenderName = recvJsonData.getString("tenderName");
                String bidBidCode = recvJsonData.getString("bidCode");
                String bidAmount = recvJsonData.getString("bidAmount");
                deleteTempDir();
                String bidDateEnd = sBidBC.loadBidEndDate(bidTenderName, bidBidCode);
                boolean result = false;
                String bidIndex;
                if (!Global.dateNowAfter(bidDateEnd) && bidManager.dateOK(bidDateEnd)) {
                    result = sBidBC.postAmount(bidTenderName, bidBidCode, bidAmount);
                    bidIndex = sBidBC.getMyIndex();
                    if (result) {
                        bidManager.insert(bidTenderName, bidBidCode, bidDateEnd, bidIndex, bidAmount, autorun);
                    }
                }
                data.put("postBidStatus", result);
            }
            //准备投标（招标者名称，标的编号）（招标者名称，标的编号，竞标结果）
            //delete
//            case "prepareBid" -> {
//                String bidTenderName = recvJsonData.getString("tenderName");
//                String bidBidCode = recvJsonData.getString("bidCode");
//
//                BidProcesser bidProcesser = new BidProcesser(sBidBC.getAccount(), bidTenderName, bidBidCode, sBidBC.getMyIndex(), sBidBC.getBidAmount(bidBidCode));
//                String BidDateEnd = sBidBC.loadBidEndDate(bidTenderName, bidBidCode);
//                data.put("bidTenderName", bidTenderName);
//                data.put("bidBidCode", bidBidCode);
//                data.put("bidResult", bidProcesser.ready(Global.getDate(BidDateEnd).getTime()));
//            }
            //审计结果页 准备审计（招标者名称，标的编号）（标的名称，标的内容，标的编号，投标人数，审计结果）
            case "prepareAudit" -> {
                auditTenderName = recvJson.getJSONObject("data").getString("tenderName");
                auditBidCode = recvJson.getJSONObject("data").getString("bidCode");
                sBidBC.loadBidDetail(auditTenderName, auditBidCode, 2, data);
            }
            //开始审计（是否为开始审计）（审计是否结束，(如果结束)审计结果，日志）
            case "startAudit" -> {
                boolean auditStart = recvJson.getBooleanValue("auditStart");
                if (auditStart) {//第一轮
                    auditProcesser = new AuditProcesser(sBidBC.getAccount(), auditTenderName, auditBidCode);
                    auditThread = new Thread(() -> {
                        auditProcesser.verify();
                    });
                    auditThread.start(); // 启动新线程
                    data.put("log", "");
                    data.put("finishStatus", false);
                } else {//后续轮
                    StringBuilder progress = new StringBuilder();
                    auditProcesser.getProgress(progress);
                    if (auditProcesser.isFinish()) {
                        data.put("auditResult", auditProcesser.getFinalResult());
                        if (role.compareTo("0") == 0)
                            data.put("auditBidderResult", auditProcesser.getAuditResult());
                    }
                    data.put("log", progress);
                    data.put("finishStatus", auditProcesser.isFinish());
                }
            }
            //退出登录，（无输入）（无输出）
            //todo: 删除临时文件
            case "logout" -> {
                createDone = false;
                if (bidManagerThread != null) {
                    bidManagerThread.interrupt();
                }
                sBidBC = null;
            }
            //区块链浏览器
            case "getChainInfo" -> {
                json.put("time", new Date().getTime());
                if (sBidBC != null)
                    sBidBC.getChainInfo(data);
            }
            default -> {
                System.err.println("unknown act: " + act);
                json.replace("result", false);
            }
        }
        json.put("data", data);
        return json.toJSONString();
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

}
