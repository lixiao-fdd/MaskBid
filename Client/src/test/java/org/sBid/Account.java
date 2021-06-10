package org.sBid;

import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.CryptoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Account {
    private SBid contract;
    private Client client;
    private final String name;

    private String address;
    private String pk;
    private final String contractAddress;
    private BcosSDK bcosSDK = null;
    private String rootPath = File.separator;
    private final String certPath = "conf" + File.separator;
    private String filesPath = "";
    CryptoKeyPair cryptoKeyPair;

    public Account(String name, String contractAddress) {
        //启动软件时执行，检测文件夹是否存在，不存在就创建
        this.name = name;
        this.contractAddress = contractAddress;
        //root path
        File directory = new File("");
        try {
            rootPath = directory.getCanonicalPath() + rootPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将名称用base64编码并作为文件夹名

        filesPath = rootPath + "Files_" + name + File.separator;
        //创建文件夹
        Path filesDir = Paths.get(filesPath);
        try {
            Files.createDirectory(filesDir);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            System.err.println("Failed to create " + filesDir);
            e.printStackTrace();
        }
    }

    public boolean connect() {
        //加载配置文件并连接节点
        String configFilePath = rootPath + certPath + "sdkConfig.toml";
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            bcosSDK = BcosSDK.build(configFilePath);
            client = bcosSDK.getClient(1);
            return true;
        }
        return false;
    }

    public void createAccount() {
        //key file path
        String pemAccountFilePath = filesPath + name + ".pem";
        // 创建非国密类型的CryptoSuite
        CryptoSuite cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
        // 随机生成非国密公私钥对
        cryptoKeyPair = cryptoSuite.createKeyPair();
        // save keys
        cryptoKeyPair.storeKeyPairWithPem(pemAccountFilePath);
        System.out.println("Account is created");
    }

    public boolean loadAccount() {
        //key file path
        String pemAccountFilePath = filesPath + name + ".pem";
        //prepare account
        File file = new File(pemAccountFilePath);
        if (!file.exists()) {//账户不存在
            return false;
        } else {//load account
            client.getCryptoSuite().loadAccount("pem", pemAccountFilePath, null);
            cryptoKeyPair = client.getCryptoSuite().getCryptoKeyPair();
            System.out.println("Account " + name + " is loaded");
        }
        this.pk = cryptoKeyPair.getHexPublicKey();
        this.address = cryptoKeyPair.getAddress();
        this.contract = SBid.load(contractAddress, client, cryptoKeyPair);//加载合约
        return true;
    }

    public boolean loadAccount(String sk) {
        //key file path
        String pemAccountFilePath = filesPath + name + ".pem";
        Global.writeFile(pemAccountFilePath, sk);
        return true;
    }

    public SBid getContract() {
        return contract;
    }

    public String getAddress() {
        return address;
    }


    public String getName() {
        return name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public BcosSDK getBcosSDK() {
        return bcosSDK;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getFilesPath() {
        return filesPath;
    }

    public String getBase64NameStr() {
        return name;
    }

    public String getPk() {
        return pk;
    }

    public CryptoKeyPair getCryptoKeyPair() {
        return cryptoKeyPair;
    }
}
