package org.sBid;

import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.model.TransactionReceipt;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Files {
    private final SBid contract;
    private final String Table_Files_Name;
    private final String rootPath;
    private String file_name;
    private String file_content;

    // Initialization
    public Files(SBid contract, String Table_Files_Name, String rootPath) {
        this.contract = contract;
        this.Table_Files_Name = Table_Files_Name;
        this.rootPath = rootPath;
    }

    // insert file
    public boolean insert(String file_name) {
        try {
            //load file
            StringBuilder fileContent = new StringBuilder();
            if(!Global.readFile(rootPath + file_name, fileContent))
                return false;
            TransactionReceipt receipt = contract.insert_table_files(Table_Files_Name, file_name, fileContent.toString());
            List<SBid.InsertTableFilesResultEventResponse> response = contract.getInsertTableFilesResultEvents(receipt);//回执
            if (!response.isEmpty()) {
                if (response.get(0).count.compareTo(new BigInteger("1")) != 0) {
                    System.err.print("Insert file failed.\n");
                }
            } else {
                System.err.println("Insert file event log not found, maybe transaction not exec. ");
            }
        } catch (Exception e) {
            System.err.println("insert file error, message is " + e.getMessage());
        }
        return true;
    }

    //Read file
    public boolean read(String file_name) {
        try {
            Tuple2<String, String> result = contract.select_table_files(Table_Files_Name, file_name);
            this.file_name = result.getValue1();
            this.file_content = result.getValue2();
        } catch (Exception e) {
            System.err.println("File " + file_name + " does not exist");
            return false;
        }
        return true;
    }

    //Delete file
    public void delete(String file_name) {
        try {
            TransactionReceipt receipt = contract.remove_table_files(Table_Files_Name, file_name);
            List<SBid.RemoveTableFilesResultEventResponse> response = contract.getRemoveTableFilesResultEvents(receipt);//回执

            if (response.isEmpty()) {
                System.err.println("Delete file event log not found, maybe transaction not exec. ");
            }
        } catch (Exception e) {
            System.err.println("Delete file error, message is  " + e.getMessage());
        }
    }


    public String getFile_name() {
        return file_name;
    }

    public String getFile_content() {
        return file_content;
    }
}
