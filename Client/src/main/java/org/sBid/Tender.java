package org.sBid;

import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.model.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;

//招标者
public class Tender {
    private final SBid contract;
    private final String Table_Tender_Name;
    private final String name;
    private String bidCounts;
    private String table_sBid_name;

    //Initialization
    public Tender(SBid contract, String Table_Tender_Name, String name) {
        this.contract = contract;
        this.Table_Tender_Name = Table_Tender_Name;
        this.name = name;
    }

    //key : name, field : bidCounts, table_sBid_name
    public boolean insert() {
        try {
            table_sBid_name = "Tender_" + this.name;
//            System.out.println("@".repeat(20));
//            System.out.println("contract: " + contract.getContractAddress() + "\n" + "Table_Tender_Name: " + Table_Tender_Name + "\n" + "tenderName: " + name + "\n" + "table_sBid_name: " + table_sBid_name);
//            System.out.println("@".repeat(20));
            TransactionReceipt receipt = contract.insert_table_tender(Table_Tender_Name, name, table_sBid_name);
            List<SBid.InsertTableTenderResultEventResponse> response = contract.getInsertTableTenderResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                return response.get(0).count.compareTo(new BigInteger("1")) == 0;
            }
        } catch (Exception e) {
            System.err.println("insert error, message is " + e.getMessage());
            e.printStackTrace();
            return false;
        }
//        System.err.println("insert event log not found, maybe transaction not exec.");
        return false;
    }

    //读取招标机构信息
    public boolean read() {
        try {
            Tuple3<String, String, String> result = contract.select_table_tender(Table_Tender_Name, name);
            bidCounts = result.getValue2();
            table_sBid_name = result.getValue3();
        } catch (Exception e) {
            System.err.println("Can not read " + name + ", Info: " + e.getMessage());
//            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int getCounts() {
        read();
        return Integer.parseInt(bidCounts);
    }

    //删除招标机构信息
    public boolean delete() {
        try {
            TransactionReceipt receipt = contract.remove_table_sBid(Table_Tender_Name, name);
            List<SBid.RemoveTableTenderResultEventResponse> response = contract.getRemoveTableTenderResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                return response.get(0).count.compareTo(new BigInteger("1")) == 0;
            }
        } catch (Exception e) {
            System.err.println("Delete tender Info error, message is  " + e.getMessage());
        }
        System.err.println("Delete event log not found, maybe transaction not exec.");
        return false;
    }

    //招标机构招标数量加一
    public void add() {
        try {
            TransactionReceipt receipt = contract.update_table_tender(Table_Tender_Name, name, String.valueOf(Integer.parseInt(bidCounts) + 1));
            List<SBid.UpdateTableTenderResultEventResponse> response = contract.getUpdateTableTenderResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                if (response.get(0).count.compareTo(new BigInteger("1")) != 0) {
                    System.err.print("Update Tender table failed.\n");
                }
            } else {
                System.err.println("Update Tender event log not found, maybe transaction not exec. ");
            }
        } catch (Exception e) {
            System.err.println("update Tender error, message is " + e.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public String getTable_sBid_name() {
        return table_sBid_name;
    }
}
