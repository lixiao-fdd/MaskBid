package org.MaskBid;

import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.model.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;

public class Parameters {
    private final SBid contract;
    private final String Table_SBid_Name;
    private String name;
    private BigInteger counts, p, q, g, h;
    private String dateStart, dateEnd;
    private String bidName;
    private String table_register_name;
    private String winner;
    private String sk;

    //Initialization
    public Parameters(SBid contract, String Table_SBid_Name) {
        this.contract = contract;
        this.Table_SBid_Name = Table_SBid_Name;
    }

    //Generate parameters and insert them into the table, and create Table_register
    //key : name, field : counts, p, q, h, g, !【winner】, !【sk】, 【dateStart】, 【dateEnd】, 【bidName】, table_register_name

    public boolean insert(String[] value) {
        try {
            // parameters
            if (value.length != 0) {
                counts = new BigInteger(value[0]);
                p = new BigInteger(value[1]);
                q = new BigInteger(value[2]);
                h = new BigInteger(value[3]);
                g = new BigInteger(value[4]);
                winner = "";
                sk = "";
                dateStart = value[5];
                dateEnd = value[6];
                bidName = value[7];
                table_register_name = "Reg_" + name;
            }
            //insert sBid parameters
            List<BigInteger> parameters = List.of(counts, p, q, h, g);
            List<String> info = List.of(dateStart, dateEnd, bidName, table_register_name);
            TransactionReceipt receipt = contract.insert_table_sBid(Table_SBid_Name, name, parameters, info);
            List<SBid.InsertTableSBidResultEventResponse> response = contract.getInsertTableSBidResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                return response.get(0).count.compareTo(new BigInteger("1")) == 0;
            }
        } catch (Exception e) {
            System.err.println("insert parameters error, message is " + e.getMessage());
            return false;
        }
        System.err.println("insert parameters event log not found, maybe transaction not exec.");
        return false;
    }

    //Read parameters
    public boolean read() {
//        System.out.println("@".repeat(20));
//        System.out.println("Table_SBid_Name: " + Table_SBid_Name + "\n" + "bidCode: " + name);
//        System.out.println("@".repeat(20));
        try {
            Tuple3<String, List<BigInteger>, List<String>> result = contract.select_table_sBid(Table_SBid_Name, name);
            counts = result.getValue2().get(0);
            p = result.getValue2().get(1);
            q = result.getValue2().get(2);
            h = result.getValue2().get(3);
            g = result.getValue2().get(4);
            winner = result.getValue3().get(0);
            sk = result.getValue3().get(1);
            dateStart = result.getValue3().get(2);
            dateEnd = result.getValue3().get(3);
            bidName = result.getValue3().get(4);
            table_register_name = result.getValue3().get(5);
        } catch (Exception e) {
            System.err.println("Bid " + name + " does not exist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //Delete parameters
    public boolean delete() {
        try {
            TransactionReceipt receipt = contract.remove_table_sBid(Table_SBid_Name, name);
            List<SBid.RemoveTableSBidResultEventResponse> response = contract.getRemoveTableSBidResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                return response.get(0).count.compareTo(new BigInteger("1")) == 0;
            }
        } catch (Exception e) {
            System.err.println("Delete parameters error, message is  " + e.getMessage());
        }
        System.err.println("event log not found, maybe transaction not exec.");
        return false;
    }

    // 结束后胜者更新自己的私钥
    public void update(String winner, String sk) {
        try {
            TransactionReceipt receipt = contract.update_table_sBid(Table_SBid_Name, name, winner, sk);
            List<SBid.UpdateTableRegisterResultEventResponse> response = contract.getUpdateTableRegisterResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                if (response.get(0).count.compareTo(new BigInteger("1")) != 0) {
                    System.err.print("Update sBid table failed.\n");
                }
            } else {
                System.err.println("Update sBid event log not found, maybe transaction not exec. ");
            }
        } catch (Exception e) {
            System.err.println("update sBid error, message is " + e.getMessage());
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public BigInteger getCounts() {
        if (this.counts.intValue() == 0) {
            int regCounts = 1;
            while (true) {
                Register register = new Register(contract, table_register_name, String.valueOf(regCounts), "");
                if (!register.read())
                    break;
                ++regCounts;
            }
            return new BigInteger(String.valueOf(regCounts - 1));
        }
        return this.counts;
    }

    public BigInteger getP() {
        return this.p;
    }

    public BigInteger getQ() {
        return this.q;
    }

    public BigInteger getH() {
        return this.h;
    }

    public BigInteger getG() {
        return this.g;
    }

    public String getTable_register_name() {
        return table_register_name;
    }

    public void setCounts(BigInteger counts) {
        this.counts = counts;
    }

    public String getDateStart() {
        return dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public String getBidName() {
        return bidName;
    }

    public String getWinner() {
        return winner;
    }

    public String getSk() {
        return sk;
    }
}
