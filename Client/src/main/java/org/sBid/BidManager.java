package org.sBid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

//功能：
//0、插入已经投标的标的
//1、将等待中的标的按开始时间先后顺序排列
//2、某个标的到了开始竞标时间后开启新线程，参与竞标
//3、更新竞标结果
public class BidManager {
    private final Account account;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分");
    private final SimpleDateFormat sdfLOG = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private TreeSet<BidItem> set = new TreeSet<>();

    //todo 从链上初始化
    public BidManager(Account account, JSONObject storeJson, String realName) {
        this.account = account;
        if (storeJson != null) {
            JSONArray storeItemList = storeJson.getJSONArray("store");
            for (Object storeItem : storeItemList) {
                JSONObject item = (JSONObject) storeItem;
                String bidTenderName = item.getString("tenderName");
                String bidBidCode = item.getString("bidCode");
                String bidAmount = item.getString("amount");
                String bidSk = item.getString("sk");
                Parameters parameters = new Parameters(account.getContract(), "Tender_" + Global.sha1(bidTenderName));
                parameters.setName(bidBidCode);
                boolean errFlag = false;
                if (parameters.read()) {
                    String bidDate = parameters.getDateEnd();
                    if (!Global.dateNowAfter(bidDate)) {
                        int index = 1;
                        while (true) {
                            Register register = new Register(account.getContract(), parameters.getTable_register_name(), String.valueOf(index), "");
                            if (!register.read()) {
                                errFlag = true;
                                break;
                            }
                            if (register.getName().compareTo(realName) == 0)
                                break;
                            ++index;
                        }
                        if (errFlag) {
                            errFlag = false;
                            continue;
                        }
                        Register register = new Register(account.getContract(), parameters.getTable_register_name(), String.valueOf(index), "");
                        register.read();
                        String bidPk = register.getPublic_key();
                        String bidCipherAmount = register.getCipher_amount();
                        //创建文件夹
                        String bidFile = account.getFilesPath() + "Tender_" + Global.sha1(bidTenderName) + "_" + bidBidCode + File.separator;
                        Global.createFolder(bidFile);
                        String amopFile = bidFile + "amopFile" + File.separator;
                        Global.createFolder(amopFile);
                        String coreFile = bidFile + "coreFile" + File.separator;
                        Global.createFolder(coreFile);
                        //parameters.txt
                        Global.writeFile(bidFile + "parameters.txt", parameters.getP().toString() + "\n" + parameters.getQ().toString() + "\n" + parameters.getH().toString() + "\n" + parameters.getG().toString());
                        Global.writeFile(coreFile + "parameters.txt", parameters.getP().toString() + "\n" + parameters.getQ().toString() + "\n" + parameters.getH().toString() + "\n" + parameters.getG().toString());
                        //cipherAmount1.txt
                        Global.writeFile(amopFile + "cipherAmount" + index + ".txt", bidCipherAmount);
                        Global.writeFile(coreFile + "cipherAmount" + index + ".txt", bidCipherAmount);
                        //plaintext_int1.txt
                        Global.writeFile(amopFile + "plaintext_int" + index + ".txt", bidAmount);
                        Global.writeFile(coreFile + "plaintext_int" + index + ".txt", bidAmount);
                        //sk1.txt
                        Global.writeFile(amopFile + "sk" + index + ".txt", bidSk);
                        Global.writeFile(coreFile + "sk" + index + ".txt", bidSk);
                        //pk1.txt
                        Global.writeFile(amopFile + "pk" + index + ".txt", bidPk);
                        Global.writeFile(coreFile + "pk" + index + ".txt", bidPk);
                        insert(bidTenderName, bidBidCode, bidDate, String.valueOf(index), bidAmount);
                    }
                }
            }
        }
        System.out.println("Bids:" + set.toString());
    }

    //每秒检查一次是否有需要开始的竞标
    public void check() {
        Date dateNow = new Date();
        if (!set.isEmpty()) {
            long clock = set.first().getTimestamp() - dateNow.getTime();
            if (clock < 6000) {
                System.out.println(sdfLOG.format(dateNow) + " --- " + "还有五秒开始竞拍: " + set.first().getCode());
                BidItem bidReadyToGo = set.first();
                Thread bidThread = new Thread(() -> {
                    BidProcesser bidProcesser = new BidProcesser(account, bidReadyToGo.getTender(), bidReadyToGo.getCode(), bidReadyToGo.getIndex(), bidReadyToGo.getAmount());
                    bidProcesser.ready(bidReadyToGo.getTimestamp());
                });
                bidThread.start(); // 启动新线程
                set.remove(set.first());//移除
                if (!set.isEmpty()) {
                    System.out.println(sdfLOG.format(dateNow) + " --- " + "下一个是: " + set.first().getCode());
                    //递归一下看下一个是否也要开始了
                }
            }
        }
    }

    //插入新的标的
    public boolean insert(String tender, String code, String dateEnd, String bidIndex, String bidAmount) {
        Date clock = new Date();
        try {
            clock = sdf.parse(dateEnd);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return set.add(new BidItem(tender, code, clock.getTime(), bidIndex, bidAmount));
    }

    //在已插入的标的中寻找是否有冲突存在

    public boolean dateOK(String date) {
        Date dateIn = new Date();
        try {
            dateIn = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long timeIn = dateIn.getTime();
        for (BidItem item : set) {
            long itemTime = item.getTimestamp();
            if ((itemTime > timeIn - 60000) && (itemTime < timeIn + 60000))
                return false;
            if (itemTime > timeIn + 60000)
                return true;
        }
        return true;
    }
}

class BidItem implements Comparable {
    private final String tender;
    private final String code;
    private final String index;
    private final String amount;
    private final long timestamp;

    @Override
    public String toString() {
        return "\nBid{" +
                "index: " + index +
                " | amount: " + amount +
                " | tender: " + tender +
                " | code: " + code +
                " | timestamp: " + timestamp +
                '}';
    }

    public BidItem(String tender, String code, long timestamp, String bidIndex, String bidAmount) {
        this.tender = tender;
        this.code = code;
        this.index = bidIndex;
        this.amount = bidAmount;
        this.timestamp = timestamp;
        System.out.println("Insert: " + this.toString());
    }

    public int compareTo(Object obj) {
        BidItem s = (BidItem) obj;
        if (this.code.compareTo(s.getCode()) == 0)
            return 0;
        else if (this.timestamp <= s.getTimestamp())
            return -1;
        return 1;
    }

    public String getTender() {
        return tender;
    }

    public String getCode() {
        return code;
    }

    public String getIndex() {
        return index;
    }

    public String getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

}