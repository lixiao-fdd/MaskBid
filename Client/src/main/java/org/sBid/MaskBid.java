package org.sBid;

import org.apache.commons.codec.digest.DigestUtils;

public class MaskBid {
    public static void main(String[] arg) {
        System.out.println("Start");
        int port = 10899;
        if (arg.length > 0)
            port = Integer.parseInt(arg[0]);
        MaskBidServer server = new MaskBidServer(port);
        server.listen();
        System.exit(0);
    }

}
