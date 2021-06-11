var test;
var bidFinish = false;
var bidPost = false;
var accountLoadFinish = false;
var loginFinish = false;
var loginFaild = false;
var registAlready = false;
var bidderAccountList = new Array();
var tenderAccountList = new Array();
var childList = new Array();
var pageNow = 1;
var roleFlag = 1;
var accountName;
var pageId = new Array("bidBox", "auditBox", "settingBox", "aboutBox");
var checkList = new Array();
var auditResultList = new Array();
var oldBidIndexList = new Array();

function sendJson(data, functionName) {
    //创建JSON
    var httpRequest = new XMLHttpRequest();
    httpRequest.onreadystatechange = functionName;//回调函数
    httpRequest.open("POST", "", true);
    httpRequest.setRequestHeader('content-type', 'application/json');
    httpRequest.send(JSON.stringify(data));
}

function showText(thisObj) {
    alert(thisObj.innerText);
}
function reset() {
    var tenderNameReset = document.getElementById("tenderNameReset").value;
    var act = "GodModReset";
    var data = { "act": act, "tenderNameReset": tenderNameReset };
    sendJson(data, callbackReset);
    layer.load(2);
}
function callbackReset() {
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        else
            alert("重置完成");
    }
}
function listAccount() {//请求已保存的账户列表
    //创建JSON
    var act = "listAccount";
    var data = { "act": act };
    sendJson(data, callbackListAccount);
    layer.load(2);
}
function callbackListAccount() {//接收已保存的账号列表
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        console.log(json);//delete
        var loginAccountContainer = document.getElementById("loginAccountContainerID");
        loginAccountContainer.innerHTML = "";
        tenderAccountList.length = 0;
        bidderAccountList.length = 0;
        for (var i = 0; i < json.accountList.length; i++) {
            var fullName = json.accountList[i];
            var name = fullName.substring(0, fullName.lastIndexOf(":"));
            var role = fullName.substring(fullName.lastIndexOf(":") + 1);
            if (role == '0') {//招标者
                tenderAccountList[tenderAccountList.length] = name;
            } else {//投标者
                bidderAccountList[bidderAccountList.length] = name;
            }
        }
        switchToRole(roleFlag)
        accountLoadFinish = true;
        //stopWaitAnima();
    }
}
function switchToRole(role) {//切换角色
    var accountList;
    var calssCountent;
    var loginAccountContainer = document.getElementById("loginAccountContainerID");
    loginAccountContainer.innerHTML = "";
    var loginBoxTitleBoxBidder = document.getElementById("loginBoxTitleBoxBidderID");
    var loginBoxTitleBoxTender = document.getElementById("loginBoxTitleBoxTenderID");
    if (role == '1') {
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
    for (var i = 0; i < accountList.length; i++) {
        var account = accountShow(accountList[i], calssCountent);
        loginAccountContainer.appendChild(account);
    }
}
function accountShow(name, calssCountent) {//列出已保存的账户
    var account = document.createElement('div');
    var cover = document.createElement('div');
    var accountName = document.createElement('p');
    accountName.innerText = name;
    cover.classList.add("cover");
    account.classList.add("loginAccount", calssCountent);
    account.append(cover);
    account.append(accountName);
    account.setAttribute("onclick", "login(this)")
    return account;
}
function startRegist() {//打开注册框
    var registerBox = document.getElementById("registerBox");
    registerBox.style.display = "block";
    UploadReturn();
    closeDelete();
}
function registReturn() {//关闭注册框
    var registerBox = document.getElementById("registerBox");
    registerBox.style.display = "none";
}
function postRegist() {//注册
    var newName = document.getElementById("newName").value;
    //创建JSON
    var act = "createAccount";
    var data = { "act": act, "name": newName + ":" + roleFlag };
    sendJson(data, callbackListAccount);
    registReturn();
    layer.load(2);
}
function login(thisObject) {//登录账号
    var name = thisObject.innerText;
    //创建JSON
    var act = "login";
    var data = { "act": act, "name": name + ":" + roleFlag };
    sendJson(data, callbackLogin);
    layer.load(2);
}
function callbackLogin() {//登录结果
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete

        if (json.act == false)
            alert("出错了，请刷新页面重试");
        if (json.result) {//进入主页
            loginFinish = true;
            if (roleFlag == '0') {
                window.location.replace("./tender.html");
            } else {
                window.location.replace("./bidder.html");
            }
        } else {//登陆失败
            loginFaild = true;
            //stopWaitAnima();
            alert("登录失败");
        }
    }
}

