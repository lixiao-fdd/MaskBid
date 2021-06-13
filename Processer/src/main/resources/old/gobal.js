let test;
let bidFinish = false;
let bidPost = false;
let accountLoadFinish = false;
let loginFinish = false;
let loginFaild = false;
let registAlready = false;
let bidderAccountList = [];
let tenderAccountList = [];
let childList = [];
let pageNow = 1;
let roleFlag = 1;
let accountName;
let pageId = ["bidBox", "auditBox", "settingBox", "aboutBox"];
let checkList = [];
let auditResultList = [];
let oldBidIndexList = [];

//创建JSON
function sendJson(data, functionName) {
    $.ajax({
        type: "POST",
        url: "/json",
        contentType: 'application/json',
        async: true,
        data: JSON.stringify(data),
        dataType: 'json',
        success: function (dat) {
            functionName(dat);
        },
        error: function (e) {
            console.log("ERROR: ", e);
        }
    });
}

//重置链上数据
function reset() {
    let tenderNameReset = document.getElementById("tenderNameReset").value;
    let act = "GodModReset";
    let data = {"act": act, "tenderNameReset": tenderNameReset};
    sendJson(data, callbackReset);
    layer.load(2);
}

//回调 重置链上数据 
function callbackReset(json) {
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    else
        alert("重置完成");
}

//请求已保存的账户列表
function listAccount() {
    let act = "listAccount";
    let data = {"act": act};
    sendJson(data, callbackListAccount);
    layer.load(2);
}

//回调 列出已保存的账户列表
function callbackListAccount(json) {
    layer.closeAll('loading');
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    console.log(json);//delete
    let loginAccountContainer = document.getElementById("loginAccountContainerID");
    loginAccountContainer.innerHTML = "";
    tenderAccountList.length = 0;
    bidderAccountList.length = 0;
    for (let i = 0; i < json.accountList.length; i++) {
        let fullName = json.accountList[i];
        let name = fullName.substring(0, fullName.lastIndexOf(":"));
        let role = fullName.substring(fullName.lastIndexOf(":") + 1);
        if (role  ==  '0') {//招标者
            tenderAccountList[tenderAccountList.length] = name;
        } else {//投标者
            bidderAccountList[bidderAccountList.length] = name;
        }
    }
    switchToRole(roleFlag);//刷新列表
    accountLoadFinish = true;
}

//切换角色
function switchToRole(role) {
    let accountList;
    let calssCountent;
    let loginAccountContainer = document.getElementById("loginAccountContainerID");
    loginAccountContainer.innerHTML = "";
    let loginBoxTitleBoxBidder = document.getElementById("loginBoxTitleBoxBidderID");
    let loginBoxTitleBoxTender = document.getElementById("loginBoxTitleBoxTenderID");
    if (role  ==  1) {
        accountList = bidderAccountList;
        calssCountent = "loginAccountBidder";
        loginBoxTitleBoxBidder.classList.replace("loginBoxTitleBoxWait", "loginBoxTitleBoxBidder");
        loginBoxTitleBoxTender.classList.replace("loginBoxTitleBoxTender", "loginBoxTitleBoxWait");
        roleFlag = 1;
    } else {
        accountList = tenderAccountList;
        calssCountent = "loginAccountTender";
        loginBoxTitleBoxTender.classList.replace("loginBoxTitleBoxWait", "loginBoxTitleBoxTender");
        loginBoxTitleBoxBidder.classList.replace("loginBoxTitleBoxBidder", "loginBoxTitleBoxWait");
        roleFlag = 0;
    }
    for (let i = 0; i < accountList.length; i++) {
        let account = accountShow(accountList[i], calssCountent);
        loginAccountContainer.appendChild(account);
    }
}

//列出已保存的账户
function accountShow(name, calssCountent) {
    let account = document.createElement('div');
    let cover = document.createElement('div');
    let accountName = document.createElement('p');
    accountName.innerText = name;
    cover.classList.add("cover");
    account.classList.add("loginAccount", calssCountent);
    account.append(cover);
    account.append(accountName);
    account.setAttribute("onclick", "login(this)")
    return account;
}

//打开注册框
function startRegist() {
    let registerBox = document.getElementById("registerBox");
    registerBox.style.display = "block";
    UploadReturn();
    closeDelete();
}

//关闭注册框
function registReturn() {
    let registerBox = document.getElementById("registerBox");
    registerBox.style.display = "none";
}

