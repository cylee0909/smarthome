package com.cylee.socket.tcp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.LinkedBlockingQueue;

import cn.csnbgsh.herbarium.EX;

/**
 * Created by cylee on 16/10/3.
 */

public class TcpSocketWriter implements Runnable{
    private volatile boolean mStoped;
    private LinkedBlockingQueue<String> mDatas;
    private BufferedWriter mBW;
    private TcpSocket mSocket;
    private long mLastTime;

    TcpSocketWriter(TcpSocket socket, LinkedBlockingQueue<String> packets) throws Exception {
        mDatas = packets;
        mBW = new BufferedWriter(
                new OutputStreamWriter(socket.mSocket.getOutputStream()));
        mStoped = false;
        mSocket = socket;
    }

    public void stop() {
        mStoped = true;
        if (mBW != null) {
            try {
                mBW.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
//                try {
//                    mSocket.mSocket.sendUrgentData(0xFF);
//                } catch (Exception e) {
//                    SocketManager.INSTANCE.reconnect();
//                    continue;
//                }
                long timeUsed = System.currentTimeMillis() - mLastTime;
                if (timeUsed < 1000) {
                    try {
                        Thread.sleep(1000 - timeUsed);
                    } catch (Exception e){
                    }
                }
                String packet = mDatas.take();
                if (packet != null) {
                    mBW.write(packet);
                    mBW.flush();
                }
                mLastTime = System.currentTimeMillis();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
