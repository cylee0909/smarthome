package com.cylee.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cylee on 16/9/24.
 */
public class SocketWriter implements Runnable {
    private UdpSocket mSocket;
    private volatile boolean mStoped;
    private LinkedBlockingQueue<DatagramPacket> mDatagramPackets;

    SocketWriter(UdpSocket socket, LinkedBlockingQueue<DatagramPacket> packets) {
        mSocket = socket;
        mDatagramPackets = packets;
        mStoped = false;
    }

    public void stop() {
        mStoped = true;
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
                DatagramPacket packet = mDatagramPackets.take();
                if (packet != null) {
                    try {
                        mSocket.innerSend(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