//注册
function postRegist() {
    let newName = document.getElementById("newName").value;
    let act = "createAccount";
    let data = {"act": act, "name": newName + ":" + roleFlag};
    sendJson(data, callbackListAccount);
    registReturn();
    layer.load(2);
}

//登录账号
function login(thisObject) {
    let name = thisObject.innerText;
    let act = "login";
    let data = {"act": act, "name": name + ":" + roleFlag};
    sendJson(data, callbackLogin);
    layer.load(2);
}

//回调 登录账号
function callbackLogin(json) {
    layer.closeAll('loading');
    console.log(json);//delete

    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    if (json.result) {//进入主页
        loginFinish = true;
        if (json.role  ==  '0') {
            window.location.replace("./tender.html");
        } else {
            window.location.replace("./bidder.html");
        }
    } else {//登陆失败
        loginFaild = true;
        alert("登录失败");
    }
}

//请求账户信息
function listAccountInfo() {
    let act = "listAccountInfo";
    let data = {"act": act};
    sendJson(data, callbackListAccountInfo);
    layer.load(2);
}

//回调 接收账户信息
function callbackListAccountInfo(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    let accountInfoName = document.getElementById("accountInfoName");
    let accountInfoAddrress = document.getElementById("accountInfoAddrress");
    let accountInfoPK = document.getElementById("accountInfoPK");
    let mianName = document.getElementById("mianName");
    registAlready = json.registAlready;
    accountName = json.name;
    accountInfoName.innerText = accountName;
    accountInfoAddrress.innerText = json.address;
    accountInfoPK.innerText = json.pk;
    mianName.innerText = accountName;
    roleFlag = json.role;
    document.getElementById("mainAccountNameShow").innerText = accountName;
    if (roleFlag == '0') {
        listOldBids();
    }
}

//切换页面
function switchPage(thisObj) {
    let switchName = thisObj.innerText;
    let bidBox = document.getElementById("bidBoxID");
    let auditBox = document.getElementById("auditBoxID");
    let settingBox = document.getElementById("settingBoxID");
    let bidSide = document.getElementById("bidSide");
    let auditSide = document.getElementById("auditSide");
    switch (switchName) {
        case '招标'://1
            bidBox.classList.remove("layui-hide");
            auditBox.classList.add("layui-hide");
            settingBox.classList.add("layui-hide");
            bidSide.classList.add("layui-this");
            auditSide.classList.remove("layui-this");
            break;
        case '审计'://2
            bidBox.classList.add("layui-hide");
            auditBox.classList.remove("layui-hide");
            settingBox.classList.add("layui-hide");
            bidSide.classList.remove("layui-this");
            auditSide.classList.add("layui-this");
            break;
        case '用户信息'://3
            bidBox.classList.add("layui-hide");
            auditBox.classList.add("layui-hide");
            settingBox.classList.remove("layui-hide");
            bidSide.classList.remove("layui-this");
            auditSide.classList.remove("layui-this");
            break;
        default:
            break;
    }
}

//打开修改投标对象界面
function openBidModify() {
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById("bidBoxCoverID").classList.remove("layui-hide");
    document.getElementById("fixBidBoxID").classList.add("boxBlur");
}

//关闭修改投标对象界面
function closeBidModify() {
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
}

//设置投标对象
function setBid() {
    let tenderNameValue = document.getElementById("bidModifySearchUnitInput").value;
    let bidCodeValue = document.getElementById("bidModifySearchCodeInput").value;
    if (tenderNameValue  !=  "" && bidCodeValue  !=  "") {
        let act = "setBid";
        let data = {"act": act, "tenderName": tenderNameValue, "bidCode": bidCodeValue};
        sendJson(data, callbackSetBid);
        layer.load(2);
    }
}

