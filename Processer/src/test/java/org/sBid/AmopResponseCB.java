package org.sBid;

import org.fisco.bcos.sdk.amop.AmopResponse;
import org.fisco.bcos.sdk.amop.AmopResponseCallback;

public class AmopResponseCB extends org.fisco.bcos.sdk.amop.AmopResponseCallback {
    private final String index;
    private final String name;
    private final String fileName;

    public AmopResponseCB(String index, String name, String fileName) {
        this.index = index;
        this.name = name;
        this.fileName = fileName;
    }

    @Override

    public void onResponse(AmopResponse response) {
        if (response.getErrorCode() == 102) {
            System.err.println(
                    "Timeout, maybe your file is too large or your gave a short timeout.");
        } else {
            if (response.getAmopMsgIn() != null) {
                String responseStr = new String(response.getAmopMsgIn().getContent());
                String indexRes = responseStr.split(":")[0];
                String nameRes = responseStr.split(":")[1];
                String fileNameRes = responseStr.split(":")[2];
//                System.out.println("Get response from " + indexRes + "_" + nameRes + "_" + fileNameRes);
                if (index.compareTo(indexRes) == 0 && name.compareTo(nameRes) == 0 && fileName.compareTo(fileNameRes) == 0) {
                    Global.signalAMOPSendOK = true;
                }
            } else {
                // 收到其它错误
                System.err.println(
                        "Get response, { errorCode:"
                                + response.getErrorCode()
                                + " error:"
                                + response.getErrorMessage()
                                + " seq:"
                                + response.getMessageID());
                Global.signalAMOPSendERR = true;
            }
        }
        Global.signalAMOPSendRes = true;
    }
}