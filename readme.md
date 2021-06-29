# MaskBid 操作手册

## 部署智能合约

> 依赖：`git`，`fisco-bcos v2.7.2`和`fisco-bcos console`

克隆MaskBid项目的git仓库，执行：

```bash
git clone https://github.com/qiuqingyun/MaskBid.git
```

智能合约文件`MaskBid.sol`位于`MaskBid/Contract`文件夹中，将其复制到区块链控制台的智能合约文件夹。本合约依赖接口合约文件`Table.sol`。

部署合约时，在区块链控制台中执行：

```bash
[group:1]> deploy MaskBid ${main_table_addr}
transaction hash: 0xbb43d6ef4e9b66310c34bba1de542ea265d474a2fd5893c0767a923bbf139b77
contract address: 0x2bb5a1058eb68051ca8849cad2d9cde3ce55f0d3
currentAccount: 0xe7e46d5e4ae6696251f76c58aa662c51d67e7585
```

其中`${main_table_addr}`为主表地址。部署完成后控制台会返回包括合约地址`contract address`在内的多个值。在首次进入系统时，需要填入主表地址`${main_table_addr}`与合约地址`contract address`，因此这两个值应妥善保管。

至此智能合约部署完成。

## 部署服务

> 依赖：`Docker Engine`，安装步骤可见[官方教程](https://docs.docker.com/engine/install/)

### 拉取 Docker 镜像

使用 Docker 命令行执行:

```bash
docker pull qing98/maskbid:latest
```

### 部署一个服务

```bash
docker container run --rm \
    --name MaskBid \
    -p ${bind_ip}:8080 \
    -v ${dir_conf}:/app/conf \
    -d qing98/maskbid:latest
```

#### 参数

* `bind_ip` - 用于访问服务的端口
* `dir_conf` - 存放SDK证书以及配置文件`sdkConfig.toml`的文件夹的路径

> 注意：配置文件需命名为`sdkConfig.toml`，否则无法读取。

### 批量部署服务

> 依赖：`Docker Compose`，安装步骤可见[官方教程](https://docs.docker.com/compose/install/)

以批量部署一个招标方，两个投标者的服务为例。

创建`docker-compose.yml`文件，写入以下内容：

```yaml
version: "3.9"
services:
    tender:
        image: qing98/maskbid:latest
        ports:
            - "8080:8080"
            # "访问端口:8080"
        volumes:
            - "/home/qqy/MaskBid/conf:/app/conf"
            # "conf文件夹:/app/conf"
        container_name: tender
        # 为容器命名，方便辨认
    bidder1:
        image: qing98/maskbid:latest
        ports:
            - "8081:8080"
        volumes:
            - "/home/qqy/MaskBid/conf:/app/conf"
        container_name: bidder1
    bidder2:
        image: qing98/maskbid:latest
        ports:
            - "8082:8080"
        volumes:
            - "/home/qqy/MaskBid/conf:/app/conf"
        container_name: bidder2
```

请确保访问端口未被占用，然后执行`docker-compose up -d`即可批量部署服务，执行`docker-compose down`可将已部署的服务关闭。

部署完成后可以在`${ip_address}:8080`访问招标方服务，在`${ip_address}:8081`访问投标者1服务，在`${ip_address}:8082`访问投标者2服务。`${ip_address}`为宿主机的ip地址。

## 进入系统

### 初次运行

部署服务后初次访问会自动弹出区块链设置框，请在其中填入正确的合约地址和主表地址。

![初次运行](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/登录页初次运行.png)

> 合约地址形如`0x2bb5a1058eb68051ca8849cad2d9cde3ce55f0d3`，主表地址形如`MaskBid`。

填写完毕后点击修改，保存设置。

### 登陆页面

访问服务时首先看到的是登陆界面。登陆界面左半部分为登录账户功能区，右半部分为注册账户功能区。

![登陆页面](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/登录页.png)

#### 注册账户

还未拥有账户时，需要先注册新的账户。根据您的身份，选择`招标方`或`投标方`，并输入账户名称。点击注册按钮，若账户名称合法即可完成注册，并自动登入系统。

登入系统后，会自动下载一个存有登录信息的加密文件`*.mbk`，后续登录需要用到这个文件。请妥善保管该文件，一旦丢失便无法找回，账户也无法再次登录。

> 注意：招标方账户名称不允许重复；投标方账户名称可以重复，但账户公钥不会重复。

#### 登录账户

点击登录按钮，在弹出来的文件选择框中，选择注册时下载的`*.mbk`文件，并确认上传。上传成功后便可登入系统。登陆时无需选择身份。

#### 区块链设置

点击右下角的`齿轮图标`，在弹出来的设置框中，可以更改合约地址和主表地址。

### 招标方主页

招标方主页分为三个大的功能区，分别为`标的管理`，`标的审计`和`区块链概览`。招标方主要操作区域为`标的管理`区。在这里招标方可以发布新的标的，查看正在进行中的标的以及查看已经结束的标的。

![招标方标的管理发布标的](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理发布标的.png)

#### 标的管理区

当招标方准备发布新的标的时，只需在`标的管理`区的`发布标的`界面填写新标的的信息，包括`标的名称`，`发起时间`，`持续时间`和`标的描述`，然后点击`发布标的`即可完成新标的的发布。

![招标方标的管理发布标的填写内容](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理发布标的填写内容.png)

> `标的描述`为可选内容，其余项为必填内容；`发起时间`为投标者可以开始发起投标的时间，可选范围限制在操作当日至未来三十天内；`持续时间`是指在`发起时间`后投标者可以进行投标的时间段的长度，即处于可报价阶段的时间长度。可选时间单位为`分钟`，`小时`和`天`。当处于可报价阶段结束后，投标方之间会自动进行加密竞标。

标的成功发布后会自动跳转到`进行中的`页面，该页面展示了已经发布，且处于可报价阶段的标的。

列表内容为`标的名称`，`标的编号`和`竞标开始时间`，以及`详细信息`按钮。

![招标方标的管理进行中的](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理进行中的.png)

点击`详细信息`按钮可以看到该标的的详细信息。点击后弹出来的展示框中展示了该标的的详细信息，以及已投标的投标方信息。此时暂无投标方进行投标。

![招标方标的管理进行中的标的信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理进行中的标的信息.png)

若已有投标方进行投标，则会展示投标方的`编号`，`投标机构名称`，`投标机构公钥`以及`投标结果`。因为报价状态仍然在进行中，还未进行竞标，所以`投标结果`均为`等待竞标`。

![招标方标的管理进行中的标的信息有人投标](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理进行中的标的信息有人投标.png)

当可报价阶段结束后，投标方之间会自动进行加密竞标。此时我们可以到`已结束的`界面查看已经处于结束阶段的标的。该界面展示了已结束标的的`标的名称`，`标的编号`与`投标人数`，以及`详细信息`和`审计`按钮。

![招标方标的管理已结束的标的](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理已结束的标的.png)

点击`详细信息`按钮可以看到该已结束标的的详细信息。点击后弹出来的展示框中展示了该标的的详细信息，以及投标方的信息；投标方列表会展示投标方的`编号`，`投标机构名称`，`投标机构公钥`以及`投标结果`。若该投标方中标，则`投标结果`为`中标`，反之为`失败`。该界面还会展示中标方的`中标金额`，但不会泄露未中标方的金额。

![招标方标的管理已结束的标的详细信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的管理已结束的标的详细信息.png)

点击`审计`按钮可以直接跳转到`标的审计`区的`审计结果`页面。该页面展示了此次投标的简略信息以及审计结果。

![招标方标的审计检索结果](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的审计检索结果.png)

#### 标的审计区

标的审计区可以对某个特定的标的进行审计操作。当招标方准备进行审计时，便可以在`标的检索`界面的`招标方名称`输入框内输入某个招标方的名称，然后点击`检索`按钮。

![招标方标的审计检索页](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的审计检索页.png)

> 招标方名称可以是任何真实存在的招标方的名称，包括本账号的。如果要对本账号的标的进行审计，更方便的方法是到`标的管理`区的`已结束的`页面选择标的进行审计。

点击`检索`按钮后，会列出该招标方已结束的标的。列表展示了已结束标的的`标的名称`，`标的编号`与`投标人数`，以及`审计`按钮。

![招标方标的审计检索页内容](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的审计检索页内容.png)

选定某个标的后，点击`审计`按钮会跳转到`审计结果`页面，该页面展示了此次投标的简略信息以及审计结果。审计结果列表会展示每个投标方的审计结果，以及整个标的的审计结果。`审计过程`框会简要输出审计的流程。

![招标方标的审计检索结果](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方标的审计检索结果.png)

#### 区块链概览区

区块链概览区可以直观的展示系统当前所处的区块链的详细信息。

概览栏显示了当前区块链的`区块高度`,`总交易数`和`总节点数`。

概览栏下方的动态图表每五秒刷新一次，以更加直观的方式展示了总交易数的动态变化。图表中的蓝色折线为总交易数的变化，而绿色竖条为某个五秒时间段内新增的交易数。图表采用了动态坐标轴，因此不会出现总交易数变得很大之后看不到曲线的变化的情况。

![招标方区块链概览](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方区块链概览.png)

再下方是最新生成的区块和交易信息。

区块信息展示了区块编号，区块生成时间以及区块中包含的交易数量。

交易信息展示了交易hash，交易生成时间，以及交易发起方和接收方的地址。

新生成的区块和交易信息会出现在列表最上方，旧的区块和交易信息在一定时间后会取消显示，最后只会保留最新的一些区块和交易信息。这样既保证了区块和交易信息有足够的时间展示，又保证了页面不会随时间变得越来越长。

![招标方区块链概览详细](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方区块链概览详细.png)

#### 账户信息

界面右上角会显示当前账户的名称。当鼠标悬浮在账户名称上时会显示一个菜单，内容为`账户信息`,`系统帮助`和`退出登录`。点击`退出登录`后可以更换账户。

点击`账户信息`后会弹出账户信息页面，该页面展示了`账户名称`,`账户身份`,`账户地址`和`账户公钥`四项信息。

![招标方用户信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/招标方用户信息.png)

### 投标方主页

投标方主页相较于招标方主页有所简化，分为两个大的功能区，即为`投标`和`审计`。投标方主要操作区域为`投标`区。在这里投标方可以检索指定招标方的标的，查看等待竞标的标的和已经结束竞标的标的。

![投标方标的检索](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的检索.png)

#### 投标区

当投标方准备投标时，便可以在`标的检索`界面的`招标方名称`输入框内输入某个招标方的名称，然后点击`检索`按钮。

点击`检索`按钮后，会列出该招标方所有处于报价状态的标的。列表展示了`标的名称`，`标的编号`与`竞标开始时间`，以及`投标`按钮。

![投标方标的检索加载标的](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的检索加载标的.png)

选择一个标的，点击`投标`按钮后，会弹出`投标中心`界面。该界面展示了这个标的的`招标机构`，`标的名称`，`标的编号`，`开始时间`，`结束时间`和`标的内容`，以及`投标金额`输入框。

当确认要进行投标时，在`投标金额`输入框中输入投标金额，然后点击`提交投标`按钮，便可发布投标。

![投标方标的检索加载标的详细信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的检索加载标的详细信息.png)

> 注意：
> 1. 投标一旦发布便不可撤销。
> 2. 当竞标开始时间到来之时，系统会自动与其他投标者进行加密竞标，该过程根据投标者人数的不同会耗费几秒到数十秒不等的时间。
> 3. 在竞标尚未结束前请勿退出账号的登陆状态，否则会出现难以预料的后果。

成功发布投标后，会自动跳转到`等待中的`页面。该页面展示了已经投标但还未进行竞标的标的。列表内容为`招标机构`，`标的名称`，`标的编号`，`竞标开始时间`和`投标金额`，以及`详细信息`按钮。

![投标方等待中的投标](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方等待中的投标.png)

点击`详细信息`按钮后弹出来的展示框中展示了该标的的详细信息。

![投标方等待中的投标详细信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方等待中的投标详细信息.png)

在`已结束的`界面展示了已经竞标结束的标的。列表内容为`招标机构`，`标的名称`，`标的编号`，`投标金额`和`竞标结果`，以及`详细信息`和`审计`按钮。在`竞标结果`一栏会展示该标的的竞标结果。

![投标方已结束的投标](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方已结束的投标.png)

此时点击`详细信息`按钮，在弹出来的展示框中展示了该标的的详细信息，并且包含此标的最后的中标金额。

![投标方已结束的投标详细信息](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方已结束的投标详细信息.png)

点击`审计`按钮可以直接跳转到`审计`区的`审计结果`页面。该页面展示了此次投标的简略信息以及审计结果。

![投标方标的审计检索结果](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的审计检索结果.png)

#### 审计区

审计区可以对某个特定的标的进行审计操作。基本操作与招标方审计一致。

![投标方标的审计检索页内容](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的审计检索页内容.png)

选定某个标的后，点击`审计`按钮会跳转到`审计结果`页面，该页面展示了此次投标的简略信息以及审计结果。`审计过程`框会简要输出审计的流程。与招标方审计结果页不同的地方在于，投标方审计结果页不显示具体投标方的信息。

![投标方标的审计检索结果](https://cdn.jsdelivr.net/gh/qiuqingyun/MaskBid@master/Img/投标方标的审计检索结果.png)


## 生成 Docker 镜像

若修改了源代码需要重新生成 Docker 镜像，只需将重新编译好的`MaskBidClient-1.0.jar`文件替换`MaskBid/MaskBidApp`文件夹中原有的，然后在`MaskBid/MaskBidApp`文件夹下运行：

```bash
docker image build -t maskbid .
```

运行结束后便会生成新的`maskbid`镜像。

## 前端源文件位置

前端页面的源文件位于`MaskBid/Client/src/main/resources/static`文件夹中。

## git仓库主要模块简介

* `Client`模块为java编写的系统客户端后台程序，负责前端界面的后台任务，并与区块链进行交互。前端源文件也位于该模块之中。

* `Contract`模块为智能合约。

* `Core`模块为C++编写的密码学核心，负责进行加密竞标。依赖`gmp`和`ntl`模块。

* `MaskBidApp`模块负责 Docker 镜像的生成。