//回调 接收投标对象的信息，发布日期， 截止日期，报价状态，竞标人数，招标名称以及是否已报价和报价金额
function callbackSetBid(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");

    if (json.result) {
        let tenderName = document.getElementById("tenderName");
        tenderName.innerText = json.tenderName;
        let bidCode = document.getElementById("bidCode");
        bidCode.innerText = json.bidCode;
        let dateStartValue = json.dateStart;
        let dateEndValue = json.dateEnd;
        let countsValue = json.counts;
        let bidNameValue = json.bidName;
        let statusValue = json.status;
        let registAlready = json.registAlready;

        let dateStart = document.getElementById("dateStart");
        dateStartValue = dateStartValue.replace("_", " ");
        dateStart.innerText = dateStartValue;
        let dateEnd = document.getElementById("dateEnd");
        dateEndValue = dateEndValue.replace("_", " ");
        dateEnd.innerText = dateEndValue;
        let status = document.getElementById("status");
        status.innerText = statusValue;
        let counts = document.getElementById("counts");
        counts.innerText = countsValue;
        let bidName = document.getElementById("bidName");
        bidName.innerText = bidNameValue;
        if (statusValue  ==  "进行中") {
            document.getElementById("bidContentBox").classList.remove("layui-hide");
            //若已报价，则加载投标信息
            if (registAlready) {
                bidPost = true;
                document.getElementById("bidContentBox").classList.add("layui-hide");
                document.getElementById("bidProgressBox").classList.remove("layui-hide");
                document.getElementById("bidInfoBox").classList.remove("layui-hide");
                document.getElementById("bidNameMe").innerText = accountName;
                refreshBidContent();
                let bidAmountMe = document.getElementById("bidAmountMe");
                bidAmountMe.innerText = json.amount;
            } else {
                bidInfoClose();
            }
        } else if (statusValue  ==  "已结束") {
            document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
            document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
            //若已报价，则加载投标信息
            if (registAlready) {
                bidPost = true;
                document.getElementById("bidContentBox").classList.add("layui-hide");
                document.getElementById("bidProgressBox").classList.remove("layui-hide");
                document.getElementById("bidInfoBox").classList.remove("layui-hide");
                document.getElementById("bidNameMe").innerText = accountName;
                refreshBidContent();
                let bidAmountMe = document.getElementById("bidAmountMe");
                bidAmountMe.innerText = json.amount;
            } else {
                document.getElementById("bidContentBox").classList.add("layui-hide");
                document.getElementById("bidProgressBox").classList.add("layui-hide");
                document.getElementById("bidInfoBox").classList.add("layui-hide");

            }
        }

        closeBidModify();
    } else
        alert("招标不存在");

}

//读取投标内容中投标金额的值，并固定
function readBidderAmount() {
    let amountInputValue = document.getElementById("bidderAmount").value;
    if (amountInputValue  !=  "" && amountInputValue > 0 && amountInputValue < 4294967295) {
        let amountInput = document.getElementById("bidderAmountInput");
        let amountButton = document.getElementById("bidderAmountButton");
        amountInput.innerHTML = "<p class=\"bidFormTableValue\" id=\"bidderAmountTrue\">" + amountInputValue + "</p>";
        amountButton.innerText = "重置金额";
        amountButton.setAttribute("onclick", "resetBidderAmount()");
    }
}

//重置投标内容中投标金额的值
function resetBidderAmount() {
    let amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    let amountInput = document.getElementById("bidderAmountInput");
    let amountButton = document.getElementById("bidderAmountButton");
    amountInput.innerHTML = '<input type="number" name="bidAccount" required lay-verify="required" placeholder="请输入投标金额" autocomplete="off" class="layui-input" id="bidderAmount" value="' + amountInputValue + '">';
    amountButton.innerText = "确认金额";
    amountButton.setAttribute("onclick", "readBidderAmount()");
}

//重置投标内容
function resetBidderCotent() {
    document.getElementById("bidContentBox").classList.remove("layui-hide");
    document.getElementById("bidProgressBox").classList.add("layui-hide");
    document.getElementById("bidInfoBox").classList.add("layui-hide");
    bidPost = false;
    //创建JSON
    let data = {"act": "resetBidContent"};
    sendJson(data, callbackBidStart);
    layer.load(2);
}

//回调 接收重置投标内容结果
function callbackBidStart(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
}

//开始竞标
function prepareBid() {
    layer.open({
        title: '注意'
        , content: '一旦开始竞标则不可中止'
        , btn: ['确认开始', '返回']
        , yes: function (index, layero) {
            let act = "prepareBid";
            let data = {"act": act};
            sendJson(data, callbackPrepareBid);
            layer.close(index);
            layer.load(2);
        }
    });
}

//回调 竞标结束
function callbackPrepareBid(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    document.getElementById("bidResultMe").innerText = json.result ? "中标" : "竞标失败";
    document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
    document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
    refreshBidContent();
}