function listAccountInfo() {//请求账户信息
    var act = "listAccountInfo";
    var data = { "act": act };
    sendJson(data, callbackListAccountInfo);
    layer.load(2);
}
function callbackListAccountInfo() {//接收账户信息
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        var accountInfoName = document.getElementById("accountInfoName");
        var accountInfoAddrress = document.getElementById("accountInfoAddrress");
        var accountInfoPK = document.getElementById("accountInfoPK");
        var mianName = document.getElementById("mianName");
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
}

function switchPage(thisObj) {//切换页面
    var switchName = thisObj.innerText;
    var bidBox = document.getElementById("bidBoxID");
    var auditBox = document.getElementById("auditBoxID");
    var settingBox = document.getElementById("settingBoxID");
    var bidSide = document.getElementById("bidSide");
    var auditSide = document.getElementById("auditSide");
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

function openBidModify() {//打开修改投标对象界面
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById("bidBoxCoverID").classList.remove("layui-hide");
    document.getElementById("fixBidBoxID").classList.add("boxBlur");
}
function closeBidModify() {//关闭修改投标对象界面
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
}
function setBid() {//设置投标对象
    var tenderNameValue = document.getElementById("bidModifySearchUnitInput").value;
    var bidCodeValue = document.getElementById("bidModifySearchCodeInput").value;
    if (tenderNameValue != "" && bidCodeValue != "") {
        var act = "setBid";
        var data = { "act": act, "tenderName": tenderNameValue, "bidCode": bidCodeValue };
        sendJson(data, callbackSetBid);
        layer.load(2);
    }
}
function callbackSetBid() {//接收投标对象的信息，发布日期， 截止日期，报价状态，竞标人数，招标名称以及是否已报价和报价金额
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");

        if (json.result) {
            var tenderName = document.getElementById("tenderName");
            tenderName.innerText = json.tenderName;
            var bidCode = document.getElementById("bidCode");
            bidCode.innerText = json.bidCode;
            var dateStartValue = json.dateStart;
            var dateEndValue = json.dateEnd;
            var countsValue = json.counts;
            var bidNameValue = json.bidName;
            var statusValue = json.status;
            var registAlready = json.registAlready;

            var dateStart = document.getElementById("dateStart");
            dateStartValue = dateStartValue.replace("_", " ");
            dateStart.innerText = dateStartValue;
            var dateEnd = document.getElementById("dateEnd");
            dateEndValue = dateEndValue.replace("_", " ");
            dateEnd.innerText = dateEndValue;
            var status = document.getElementById("status");
            status.innerText = statusValue;
            var counts = document.getElementById("counts");
            counts.innerText = countsValue;
            var bidName = document.getElementById("bidName");
            bidName.innerText = bidNameValue;
            if (statusValue == "进行中") {
                document.getElementById("bidContentBox").classList.remove("layui-hide");
                //若已报价，则加载投标信息
                if (registAlready) {
                    bidPost = true;
                    document.getElementById("bidContentBox").classList.add("layui-hide");
                    document.getElementById("bidProgressBox").classList.remove("layui-hide");
                    document.getElementById("bidInfoBox").classList.remove("layui-hide");
                    document.getElementById("bidNameMe").innerText = accountName;
                    refreshBidContent();
                    var bidAmountMe = document.getElementById("bidAmountMe");
                    bidAmountMe.innerText = json.amount;
                } else {
                    bidInfoClose();
                }
            } else if (statusValue == "已结束") {
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
                    var bidAmountMe = document.getElementById("bidAmountMe");
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
}
function readBidderAmount() {//读取投标内容中投标金额的值，并固定
    var amountInputValue = document.getElementById("bidderAmount").value;
    if (amountInputValue != "" && amountInputValue > 0 && amountInputValue < 4294967295) {
        var amountInput = document.getElementById("bidderAmountInput");
        var amountButton = document.getElementById("bidderAmountButton");
        amountInput.innerHTML = "<p class=\"bidFormTableValue\" id=\"bidderAmountTrue\">" + amountInputValue + "</p>";
        amountButton.innerText = "重置金额";
        amountButton.setAttribute("onclick", "resetBidderAmount()");

    }
}
function resetBidderAmount() {//重置投标内容中投标金额的值
    var amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    var amountInput = document.getElementById("bidderAmountInput");
    var amountButton = document.getElementById("bidderAmountButton");
    amountInput.innerHTML = '<input type="number" name="bidAccount" required lay-verify="required" placeholder="请输入投标金额" autocomplete="off" class="layui-input" id="bidderAmount" value="' + amountInputValue + '">';
    amountButton.innerText = "确认金额";
    amountButton.setAttribute("onclick", "readBidderAmount()");
    // amountButton.innerHTML = '<button class="bidFormTableButton" onclick="readBidderAmount()">确认</button>';
}
function resetBidderCotent() {//重置投标内容
    document.getElementById("bidContentBox").classList.remove("layui-hide");
    document.getElementById("bidProgressBox").classList.add("layui-hide");
    document.getElementById("bidInfoBox").classList.add("layui-hide");
    bidPost = false;
    //创建JSON
    var data = { "act": "resetBidContent" };
    sendJson(data, callbackBidStart);
    layer.load(2);
}
function callbackBidStart() {//接收重置投标内容结果
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
    }
}
function prepareBid() {//开始竞标
    layer.open({
        title: '注意'
        , content: '一旦开始竞标则不可中止'
        , btn: ['确认开始', '返回']
        , yes: function (index, layero) {
            var act = "prepareBid";
            var data = { "act": act };
            sendJson(data, callbackPrepareBid);
            layer.close(index);
            layer.load(2);
        }
    });
}
function callbackPrepareBid() {//竞标结束
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        var bidResult = json.result ? "中标" : "竞标失败";
        document.getElementById("bidResultMe").innerText = bidResult;
        document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
        document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
        refreshBidContent();
    }
}

function bidInfoShow() {//显示投标信息
    var amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    document.getElementById("bidContentBox").classList.add("layui-hide");
    document.getElementById("bidProgressBox").classList.remove("layui-hide");
    document.getElementById("bidInfoBox").classList.remove("layui-hide");
    var bidAmountMe = document.getElementById("bidAmountMe");
    bidAmountMe.innerText = amountInputValue;
    var bidNameMe = document.getElementById("bidNameMe");
    bidNameMe.innerText = accountName;
}
function bidInfoClose() {//关闭投标信息
    document.getElementById("bidContentBox").classList.remove("layui-hide");
    document.getElementById("bidProgressBox").classList.add("layui-hide");
    document.getElementById("bidInfoBox").classList.add("layui-hide");
    // var amountInputValue = document.getElementById("bidderAmountTrue").innerText;
    var amountInput = document.getElementById("bidderAmountInput");
    var amountButton = document.getElementById("bidderAmountButton");
    amountInput.innerHTML = '<input type="number" name="bidAccount" required lay-verify="required" placeholder="请输入投标金额" autocomplete="off" class="layui-input" id="bidderAmount">';
    amountButton.innerText = "确认金额";
    amountButton.setAttribute("onclick", "readBidderAmount()");
}
function postBidContent() {//发送投标信息
    // var nameInputValue = document.getElementById("bidderName").innerText;
    var amount = document.getElementById("bidderAmountTrue");
    if (amount != undefined) {
        //创建JSON
        var amountInputValue = amount.innerText;
        var tenderName = document.getElementById("tenderName").innerText;
        var bidCode = document.getElementById("bidCode").innerText;
        var act = "setBidContent";
        var data = { "act": act, "name": accountName, "amount": amountInputValue, "tenderName": tenderName, "bidCode": bidCode };
        sendJson(data, callbackBidInfo);
        bidInfoShow();
        bidPost = true;
        layer.load(2);
    }
}
function refreshBidContent() {//刷新竞标信息
    if (bidPost == true) {
        //创建JSON
        var act = "refreshBidContent";
        var data = { "act": act };
        sendJson(data, callbackBidInfo);
        layer.load(2);
    }
}
function callbackBidInfo() {//接收竞标信息
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        document.getElementById("bidFormTablePrepareButtonID").classList.remove("layui-hide");
        document.getElementById("bidFormTableResetButtonID").classList.remove("layui-hide");

        var bidInfoTableBody = document.getElementById("bidInfoTableBody");
        var bidProgressTableCountsNow = document.getElementById("bidProgressTableCountsNow");
        var bidProgressTableResult = document.getElementById("bidProgressTableResult");
        var bidResultMe = document.getElementById("bidResultMe").innerText;
        var bidNameMe = document.getElementById("bidNameMe");
        var bidProgressTableRegistResult = document.getElementById("bidProgressTableRegistResult");
        bidInfoTableBody.innerHTML = "";
        bidProgressTableCountsNow.innerText = json.registrationInfo.length;
        if (json.registResult != undefined) {//投标状态
            bidProgressTableRegistResult.innerText = json.registResult;
            if (json.registResult == "投标人数已满") {
                document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
                document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
            }
        }
        if (json.finishAlready) {//已结束
            document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
            document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
        }else{
            if(json.registAlready==false){
                document.getElementById("bidFormTablePrepareButtonID").classList.add("layui-hide");
                document.getElementById("bidFormTableResetButtonID").classList.add("layui-hide");
            
            }
        }
        bidProgressTableResult.innerText = "竞标结束";
        for (var i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
            var index = "No." + json.registrationInfo[i][0];
            var name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
            var address = json.registrationInfo[i][2];
            var resultTemp = json.registrationInfo[i][3];
            var result;
            if (resultTemp == "win") {
                result = "中标";
                bidFinish = true;
                if (name == bidNameMe.innerText) {
                    bidResultMe = "中标";
                }
            } else if (resultTemp == "lose") {
                result = "竞标失败";
                if (name == bidNameMe.innerText) {
                    bidResultMe = "竞标失败";
                }
            } else {
                result = "等待结果";
                bidResultMe = result;
                bidProgressTableResult.innerText = "进行中";
            }
            var row = bidInfoTableBody.insertRow(i);
            var data1 = row.insertCell(0);
            data1.appendChild(document.createTextNode(index));
            data1.classList.add("bidInfoTableData", "bidInfoTableData1");
            var data2 = row.insertCell(1);
            data2.appendChild(document.createTextNode(name));
            data2.classList.add("bidInfoTableData", "bidInfoTableData2", "layui-elip");
            var data3 = row.insertCell(2);
            data3.appendChild(document.createTextNode(address));
            data3.classList.add("bidInfoTableData", "bidInfoTableData3", "layui-elip");
            var data4 = row.insertCell(3);
            data4.appendChild(document.createTextNode(result));
            data4.classList.add("bidInfoTableData", "bidInfoTableData4");
        }
        if (json.registAlready) {
            document.getElementById("bidProgressTableRegistResult").innerText = "已投标";
        }
        document.getElementById("bidResultMe").innerText = bidResultMe;
    }
}
function loadRegList() {//请求注册表的内容
    var tenderNameValue = document.getElementById("bidAuditSearchUnitInput").value;
    var bidCodeValue = document.getElementById("bidAuditSearchCodeInput").value;
    if (tenderNameValue != "" && bidCodeValue != "") {
        //创建JSON
        var act = "loadRegList";
        var data = { "act": act, "tenderName": tenderNameValue, "bidCode": bidCodeValue };
        sendJson(data, callbackLoadRegList);
        layer.load(2);
    }
}
function callbackLoadRegList() {//加载注册表的内容
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        if (json.status) {
            var bidListTableBody = document.getElementById("bidListTableBody");
            bidListTableBody.innerHTML = "";//清空表
            auditResultList.length = 0;
            for (var i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
                var index = "No." + json.registrationInfo[i][0];
                var name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
                var address = json.registrationInfo[i][2];
                var resultTemp = json.registrationInfo[i][3];
                var result;
                if (resultTemp == "win") {
                    result = "中标";
                } else {
                    result = "竞标失败";
                }
                var row = bidListTableBody.insertRow(i);
                var data0 = row.insertCell(0);
                checkList[i] = "bidListCheck" + (i + 1);
                data0.innerHTML = '<input type="checkbox" class="bidListCheckbox" id="' + checkList[i] + '">';
                data0.classList.add("bidListTableData", "bidListTableData0");
                var data1 = row.insertCell(1);
                data1.appendChild(document.createTextNode(index));
                data1.classList.add("bidListTableData", "bidListTableData1");
                var data2 = row.insertCell(2);
                data2.appendChild(document.createTextNode(name));
                data2.classList.add("bidListTableData", "bidListTableData2", "layui-elip");
                var data3 = row.insertCell(3);
                data3.appendChild(document.createTextNode(address));
                data3.classList.add("bidListTableData", "bidListTableData3", "layui-elip");
                var data4 = row.insertCell(4);
                data4.appendChild(document.createTextNode(result));
                data4.classList.add("bidListTableData", "bidListTableData4");
                var data5 = row.insertCell(5);
                data5.appendChild(document.createTextNode(""));
                data5.classList.add("bidListTableData", "bidListTableData5");
                data5.setAttribute("id", "auditResult" + (i + 1));
            }
            document.getElementById("bidAuditButton").classList.remove("layui-hide");
            document.getElementById("bidAuditSearchTable").classList.remove("layui-hide");
        } else
            alert("招标不存在或未结束");
    }
}
function checkAll() {//全选或全不选
    var select = document.getElementById("bidListCheckAll").checked;
    for (var i = 0; i < checkList.length; i++) {
        document.getElementById(checkList[i]).checked = select;
    }
}
function startAudit() {//请求进行审计
    var checkedList = new Array();
    checkedList.length = 0;
    for (var i = 0; i < checkList.length; i++) {
        if (document.getElementById(checkList[i]).checked)
            checkedList[checkedList.length] = i + 1;
    }
    if (checkedList.length > 0) {
        //创建JSON
        var act = "startAudit";
        var data = { "act": act, "checkedList": checkedList };
        sendJson(data, callbackStartAudit);
        //打开等待动画
        layer.load(2);
    }
}
function callbackStartAudit() {//审计结果
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        for (var i = 0; i < json.result.length; i++) {
            var indexTemp;
            for (x in json.result[i])
                indexTemp = x;
            var idTemp = "auditResult" + indexTemp;
            document.getElementById(idTemp).innerText = (json.result[i][indexTemp]) ? "审计通过" : "审计失败"
        }
    }
}
function showNewBidBox() {//打开添加招标信息界面
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById("bidBoxCoverID").classList.remove("layui-hide");
    document.getElementById("fixBidBoxID").classList.add("boxBlur");

    document.getElementById('bidModifyBox').classList.remove("layui-hide");
    document.getElementById('bidDetailBox').classList.add("layui-hide");
}
function closeNewBidBox() {//关闭添加招标信息界面
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
}
function createNewBid() {//创建新的招标
    var name = document.getElementById("mianName").innerText;
    var counts = document.getElementById("countsInput").value;
    var dateStart = document.getElementById("dateStartInput").value;
    var dateEnd = document.getElementById("dateEndInput").value;
    var bidName = document.getElementById("bidNameInput").value;
    if (name != "" && counts != "" && dateStart != "" && dateEnd != "" && bidName != "") {
        var act = "createNewBid";
        var data = { "act": act, "name": name, "counts": counts, "dateStart": dateStart, "dateEnd": dateEnd, "bidName": bidName };
        sendJson(data, callbackCreateNewBid);
        layer.load(2);
    }
}
function callbackCreateNewBid() {//已创建新的招标
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        closeNewBidBox();
        listOldBids();
    }
}
function listOldBids() {//请求已创建的招标
    if (accountName != undefined) {
        var act = "listOldBids";
        var data = { "act": act, "name": accountName };
        sendJson(data, callbackListOldBids);
        layer.load(2);
    }
}
function callbackListOldBids() {//加载已创建的招标
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        var bidListTableBody = document.getElementById("oldBidListTableBody");
        bidListTableBody.innerHTML = "";//清空表
        oldBidIndexList.length = 0;

        for (var i = json.BidTableList.length - 1; i >= 0; i--) {
            var bidCodeTemp = json.BidTableList[i].bidCode;
            var bidNameTemp = json.BidTableList[i].bidName;
            var bidStatusTemp = (JSON.parse(json.BidTableList[i].bidable)) ? "进行中" : "已结束";
            var tableIndex = json.BidTableList.length - i - 1;
            oldBidIndexList[tableIndex] = bidCodeTemp;
            var row = bidListTableBody.insertRow(tableIndex);
            row.classList.add("bidsCard");
            row.setAttribute("onclick", "showBidDetail(this)");
            var data0 = row.insertCell(0);
            data0.appendChild(document.createTextNode(bidStatusTemp));
            data0.classList.add("bidStatus");
            var data1 = row.insertCell(1);
            data1.appendChild(document.createTextNode(bidNameTemp));
            data1.classList.add("bidName");
        }
    }
}
function showBidDetail(thisObject) {//查询招标详细信息
    var bidCode = oldBidIndexList[thisObject.sectionRowIndex];
    var act = "showBidDetail";
    var data = { "act": act, "name": accountName, "bidCode": bidCode };
    sendJson(data, callbackShowBidDetail);
    openBidDetail();
    layer.load(2);
}
function callbackShowBidDetail() {//显示招标详细信息
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        document.getElementById("bidName").innerText = json.bidInfo.bidName;
        document.getElementById("counts").innerText = json.bidInfo.bidCounts;
        document.getElementById("bidCode").innerText = json.bidInfo.bidCode;
        document.getElementById("dateStart").innerText = json.bidInfo.dateStart.replace("_", " ");
        document.getElementById("dateEnd").innerText = json.bidInfo.dateEnd.replace("_", " ");
        document.getElementById("status").innerText = (json.isBidFinished) ? "已结束" : "进行中";
        document.getElementById("tenderAmount").innerText = json.amount;

        var bidInfoTableBody = document.getElementById("bidInfoTableBody");
        bidInfoTableBody.innerHTML = "";
        for (var i = 0; i < json.registrationInfo.length; i++) {//加载竞标信息
            var index = "No." + json.registrationInfo[i][0];
            var name = json.registrationInfo[i][1].substring(0, json.registrationInfo[i][1].lastIndexOf(":"));
            var address = json.registrationInfo[i][2];
            var resultTemp = json.registrationInfo[i][3];
            var result;
            if (resultTemp == "win") {
                result = "中标";
                bidFinish = true;
            } else if (resultTemp == "lose") {
                result = "竞标失败";
            } else {
                result = "等待结果";
            }
            var row = bidInfoTableBody.insertRow(i);
            var data1 = row.insertCell(0);
            data1.appendChild(document.createTextNode(index));
            data1.classList.add("bidInfoTableData", "bidInfoTableData1");
            var data2 = row.insertCell(1);
            data2.appendChild(document.createTextNode(name));
            data2.classList.add("bidInfoTableData", "bidInfoTableData2");
            var data3 = row.insertCell(2);
            data3.appendChild(document.createTextNode(address));
            data3.classList.add("bidInfoTableData", "bidInfoTableData3");
            var data4 = row.insertCell(3);
            data4.appendChild(document.createTextNode(result));
            data4.classList.add("bidInfoTableData", "bidInfoTableData4");
        }
        listOldBids();
    }
}
function openBidDetail() {//打开招标信息框
    document.getElementById("floatBidBoxID").classList.remove("layui-hide");
    document.getElementById('bidBoxCoverID').classList.remove("layui-hide");
    document.getElementById('fixBidBoxID').classList.add("boxBlur");

    document.getElementById('bidModifyBox').classList.add("layui-hide");
    document.getElementById('bidDetailBox').classList.remove("layui-hide");
}
function closeBidDetail() {//关闭招标信息框
    document.getElementById("floatBidBoxID").classList.add("layui-hide");
    document.getElementById("bidBoxCoverID").classList.add("layui-hide");
    document.getElementById("fixBidBoxID").classList.remove("boxBlur");
    document.getElementById("bidInfoTableBody").innerHTML = "";
}
function startUpload() {//打开上传框
    var uploadFileBox = document.getElementById("uploadFileBox");
    uploadFileBox.style.display = "block";
    registReturn();
    closeDelete();
}
function UploadReturn() {//关闭上传框
    var uploadFileBox = document.getElementById("uploadFileBox");
    uploadFileBox.style.display = "none";
}
function postSk() {//上传密钥文件
    var fileInput = document.getElementById("skFileInput");
    var skName = document.getElementById("skName");
    if (skName.value != "" && fileInput.value != "") {
        var file = fileInput.files[0];
        // 读取文件:
        var reader = new FileReader();
        reader.onload = function (e) {
            var data = e.target.result;
            var fileContent = data.substring(data.indexOf("base64,") + 7, data.length);
            var act = "postSk";
            var data = { "act": act, "name": skName.value + ":" + roleFlag, "sk": fileContent };
            sendJson(data, callbackPostSk);
            layer.load(2);
        };
        // 以DataURL的形式读取文件:
        reader.readAsDataURL(file);
    } else
        alert("请输入用户名并选择密钥文件");
}
function callbackPostSk() {//加载用户成功
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        var loginAccountContainer = document.getElementById("loginAccountContainerID");
        loginAccountContainer.innerHTML = "";
        tenderAccountList.length = 0;
        bidderAccountList.length = 0;
        for (var i = 0; i < json.accountList.length; i++) {
            var fullName = json.accountList[i];
            var name = fullName.substring(0, fullName.lastIndexOf(":"));
            var role = fullName.substring(fullName.lastIndexOf(":") + 1);
            if (role == '0') {//招标者
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
}
function startDelete() {//弹出确认删除所有账户框
    var deleteBox = document.getElementById("deleteBox");
    deleteBox.style.display = "block";
    registReturn();
    UploadReturn();
}
function closeDelete() {//关闭确认删除所有账户框
    var deleteBox = document.getElementById("deleteBox");
    deleteBox.style.display = "none";
}
function deleteAllAccount() {//删除所有账户
    var act = "deleteAllAccount";
    var data = { "act": act };
    sendJson(data, callbackDeleteAllAccount);
    layer.load(2);
}
function callbackDeleteAllAccount() {//已经删除所有账户
    layer.closeAll('loading');
    if (this.readyState == 4 && this.status == 200) {//验证请求是否发送成功
        var jsonStr = this.responseText;//获取到服务端返回的数据
        var json = JSON.parse(jsonStr);
        console.log(json);//delete
        if (json.act == false)
            alert("出错了，请刷新页面重试");
        var loginAccountContainer = document.getElementById("loginAccountContainerID");
        loginAccountContainer.innerHTML = "";
        tenderAccountList.length = 0;
        bidderAccountList.length = 0;
        for (var i = 0; i < json.accountList.length; i++) {
            var fullName = json.accountList[i];
            var name = fullName.substring(0, fullName.lastIndexOf(":"));
            var role = fullName.substring(fullName.lastIndexOf(":") + 1);
            if (role == '0') {//招标者
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
}