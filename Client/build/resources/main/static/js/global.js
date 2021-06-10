let table = layui.table;
let tabShowNow;
let auditTenderName;
let auditStart;
//创建JSON
function sendJson(jsonData, functionName) {
    $.ajax({
        type: "POST",
        url: "/data",
        contentType: 'application/json',
        async: true,
        data: JSON.stringify(jsonData),
        dataType: 'json',
        success: function (dat) {
            functionName(dat);
        },
        error: function (e) {
            console.log("ERROR: ", e);
        }
    });
}
//回调测试
function callback(json) {
    console.log(json);
}
//退出登录
function logout() {
    let jsonData = { "act": "logout" };
    sendJson(jsonData, callbackLogut);
    layer.load(2);
}
function callbackLogut(json) {
    layer.closeAll('loading');
    window.location.replace("./");

}
//获取账户信息并渲染发布标的页面，以及发布标的
function tenderStarter() {
    let newBidDataStartValue;
    tabShowNow = "tenderBidAdmin";
    //获取账户信息
    let jsonData = { "act": "listAccountInfo" };
    sendJson(jsonData, callbackListAccountInfo);
    //表单控件
    //发布新标的
    layui.use('form', function () {
        var form = layui.form;
        form.on('submit(postBid)', function (data) {
            postNewBid(data.field, newBidDataStartValue);
            return false; //阻止表单跳转
        });
        form.verify({
            rangeCounts: function (value, item) { //value：表单的值、item：表单的DOM对象
                if (value < 2) {
                    return '竞标人数不能小于2';
                }
                if (value > 2147483646) {
                    return '竞标人数不能大于2147483646';
                }
            }
            , rangeDate: function (value, item) { //value：表单的值、item：表单的DOM对象
                if (value < 1) {
                    return '持续时间不能小于1';
                }
                if (value > 3600) {
                    return '持续时间不能大于3600';
                }
            }
            , maxLengthName: function (value, item) {
                var s = new String(value);
                if (s.length > 64) {
                    return '标的名称最大为64字符，当前为' + s.length + '字符';
                }
            }
            , maxLengthContent: function (value, item) {
                var s = new String(value);
                if (s.length > 10240) {
                    return '标的内容最大为10240字符，当前为' + s.length + '字符';
                }
            }
        });
    });
    //标的检索控件
    layui.use('form', function () {
        var form = layui.form;
        form.on('submit(auditSearchBid)', function (data) {
            auditSearchBid(data.field);
            return false; //阻止表单跳转
        });
        form.verify({
            maxLengthName: function (value, item) {
                var s = new String(value);
                if (s.length > 64) {
                    return '招标方名称最大为64字符，当前为' + s.length + '字符';
                }
            }
        });
    });
    // 时间输入控件
    layui.use('laydate', function () {
        var laydate = layui.laydate;
        laydate.render({
            elem: '#newBidDateStart' //指定元素
            , type: 'datetime'
            , format: 'yyyy年MM月dd日 HH时mm分' //可任意组合
            , min: 0
            , max: 30 //最多选择30天后发布
            , done: function (value, date, endDate) {
                newBidDataStartValue = date;
            }
        });
    });
}
//回调 获取账户信息
function callbackListAccountInfo(json) {
    console.log(json);
    document.getElementById("mainTitleAccountName").innerHTML = json.data.accountName + document.getElementById("mainTitleAccountName").innerHTML;
    document.getElementById("accountInfoName").innerText = json.data.accountName;
    document.getElementById("accountInfoRole").innerText = json.data.accountRole;
    document.getElementById("accountInfoAddress").innerText = json.data.accountAddress;
    document.getElementById("accountInfoPk").innerText = json.data.accountPk;
}
//渲染等待中的标的表格
function loadTableBidWaiting() {
    table.render({
        elem: '#tableBidWaiting'
        , url: '/table' //数据接口
        , headers: { "tableType": 'BidWaiting' }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidName', title: '标的名称', sort: true, unresize: true, minWidth: 200 }
            , { field: 'bidCode', title: '标的编号', width: 400, unresize: true }
            , { field: 'bidCounts', title: '投标人数', width: 110, sort: true, align: "center", unresize: true }
            , { field: 'detail', title: '', width: 100, fixed: 'right', align: "center", unresize: true, templet: '<div><a href="javascript:;" onclick="showBidDetail(this)">详细信息</a></div>' }
        ]]
        , size: 'lg '
        , text: {
            none: '暂无等待中的标的'
        }
    });
}
//渲染进行中的标的表格
function loadTableBidOngoing() {
    table.render({
        elem: '#tableBidOngoing'
        , url: '/table' //数据接口
        , headers: { "tableType": 'BidOngoing' }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidName', title: '标的名称', sort: true, unresize: true, minWidth: 200 }
            , { field: 'bidCode', title: '标的编号', width: 400, unresize: true }
            , { field: 'bidCounts', title: '投标人数', width: 110, sort: true, align: "center", unresize: true }
            , { field: 'detail', title: '', width: 100, fixed: 'right', align: "center", unresize: true, templet: '<div><a href="javascript:;" onclick="showBidDetail(this)">详细信息</a></div>' }
        ]]
        , size: 'lg '
        , text: {
            none: '暂无进行中的标的'
        }
    });
}
//渲染已完成的标的表格
function loadTableBidFinished() {
    table.render({
        elem: '#tableBidFinished'
        , url: '/table' //数据接口
        , headers: { "tableType": 'BidFinished' }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidName', title: '标的名称', sort: true, unresize: true, minWidth: 200 }
            , { field: 'bidCode', title: '标的编号', width: 400, unresize: true }
            , { field: 'bidCounts', title: '投标人数', width: 110, sort: true, align: "center", unresize: true }
            , { field: 'detail', title: '', width: 100, fixed: 'right', align: "center", unresize: true, templet: '<div><a href="javascript:;" onclick="showBidDetail(this)">详细信息</a></div>' }
        ]]
        , size: 'lg '
        , text: {
            none: '暂无已完成的标的'
        }
    });
}
//渲染标的投标信息表格
function loadTableBidRegInfo() {
    table.render({
        elem: '#tableBidRegInfo'
        , url: '/table' //数据接口
        , headers: { "tableType": 'BidRegInfo' }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidderIndex', title: '编号', sort: true, unresize: true, width: 90, align: "center" }
            , { field: 'bidderName', title: '投标机构名称', width: 500, unresize: true }
            , { field: 'bidderPk', title: '投标机构公钥', unresize: true }
            , { field: 'bidderResults', title: '投标结果', sort: true, width: 100, align: "center", unresize: true }
        ]]
        , size: 'lg '
        , text: {
            none: '暂无投标'
        }
        , height: 490
    });
}
//渲染指定招标方已完成的标的列表
function loadTableAuditBidList(tenderName) {
    table.render({
        elem: '#tableAuditBidList'
        , url: '/search' //数据接口
        , headers: { "tenderName": encodeURI(tenderName) }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidName', title: '标的名称', sort: true, unresize: true, minWidth: 200 }
            , { field: 'bidCode', title: '标的编号', width: 400, unresize: true }
            , { field: 'bidCounts', title: '投标人数', width: 110, sort: true, align: "center", unresize: true }
            , { field: 'detail', title: '', width: 100, fixed: 'right', align: "center", unresize: true, templet: '<div><a href="javascript:;" onclick="prepareAudit(this)">开始审计</a></div>' }
        ]]
        , size: 'lg '
        , text: {
            none: '该招标方暂无标的'
        }
    });
}
//渲染审计标的投标者表格
function loadTableBidAuditRegInfo() {
    document.getElementById("auditLogBox").classList.remove("layui-hide");
    table.render({
        elem: '#tableBidAuditRegInfo'
        , url: '/table' //数据接口
        , headers: { "tableType": 'BidAuditRegInfo' }
        , page: false //开启分页
        , cols: [[ //表头
            { field: 'bidderIndex', title: '编号', sort: true, unresize: true, width: 90, align: "center" }
            , { field: 'bidderName', title: '投标机构名称', width: 500, unresize: true }
            , { field: 'bidderPk', title: '投标机构公钥', unresize: true }
            , { field: 'bidderResults', title: '投标结果', sort: true, width: 110, align: "center", unresize: true }
            , { field: 'auditResults', title: '审计结果', width: 100, align: "center", unresize: true }
        ]]
        , size: 'lg '
        , text: {
            none: '请选择审计对象'
        }
        , height: 300
    });
}
//查看标的的详细信息
function showBidDetail(thisObj) {
    let thisBidCode = thisObj.parentElement.parentElement.parentElement.childNodes[1].innerText
    let data = { "tenderName": document.getElementById("mainTitleAccountName").innerText, "bidCode": thisBidCode };
    let jsonData = { "act": "showBidDetail", "data": data };
    sendJson(jsonData, callbackShowBidDetail);
}
//回调 查看标的的详细信息
function callbackShowBidDetail(json) {
    console.log(json);
    loadTableBidRegInfo();
    document.getElementById("bidName").innerText = json.data.bidName;
    document.getElementById("bidCounts").innerText = json.data.bidCounts;
    document.getElementById("bidCode").innerText = json.data.bidCode;
    document.getElementById("bidDateStart").innerText = json.data.bidDateStart;
    document.getElementById("bidDateEnd").innerText = json.data.bidDateEnd;
    document.getElementById("bidStatus").innerText = json.data.bidStatus;
    document.getElementById("bidAmount").innerText = json.data.bidAmount;
    if (json.data.bidContent != "")
        document.getElementById("bidContent").innerText = json.data.bidContent;
    else
        document.getElementById("bidContent").innerText = "无";

    layer.open({
        type: 1
        , title: false
        , content: $('#bidDetail')
        , move: '.layerMove'
        , moveOut: true
        , area: ['1000px', '800px']
    });
}
//切换页面
function switchPage(thisObj) {
    document.getElementById(tabShowNow).classList.remove("layui-show");
    tabShowNow = thisObj.attributes.getNamedItem("tabId").value;
    document.getElementById(tabShowNow).classList.add("layui-show");
}
//发布标的
function postNewBid(data, date) {
    data.newBidDateStart = date.year + "年" + date.month + "月" + date.date + "日 " + date.hours + "时" + date.minutes + "分";
    let jsonFormData = { "act": "postNewBid", "data": data };
    sendJson(jsonFormData, callbackPostNewBid);
}
//回调 发布标的
function callbackPostNewBid(json) {
    console.log(json.data.bidDateStart + " - " + json.data.bidDateEnd);
}
//标的检索
function auditSearchBid(data) {
    auditTenderName = data.auditTenderName;
    //将检索关键字传递给表格渲染服务
    loadTableAuditBidList(auditTenderName);
}
//准备审计
function prepareAudit(thisObj) {
    //发送要审计的招标方名称以及标的编号
    let thisBidCode = thisObj.parentElement.parentElement.parentElement.childNodes[1].innerText
    let data = { "tenderName": auditTenderName, "bidCode": thisBidCode };
    let jsonData = { "act": "prepareAudit", "data": data };
    sendJson(jsonData, callbackPrepareAudit);
    //跳转到审计结果页
    document.getElementById("auditTabSearch").classList.remove("layui-this");
    document.getElementById("auditTabItemSearch").classList.remove("layui-show");
    document.getElementById("auditTabResult").classList.add("layui-this");
    document.getElementById("auditTabItemResult").classList.add("layui-show");
}
//回调 开始审计，接收标的信息
function callbackPrepareAudit(json) {
    console.log(json);
    //渲染标的参与者表格
    loadTableBidAuditRegInfo();
    document.getElementById("auditInfoBidName").innerText = json.data.auditInfoBidName;
    document.getElementById("auditInfoBidContent").innerText = json.data.auditInfoBidContent;
    document.getElementById("auditInfoBidCode").innerText = json.data.auditInfoBidCode;
    document.getElementById("auditInfoBidCounts").innerText = json.data.auditInfoBidCounts;
    document.getElementById("auditInfoBidResult").innerText = json.data.auditInfoBidResult;
    //开审计并打印日志
    document.getElementById("auditLog").value = "------------ audit start ------------ \n";
    auditStart = true;
    startAudit();
}
//加载审计结果
function startAudit() {
    let jsonData = { "act": "startAudit", "auditStart": auditStart };
    sendJson(jsonData, callbackStartAudit);
}
//回调 加载审计结果
function callbackStartAudit(json) {
    console.log(json)
    auditStart = false;
    let auditLog = document.getElementById("auditLog");
    auditLog.value = auditLog.value + json.data.log;
    if (json.data.finishStatus == false)
        startAudit()
    else if (json.data.finishStatus == true) {
        console.log(json.data.auditResult);//应为数组
        document.getElementById("auditLog").value = document.getElementById("auditLog").value + "\n------------ audit finish ------------ \n";
    }
    auditLog.scrollTop = auditLog.scrollHeight;
}
//打开用户信息页
function openAccountInfoLayer() {
    layer.open({
        type: 1
        , title: false
        , content: $('#accountInfo')
        , move: '.layerMove'
        , moveOut: true
        , area: '1000px'
    });
}