//显示投标信息
function bidInfoShow() {
    let amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    document.getElementById("bidContentBox").classList.add("layui-hide");
    document.getElementById("bidProgressBox").classList.remove("layui-hide");
    document.getElementById("bidInfoBox").classList.remove("layui-hide");
    let bidAmountMe = document.getElementById("bidAmountMe");
    bidAmountMe.innerText = amountInputValue;
    let bidNameMe = document.getElementById("bidNameMe");
    bidNameMe.innerText = accountName;
}

//关闭投标信息
function bidInfoClose() {
    document.getElementById("bidContentBox").classList.remove("layui-hide");
    document.getElementById("bidProgressBox").classList.add("layui-hide");
    document.getElementById("bidInfoBox").classList.add("layui-hide");
    // let amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    let amountInput = document.getElementById("bidderAmountInput");
    let amountButton = document.getElementById("bidderAmountButton");
    amountInput.innerHTML = '<input type="number" name="bidAccount" required lay-verify="required" placeholder="请输入投标金额" autocomplete="off" class="layui-input" id="bidderAmount">';
    amountButton.innerText = "确认金额";
    amountButton.setAttribute("onclick", "readBidderAmount()");
}

//发送投标信息
function postBidContent() {
    let amount = document.getElementById("bidderAmountTrue");
    if (amount  !=  undefined) {
        let amountInputValue = amount.innerText;
        let tenderName = document.getElementById("tenderName").innerText;
        let bidCode = document.getElementById("bidCode").innerText;
        let act = "setBidContent";
        let data = {
            "act": act,
            "name": accountName,
            "amount": amountInputValue,
            "tenderName": tenderName,
            "bidCode": bidCode
        };
        sendJson(data, callbackBidInfo);
        bidInfoShow();
        bidPost = true;
        layer.load(2);
    }
}

//刷新竞标信息
function refreshBidContent() {
    if (bidPost  ==  true) {
        let act = "refreshBidContent";
        let data = {"act": act};
        sendJson(data, callbackBidInfo);
        layer.load(2);
    }
}

//回调 接收竞标信息
function callbackBidInfo(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    document.getElementById("bidFormTablePrepareButtonID").classList.remove("layui-hide");
    document.getElementById("bidFormTableResetButtonID").classList.remove("layui-hide");

    let bidInfoTableBody = document.getElementById("bidInfoTableBody");
    let bidProgressTableCountsNow = document.getElementById("bidProgressTableCountsNow");
    let bidProgressTableResult = document.getElementById("bidProgressTableResult");
    let bidResultMe = document.getElementById("bidResultMe").innerText;
    let bidNameMe = document.getElementById("bidNameMe");
    let bidProgressTableRegistResult = document.getElementById("bidProgressTableRegistResult");
    bidInfoTableBody.innerHTML = "";
    bidProgressTableCountsNow.innerText = json.registrationInfo.length;
    if (json.registResult  !=  undefined) {//投标状态
        bidProgressTableRegistResult.innerText = json.registResult;
        if (json.registResult  ==  "投标人数已满") {
            document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
            document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
        }
    }
    if (json.finishAlready) {//已结束
        document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
        document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
    } else {
        if (json.registAlready  ==  false) {
            document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
            document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");

        }
    }
    bidProgressTableResult.innerText = "竞标结束";
    for (let i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
        let index = "No." + json.registrationInfo[i][0];
        let name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
        let address = json.registrationInfo[i][2];
        let resultTemp = json.registrationInfo[i][3];
        let result;
        if (resultTemp  ==  "win") {
            result = "中标";
            bidFinish = true;
            if (name  ==  bidNameMe.innerText) {
                bidResultMe = "中标";
            }
        } else if (resultTemp  ==  "lose") {
            result = "竞标失败";
            if (name  ==  bidNameMe.innerText) {
                bidResultMe = "竞标失败";
            }
        } else {
            result = "等待结果";
            bidResultMe = result;
            bidProgressTableResult.innerText = "进行中";
        }
        let row = bidInfoTableBody.insertRow(i);
        let data1 = row.insertCell(0);
        data1.appendChild(document.createTextNode(index));
        data1.classList.add("bidInfoTableData", "bidInfoTableData1");
        let data2 = row.insertCell(1);
        data2.appendChild(document.createTextNode(name));
        data2.classList.add("bidInfoTableData", "bidInfoTableData2", "layui-elip");
        let data3 = row.insertCell(2);
        data3.appendChild(document.createTextNode(address));
        data3.classList.add("bidInfoTableData", "bidInfoTableData3", "layui-elip");
        let data4 = row.insertCell(3);
        data4.appendChild(document.createTextNode(result));
        data4.classList.add("bidInfoTableData", "bidInfoTableData4");
    }
    if (json.registAlready) {
        document.getElementById("bidProgressTableRegistResult").innerText = "已投标";
    }
    document.getElementById("bidResultMe").innerText = bidResultMe;
    prepareBid();//delete
}

