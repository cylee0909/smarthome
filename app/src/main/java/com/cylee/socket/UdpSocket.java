package com.cylee.socket;

import com.babt.smarthome.SocketManager;
import com.cylee.socket.tcp.IConnectListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cylee on 16/9/24.
 */
public class UdpSocket {
    private static final int DEFAULT_SOCKET_TIMEOUT = 0; // none timeout
    private int mSocketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private DatagramSocket mDatagramSocket;
    private SocketReader mReader;
    private SocketWriter[] mWriters;
    private LinkedBlockingQueue<DatagramPacket> mWriteDatas = new LinkedBlockingQueue<>();
    private IConnectListener mListener;
    private SocketAddress mClientAddress;
    protected volatile boolean mStoped;

    public UdpSocket() {
    }

    public void setSoTimeout(int timeout) {
        mSocketTimeout = timeout;
    }

    public int getSoTimeOut() {
        return mSocketTimeout;
    }

    public UdpSocket connect(SocketAddress addr, IConnectListener listener) {
        mStoped = false;
        mListener = listener;
        if(mDatagramSocket!=null && !mDatagramSocket.isClosed()){
            throw new IllegalAccessError("Had a alive Binded");
        }
        try {
            mDatagramSocket = new DatagramSocket(addr);
            mDatagramSocket.setSoTimeout(mSocketTimeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        checkSocket();

        onConnect();

        if (mReader == null) {
            mReader = new SocketReader(this);
            new Thread(mReader, "Socket-Reader").start();
        }

        if (mWriters == null) {
            mWriters = new SocketWriter[2];
            for (int i = 0; i < mWriters.length; i++) {
                mWriters[i] = new SocketWriter(this, mWriteDatas);
                new Thread(mWriters[i], "Socket-Writer"+i).start();
            }
        }
        return this;
    }

    public void send(DatagramPacket datagramPacket) {
        if (datagramPacket.getAddress() == null) {
            if (mClientAddress != null) {
                datagramPacket.setSocketAddress(mClientAddress);
            } else {
                throw new RuntimeException("address must not be null");
            }
        }
        mWriteDatas.add(datagramPacket);
    }

    void innerSend(DatagramPacket datagramPacket) throws IOException {
        checkSocket();
        mDatagramSocket.send(datagramPacket);
    }

    void receive(DatagramPacket packet) throws IOException {
        checkSocket();
        mDatagramSocket.receive(packet);
    }

    public void disConnect() {
        stop();
        mDatagramSocket.close();
    }

    private void checkSocket() {
        if (mDatagramSocket == null) {
            throw new RuntimeException("Socket must not be null");
        }

        if (mDatagramSocket.isClosed()) {
            throw new RuntimeException("Socket closed, please check it");
        }
    }

    protected void onReceive(DatagramPacket dp) {
        try {
            mClientAddress = dp.getSocketAddress();
        } catch (Exception e){
            e.printStackTrace();
        }
        if (mListener != null) {
            mListener.onReceive(this, dp.getData());
        }
    }

    protected void onConnect() {
        if (mListener != null) {
            mListener.onConnect(this);
        }
    }

    public void stop() {
        if (mWriters != null) {
            for (SocketWriter w : mWriters) {
                w.stop();
            }
        }
        if (mReader != null) {
            mReader.stop();
        }
        mStoped = true;
    }
}
