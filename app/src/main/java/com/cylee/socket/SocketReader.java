package com.cylee.socket;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Created by cylee on 16/9/24.
 */
public class SocketReader implements Runnable {
    private static final int DEF_CACHE_LENGTH = 1024; // 1k
    private UdpSocket mSocket;
    private volatile boolean mStoped;
    private int mCacheLength = DEF_CACHE_LENGTH;

    SocketReader(UdpSocket socket) {
        mSocket = socket;
        mStoped = false;
    }

    @Override
    public void run() {
        while (!mStoped) {
            byte[] buf = new byte[mCacheLength];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mSocket.receive(packet);
            } catch (IOException e){
                e.printStackTrace();
            }
            if (packet != null) {
                mSocket.onReceive(packet);
            }
        }
    }

    void stop() {
        mStoped = true;
    }
}