//请求注册表的内容
function loadRegList() {
    let tenderNameValue = document.getElementById("bidAuditSearchUnitInput").value;
    let bidCodeValue = document.getElementById("bidAuditSearchCodeInput").value;
    if (tenderNameValue  !=  "" && bidCodeValue  !=  "") {
        //创建JSON
        let act = "loadRegList";
        let data = {"act": act, "tenderName": tenderNameValue, "bidCode": bidCodeValue};
        sendJson(data, callbackLoadRegList);
        layer.load(2);
    }
}

//回调 加载注册表的内容
function callbackLoadRegList(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    if (json.status) {
        let bidListTableBody = document.getElementById("bidListTableBody");
        bidListTableBody.innerHTML = "";//清空表
        auditResultList.length = 0;
        for (let i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
            let index = "No." + json.registrationInfo[i][0];
            let name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
            let address = json.registrationInfo[i][2];
            let resultTemp = json.registrationInfo[i][3];
            let result;
            if (resultTemp  ==  "win") {
                result = "中标";
            } else {
                result = "竞标失败";
            }
            let row = bidListTableBody.insertRow(i);
            let data0 = row.insertCell(0);
            checkList[i] = "bidListCheck" + (i + 1);
            data0.innerHTML = '<input type="checkbox" class="bidListCheckbox" id="' + checkList[i] + '">';
            data0.classList.add("bidListTableData", "bidListTableData0");
            let data1 = row.insertCell(1);
            data1.appendChild(document.createTextNode(index));
            data1.classList.add("bidListTableData", "bidListTableData1");
            let data2 = row.insertCell(2);
            data2.appendChild(document.createTextNode(name));
            data2.classList.add("bidListTableData", "bidListTableData2", "layui-elip");
            let data3 = row.insertCell(3);
            data3.appendChild(document.createTextNode(address));
            data3.classList.add("bidListTableData", "bidListTableData3", "layui-elip");
            let data4 = row.insertCell(4);
            data4.appendChild(document.createTextNode(result));
            data4.classList.add("bidListTableData", "bidListTableData4");
            let data5 = row.insertCell(5);
            data5.appendChild(document.createTextNode(""));
            data5.classList.add("bidListTableData", "bidListTableData5");
            data5.setAttribute("id", "auditResult" + (i + 1));
        }
        document.getElementById("bidAuditButton").classList.remove("layui-hide");
        document.getElementById("bidAuditSearchTable").classList.remove("layui-hide");
    } else
        alert("招标不存在或未结束");
}

//全选或全不选
function checkAll() {
    let select = document.getElementById("bidListCheckAll").checked;
    for (let i = 0; i < checkList.length; i++) {
        document.getElementById(checkList[i]).checked = select;
    }
}

//请求进行审计
function startAudit() {
    let checkedList = [];
    checkedList.length = 0;
    for (let i = 0; i < checkList.length; i++) {
        if (document.getElementById(checkList[i]).checked)
            checkedList[checkedList.length] = i + 1;
    }
    if (checkedList.length > 0) {
        //创建JSON
        let act = "startAudit";
        let data = {"act": act, "checkedList": checkedList};
        sendJson(data, callbackStartAudit);
        //打开等待动画
        layer.load(2);
    }
}

//回调 审计结果
function callbackStartAudit(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    for (let i = 0; i < json.result.length; i++) {
        let indexTemp;
        for (x in json.result[i])
            indexTemp = x;
        let idTemp = "auditResult" + indexTemp;
        document.getElementById(idTemp).innerText = (json.result[i][indexTemp]) ? "审计通过" : "审计失败"
    }

}

//打开添加招标信息界面
function showNewBidBox() {
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById("bidBoxCoverID").classList.remove("layui-hide");
    document.getElementById("fixBidBoxID").classList.add("boxBlur");

    document.getElementById('bidModifyBox').classList.remove("layui-hide");
    document.getElementById('bidDetailBox').classList.add("layui-hide");
}

