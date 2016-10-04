package com.cylee.socket.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by cylee on 16/10/3.
 */

public class TcpSocketReader implements Runnable{
    private BufferedReader mBR;
    private volatile boolean mStoped;
    private TcpSocket mSocket;

    TcpSocketReader(TcpSocket socket) throws Exception {
        mSocket = socket;
        mBR = new BufferedReader(
                new InputStreamReader(socket.mSocket.getInputStream()));
        mStoped = false;
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
                String current = mBR.readLine();
                mSocket.onReceive(current);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    void stop() {
        mStoped = true;
        if (mBR != null) {
            try {
                mBR.close();
            } catch (Exception e) {
            }
        }
    }
}
