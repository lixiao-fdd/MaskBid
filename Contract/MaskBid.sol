//SPDX-License-Identifier: UNLICENSED;
pragma solidity >=0.4.24 <0.6.11;
pragma experimental ABIEncoderV2;

import "./Table.sol";

contract MaskBid {
    event InsertTableTenderResult(int256 count);
    event UpdateTableTenderResult(int256 count);
    event RemoveTableTenderResult(int256 count);

    event InsertTableSBidResult(int256 count);
    event RemoveTableSBidResult(int256 count);

    event InsertTableRegisterResult(int256 count);
    event UpdateTableRegisterResult(int256 count);
    event RemoveTableRegisterResult(int256 count);

    event InsertTableFilesResult(int256 count);
    event RemoveTableFilesResult(int256 count);

    TableFactory tableFactory;

    //构造函数，创建Table_tender
    constructor(string memory Table_Tender_Name) public {
        tableFactory = TableFactory(0x1001);
        // 招标者登记表, key : name, field : bidCounts, Table_SBid_Name

        // 创建表
        tableFactory.createTable(
            Table_Tender_Name,
            "name",
            "bidCounts,Table_SBid_Name"
        );
    }

    //创建Table_sBid
    function create_table_sBid(string memory Table_SBid_Name) private {
        tableFactory = TableFactory(0x1001);
        // 竞标参数管理表, key : name, field : counts, p, q, h, g, winner, sk, dateStart, dateEnd, bidName, table_register_name

        // 创建表
        tableFactory.createTable(
            Table_SBid_Name,
            "name",
            "counts,p,q,h,g,winner,sk,dateStart,dateEnd,bidName,table_register_name"
        ); //更改了
    }

    //创建Table_register
    function create_table_register(string memory Table_Register_Name) private {
        tableFactory = TableFactory(0x1001);
        // 竞标人员注册表, key : index, field : name, mainPK, sBidPK, cipher_amount, result, table_files_name

        // 创建表
        tableFactory.createTable(
            Table_Register_Name,
            "index",
            "name,mainPK,sBidPK,cipher_amount,result,table_files_name"
        ); //要更改
    }

    //创建Table_files
    function create_table_files(string memory Table_Files_Name) private {
        tableFactory = TableFactory(0x1001);
        // 竞标人员文件表, key : File_name, field : file_content

        // 创建表
        tableFactory.createTable(Table_Files_Name, "file_name", "file_content");
    }

    //插入Table_tender项
    function insert_table_tender(
        string memory Table_Tender_Name,
        string memory name,
        string memory Table_SBid_Name
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Tender_Name);

        Entry entry = table.newEntry();
        entry.set("name", name);
        entry.set("bidCounts", "0");
        entry.set("Table_SBid_Name", Table_SBid_Name);
        create_table_sBid(Table_SBid_Name);

        int256 count = table.insert(name, entry);
        emit InsertTableTenderResult(count);

        return count;
    }

    //查找Table_tender项
    function select_table_tender(
        string memory Table_Tender_Name,
        string memory name
    )
        public
        view
        returns (
            string memory,
            string memory,
            string memory
        )
    {
        Table table = tableFactory.openTable(Table_Tender_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("name", name);
        // 数据记录集
        Entries entries = table.select(name, condition);
        Entry entry = entries.get(0);
        string memory bidCounts = entry.getString("bidCounts");
        string memory Table_SBid_Name = entry.getString("Table_SBid_Name");

        return (name, bidCounts, Table_SBid_Name);
    }

    //更新Table_tender项
    function update_table_tender(
        string memory Table_Tender_Name,
        string memory name,
        string memory bidCounts
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Tender_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("name", name);
        // 数据记录集
        Entries entries = table.select(name, condition);
        Entry entry = entries.get(0);
        //新记录
        Entry entry_new = table.newEntry();
        entry_new.set("bidCounts", bidCounts);
        entry_new.set("Table_SBid_Name", entry.getString("Table_SBid_Name"));

        int256 ans = table.update(name, entry_new, condition);
        emit UpdateTableTenderResult(ans);
        return ans;
    }

    //删除Table_tender项
    function remove_table_tender(
        string memory Table_Tender_Name,
        string memory name
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Tender_Name);

        Condition condition = table.newCondition();
        condition.EQ("name", name);

        int256 result = table.remove(name, condition);
        emit RemoveTableTenderResult(result);
        return result;
    }

    //插入Table_sBid项
    function insert_table_sBid(
        string memory Table_SBid_Name,
        string memory name,
        int256[] memory parameters_list,
        string[] memory info_list
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_SBid_Name);

        Entry entry = table.newEntry();
        entry.set("name", name);
        entry.set("counts", parameters_list[0]);
        entry.set("p", parameters_list[1]);
        entry.set("q", parameters_list[2]);
        entry.set("h", parameters_list[3]);
        entry.set("g", parameters_list[4]);

        entry.set("winner", "");
        entry.set("sk", "");
        entry.set("dateStart", info_list[0]);
        entry.set("dateEnd", info_list[1]);
        entry.set("bidName", info_list[2]);
        entry.set("table_register_name", info_list[3]);
        create_table_register(info_list[3]);

        int256 count = table.insert(name, entry);
        emit InsertTableSBidResult(count);

        return count;
    }

    //查找Table_sBid项
    function select_table_sBid(
        string memory Table_SBid_Name,
        string memory name
    )
        public
        view
        returns (
            string memory,
            int256[] memory,
            string[] memory
        )
    {
        Table table = tableFactory.openTable(Table_SBid_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("name", name);
        // 数据记录集
        Entries entries = table.select(name, condition);

        int256[] memory parameters_list = new int256[](uint256(5));
        string[] memory info_list = new string[](uint256(6));
        Entry entry = entries.get(0);

        parameters_list[uint256(0)] = entry.getInt("counts");
        parameters_list[uint256(1)] = entry.getInt("p");
        parameters_list[uint256(2)] = entry.getInt("q");
        parameters_list[uint256(3)] = entry.getInt("h");
        parameters_list[uint256(4)] = entry.getInt("g");

        info_list[uint256(0)] = entry.getString("winner");
        info_list[uint256(1)] = entry.getString("sk");
        info_list[uint256(2)] = entry.getString("dateStart");
        info_list[uint256(3)] = entry.getString("dateEnd");
        info_list[uint256(4)] = entry.getString("bidName");
        info_list[uint256(5)] = entry.getString("table_register_name");

        return (name, parameters_list, info_list);
    }

    //更新Table_register项
    function update_table_sBid(
        string memory Table_SBid_Name,
        string memory name,
        string memory winner,
        string memory sk
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_SBid_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("name", name);
        // 数据记录集
        Entries entries = table.select(name, condition);
        Entry entry = entries.get(0);
        //新记录
        Entry entry_new = table.newEntry();
        entry_new.set("counts", entry.getInt("counts"));
        entry_new.set("p", entry.getInt("p"));
        entry_new.set("q", entry.getInt("q"));
        entry_new.set("h", entry.getInt("h"));
        entry_new.set("g", entry.getInt("g"));
        entry_new.set("winner", winner);
        entry_new.set("sk", sk);
        entry_new.set("dateStart", entry.getString("dateStart"));
        entry_new.set("dateEnd", entry.getString("dateEnd"));
        entry_new.set("bidName", entry.getString("bidName"));
        entry_new.set(
            "table_register_name",
            entry.getString("table_register_name")
        );

        int256 ans = table.update(name, entry_new, condition);
        emit UpdateTableRegisterResult(ans);
        return ans;
    }

    //删除Table_sBid项
    function remove_table_sBid(
        string memory Table_SBid_Name,
        string memory name
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_SBid_Name);

        Condition condition = table.newCondition();
        condition.EQ("name", name);

        int256 result = table.remove(name, condition);
        emit RemoveTableSBidResult(result);
        return result;
    }

    //插入Table_register项
    function insert_table_register(
        string memory Table_Register_Name,
        string memory index,
        string[] memory account_info_list
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Register_Name);

        Entry entry = table.newEntry();
        entry.set("index", index);
        entry.set("name", account_info_list[0]);
        entry.set("mainPK", account_info_list[1]);
        entry.set("sBidPK", account_info_list[2]);
        entry.set("cipher_amount", account_info_list[3]);
        entry.set("result", account_info_list[4]);
        entry.set("table_files_name", account_info_list[5]);
        create_table_files(account_info_list[5]);

        int256 count = table.insert(index, entry);
        emit InsertTableRegisterResult(count);
        return count;
    }

    //查找Table_register项
    function select_table_register(
        string memory Table_Register_Name,
        string memory index
    )
        public
        view
        returns (
            string memory,
            string[] memory,
            string memory
        )
    {
        Table table = tableFactory.openTable(Table_Register_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("index", index);
        // 数据记录集
        Entries entries = table.select(index, condition);
        Entry entry = entries.get(0);

        string[] memory account_info_list = new string[](uint256(5));
        account_info_list[uint256(0)] = entry.getString("name");
        account_info_list[uint256(1)] = entry.getString("mainPK");
        account_info_list[uint256(2)] = entry.getString("sBidPK");
        account_info_list[uint256(3)] = entry.getString("cipher_amount");
        account_info_list[uint256(4)] = entry.getString("result");
        string memory table_files_name = entry.getString("table_files_name");
        return (index, account_info_list, table_files_name);
    }

    //更新Table_register项
    function update_table_register(
        string memory Table_Register_Name,
        string memory index,
        string memory result
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Register_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("index", index);
        // 数据记录集
        Entries entries = table.select(index, condition);
        Entry entry = entries.get(0);
        //新记录
        Entry entry_new = table.newEntry();
        entry_new.set("index", entry.getString("index"));
        entry_new.set("name", entry.getString("name"));
        entry_new.set("mainPK", entry.getString("mainPK"));
        entry_new.set("sBidPK", entry.getString("sBidPK"));
        entry_new.set("cipher_amount", entry.getString("cipher_amount"));
        entry_new.set("result", result);
        entry_new.set("table_files_name", entry.getString("table_files_name"));

        int256 ans = table.update(index, entry_new, condition);
        emit UpdateTableRegisterResult(ans);
        return ans;
    }

    //删除Table_register项
    function remove_table_register(
        string memory Table_Register_Name,
        string memory index
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Register_Name);

        Condition condition = table.newCondition();
        condition.EQ("index", index);

        int256 result = table.remove(index, condition);
        emit RemoveTableRegisterResult(result);
        return result;
    }

    //插入Table_files项
    function insert_table_files(
        string memory Table_Files_Name,
        string memory file_name,
        string memory file_content
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Files_Name);

        Entry entry = table.newEntry();
        entry.set("file_name", file_name);
        entry.set("file_content", file_content);

        int256 ans = table.insert(file_name, entry);
        emit InsertTableFilesResult(ans);

        return ans;
    }

    //查找Table_files项
    function select_table_files(
        string memory Table_Files_Name,
        string memory file_name
    ) public view returns (string memory, string memory) {
        Table table = tableFactory.openTable(Table_Files_Name);
        // 查询条件
        Condition condition = table.newCondition();
        condition.EQ("file_name", file_name);
        // 数据记录集
        Entries entries = table.select(file_name, condition);

        Entry entry = entries.get(0);

        string memory file_content = entry.getString("file_content");

        return (file_name, file_content);
    }

    //删除Table_files项
    function remove_table_files(
        string memory Table_Files_Name,
        string memory file_name
    ) public returns (int256) {
        tableFactory = TableFactory(0x1001);
        Table table = tableFactory.openTable(Table_Files_Name);

        Condition condition = table.newCondition();
        condition.EQ("file_name", file_name);

        int256 ans = table.remove(file_name, condition);
        emit RemoveTableFilesResult(ans);
        return ans;
    }
}