//关闭添加招标信息界面
function closeNewBidBox() {
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
}

//创建新的招标
function createNewBid() {
    let name = document.getElementById("mianName").innerText;
    let counts = document.getElementById("countsInput").value;
    let dateStart = document.getElementById("dateStartInput").value;
    let dateEnd = document.getElementById("dateEndInput").value;
    let bidName = document.getElementById("bidNameInput").value;
    if (name  !=  "" && counts  !=  "" && dateStart  !=  "" && dateEnd  !=  "" && bidName  !=  "") {
        let act = "createNewBid";
        let data = {
            "act": act,
            "name": name,
            "counts": counts,
            "dateStart": dateStart,
            "dateEnd": dateEnd,
            "bidName": bidName
        };
        sendJson(data, callbackCreateNewBid);
        layer.load(2);
    }
}

//回调 已创建新的招标
function callbackCreateNewBid(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    closeNewBidBox();
    listOldBids();
}

//请求已创建的招标
function listOldBids() {
    if (accountName  !=  undefined) {
        let act = "listOldBids";
        let data = {"act": act, "name": accountName};
        sendJson(data, callbackListOldBids);
        layer.load(2);
    }
}

//回调 加载已创建的招标
function callbackListOldBids(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    let bidListTableBody = document.getElementById("oldBidListTableBody");
    bidListTableBody.innerHTML = "";//清空表
    oldBidIndexList.length = 0;

    for (let i = json.BidTableList.length - 1; i >= 0; i--) {
        let bidCodeTemp = json.BidTableList[i].bidCode;
        let bidNameTemp = json.BidTableList[i].bidName;
        let bidStatusTemp = (JSON.parse(json.BidTableList[i].bidable)) ? "进行中" : "已结束";
        let tableIndex = json.BidTableList.length - i - 1;
        oldBidIndexList[tableIndex] = bidCodeTemp;
        let row = bidListTableBody.insertRow(tableIndex);
        row.classList.add("bidsCard");
        row.setAttribute("onclick", "showBidDetail(this)");
        let data0 = row.insertCell(0);
        data0.appendChild(document.createTextNode(bidStatusTemp));
        data0.classList.add("bidStatus");
        let data1 = row.insertCell(1);
        data1.appendChild(document.createTextNode(bidNameTemp));
        data1.classList.add("bidName");
    }
}

//查询招标详细信息
function showBidDetail(thisObject) {
    let bidCode = oldBidIndexList[thisObject.sectionRowIndex];
    let act = "showBidDetail";
    let data = {"act": act, "name": accountName, "bidCode": bidCode};
    sendJson(data, callbackShowBidDetail);
    openBidDetail();
    layer.load(2);
}

//回调 显示招标详细信息
function callbackShowBidDetail(json) {
    layer.closeAll('loading');
    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    document.getElementById("bidName").innerText = json.bidInfo.bidName;
    document.getElementById("counts").innerText = json.bidInfo.bidCounts;
    document.getElementById("bidCode").innerText = json.bidInfo.bidCode;
    document.getElementById("dateStart").innerText = json.bidInfo.dateStart.replace("_", " ");
    document.getElementById("dateEnd").innerText = json.bidInfo.dateEnd.replace("_", " ");
    document.getElementById("status").innerText = (json.isBidFinished) ? "已结束" : "进行中";
    document.getElementById("tenderAmount").innerText = json.amount;

    let bidInfoTableBody = document.getElementById("bidInfoTableBody");
    bidInfoTableBody.innerHTML = "";
    for (let i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
        let index = "No." + json.registrationInfo[i][0];
        let name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
        let address = json.registrationInfo[i][2];
        let resultTemp = json.registrationInfo[i][3];
        let result;
        if (resultTemp  ==  "win") {
            result = "中标";
            bidFinish = true;
        } else if (resultTemp  ==  "lose") {
            result = "竞标失败";
        } else {
            result = "等待结果";
        }
        let row = bidInfoTableBody.insertRow(i);
        let data1 = row.insertCell(0);
        data1.appendChild(document.createTextNode(index));
        data1.classList.add("bidInfoTableData", "bidInfoTableData1");
        let data2 = row.insertCell(1);
        data2.appendChild(document.createTextNode(name));
        data2.classList.add("bidInfoTableData", "bidInfoTableData2");
        let data3 = row.insertCell(2);
        data3.appendChild(document.createTextNode(address));
        data3.classList.add("bidInfoTableData", "bidInfoTableData3");
        let data4 = row.insertCell(3);
        data4.appendChild(document.createTextNode(result));
        data4.classList.add("bidInfoTableData", "bidInfoTableData4");
    }
    listOldBids();
}

