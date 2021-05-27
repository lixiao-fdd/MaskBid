package org.sBid;

import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.model.TransactionReceipt;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Register {
    private final SBid contract;
    private final String Table_Register_Name;
    private final String rootPath;
    private String index = "";
    private String name = "";
    private String accountPk = "";
    private String public_key = "";
    private String cipher_amount = "";
    private String result = "";
    private String status = "";
    private String lastFinishRound = "";
    private String table_files_name = "";

    // Insert account infomations and create Table_files
    public Register(SBid contract, String Table_Register_Name, String index, String rootPath) {
        this.contract = contract;
        this.Table_Register_Name = Table_Register_Name;
        this.index = index;
        this.rootPath = rootPath;
    }

    // Insert account infomations and create Table_files
    public Register() {
        this.contract = null;
        this.Table_Register_Name = "";
        this.rootPath = "";
    }

    //key : index, field : name, 【mainPK】, 【sBidPK】, cipher_amount, result, table_files_name
    public boolean insert(String name, String pk) {
        this.result = "1,0";
        this.table_files_name = "Bidder_" + Global.sha1(pk);
        //load public_key
        String filePath = rootPath + "pk" + index + ".txt";
        Global.waitFile(filePath);
        ArrayList<String> fileContent = new ArrayList<>();
        Global.readFile(filePath, fileContent);
        public_key = fileContent.get(0);
        fileContent.clear();
        //load cipher_amount
        filePath = rootPath + "cipherAmount" + index + ".txt";
        Global.waitFile(filePath);
        Global.readFile(filePath, fileContent);
        cipher_amount = fileContent.get(0);
        //insert account infomations
        List<String> info_list = List.of(name, pk, public_key, cipher_amount, result, table_files_name);
        TransactionReceipt receipt = contract.insert_table_register(Table_Register_Name, index, info_list);
        List<SBid.InsertTableRegisterResultEventResponse> response = contract.getInsertTableRegisterResultEvents(receipt);//回执
        if (!response.isEmpty()) {
            return response.get(0).count.compareTo(new BigInteger("1")) == 0;
        }
        System.err.println("event log not found, maybe transaction not exec.");
        return false;
    }

    // Read registration infomations
    public boolean read() {
        try {
            Tuple3<String, List<String>, String> info_list = contract.select_table_register(Table_Register_Name, index);
            index = info_list.getValue1();
            name = info_list.getValue2().get(0);
            accountPk = info_list.getValue2().get(1);
            public_key = info_list.getValue2().get(2);
            cipher_amount = info_list.getValue2().get(3);
            result = info_list.getValue2().get(4);
            table_files_name = info_list.getValue3();
            if (result.compareTo("lose") != 0 && result.compareTo("win") != 0) {
                String[] res = result.split(",");
                status = res[0];
                lastFinishRound = res[1];
            }
        } catch (Exception e) {
//            System.out.println("read registration infomations error, message is " + e.getMessage());
            return false;
        }
        return true;
    }

    // Update registration infomations
    public void update(String result) {
        try {
//            result = "lose";
            TransactionReceipt receipt = contract.update_table_register(Table_Register_Name, index, result);
            List<SBid.UpdateTableRegisterResultEventResponse> response = contract.getUpdateTableRegisterResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                if (response.get(0).count.compareTo(new BigInteger("1")) != 0) {
                    System.err.print("Update registration infomations failed.\n");
                }
            } else {
                System.err.println("event log not found, maybe transaction not exec. ");
            }
        } catch (Exception e) {
            System.err.println("update registration infomations error, message is " + e.getMessage());
        }
    }

    // Delete registration infomations
    public boolean delete() {
        try {
            TransactionReceipt receipt = contract.remove_table_register(Table_Register_Name, index);
            List<SBid.RemoveTableRegisterResultEventResponse> response = contract.getRemoveTableRegisterResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                return response.get(0).count.compareTo(new BigInteger("1")) == 0;
            }
        } catch (Exception e) {
            System.err.println("Delete registration infomations error, message is " + e.getMessage());
        }
        System.err.println("event log not found, maybe transaction not exec.");
        return false;
    }

    public void copy(Register obj) {
        this.index = obj.getIndex();
        this.name = obj.getName();
        this.accountPk = obj.getAccountPk();
        this.public_key = obj.getPublic_key();
        this.cipher_amount = obj.getCipher_amount();
        this.result = obj.getResult();
        this.table_files_name = obj.getTable_files_name();
        this.status = obj.getStatus();
        this.lastFinishRound = obj.getLastFinishRound();
    }

    public String getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getAccountPk() {
        return accountPk;
    }

    public String getPublic_key() {
        return public_key;
    }

    public String getCipher_amount() {
        return cipher_amount;
    }

    public String getResult() {
        return result;
    }

    public String getTable_files_name() {
        return table_files_name;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public String getLastFinishRound() {
        return lastFinishRound;
    }
}