//打开招标信息框
function openBidDetail() {
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById('bidBoxCoverID').classList.remove("layui-hide");
    document.getElementById('fixBidBoxID').classList.add("boxBlur");

    document.getElementById('bidModifyBox').classList.add("layui-hide");
    document.getElementById('bidDetailBox').classList.remove("layui-hide");
}

//关闭招标信息框
function closeBidDetail() {
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
    document.getElementById("bidInfoTableBody").innerHTML = "";
}

//打开上传框
function startUpload() {
    let uploadFileBox = document.getElementById("uploadFileBox");
    uploadFileBox.style.display = "block";
    registReturn();
    closeDelete();
}

//关闭上传框
function UploadReturn() {
    let uploadFileBox = document.getElementById("uploadFileBox");
    uploadFileBox.style.display = "none";
}

//上传密钥文件
function postSk() {
    let fileInput = document.getElementById("skFileInput");
    let skName = document.getElementById("skName");
    if (skName.value  !=  "" && fileInput.value  !=  "") {
        let file = fileInput.files[0];
        // 读取文件:
        let reader = new FileReader();
        reader.onload = function (e) {
            let result = e.target.result;
            let fileContent = result.substring(result.indexOf("base64,") + 7, result.length);
            let act = "postSk";
            let data = {"act": act, "name": skName.value + ":" + roleFlag, "sk": fileContent};
            sendJson(data, callbackPostSk);
            layer.load(2);
        };
        // 以DataURL的形式读取文件:
        reader.readAsDataURL(file);
    } else
        alert("请输入用户名并选择密钥文件");
}

//回调 上传密钥文件成功
function callbackPostSk(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    let loginAccountContainer = document.getElementById("loginAccountContainerID");
    loginAccountContainer.innerHTML = "";
    tenderAccountList.length = 0;
    bidderAccountList.length = 0;
    for (let i = 0; i < json.accountList.length; i++) {
        let fullName = json.accountList[i];
        let name = fullName.substring(0, fullName.lastIndexOf(":"));
        let role = fullName.substring(fullName.lastIndexOf(":") + 1);
        if (role  ==  '0') {//招标者
            tenderAccountList[tenderAccountList.length] = name;
        } else {//投标者
            bidderAccountList[bidderAccountList.length] = name;
        }
    }
    switchToRole(roleFlag)
    accountLoadFinish = true;
    //stopWaitAnima();
    UploadReturn();

}

//弹出确认删除所有账户框
function startDelete() {
    let deleteBox = document.getElementById("deleteBox");
    deleteBox.style.display = "block";
    registReturn();
    UploadReturn();
}

//关闭确认删除所有账户框
function closeDelete() {
    let deleteBox = document.getElementById("deleteBox");
    deleteBox.style.display = "none";
}

//删除所有账户
function deleteAllAccount() {
    let act = "deleteAllAccount";
    let data = {"act": act};
    sendJson(data, callbackDeleteAllAccount);
    layer.load(2);
}

//回调 已经删除所有账户
function callbackDeleteAllAccount(json) {
    layer.closeAll('loading');

    console.log(json);//delete
    if (json.act  ==  false)
        alert("出错了，请刷新页面重试");
    let loginAccountContainer = document.getElementById("loginAccountContainerID");
    loginAccountContainer.innerHTML = "";
    tenderAccountList.length = 0;
    bidderAccountList.length = 0;
    for (let i = 0; i < json.accountList.length; i++) {
        let fullName = json.accountList[i];
        let name = fullName.substring(0, fullName.lastIndexOf(":"));
        let role = fullName.substring(fullName.lastIndexOf(":") + 1);
        if (role  ==  '0') {//招标者
            tenderAccountList[tenderAccountList.length] = name;
        } else {//投标者
            bidderAccountList[bidderAccountList.length] = name;
        }
    }
    switchToRole(roleFlag)
    accountLoadFinish = true;
    //stopWaitAnima();
    closeDelete()

}