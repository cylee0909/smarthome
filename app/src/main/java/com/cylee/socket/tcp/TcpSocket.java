package com.cylee.socket.tcp;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cylee on 16/9/24.
 */
public class TcpSocket {
    private static final int DEFAULT_SOCKET_TIMEOUT = 0; // none timeout
    private int mSocketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private TcpSocketReader mReader;
    private TcpSocketWriter mWriter;
    private LinkedBlockingQueue<String> mWriteDatas = new LinkedBlockingQueue<>();
    private ITcpConnectListener mListener;
    protected volatile boolean mStoped;
    protected Socket mSocket;

    public TcpSocket() {
    }

    public void setSoTimeout(int timeout) {
        mSocketTimeout = timeout;
    }

    public int getSoTimeOut() {
        return mSocketTimeout;
    }

    public TcpSocket connect(String address, int port, ITcpConnectListener listener) {
        mStoped = false;
        mListener = listener;
        if (mSocket != null && !mSocket.isClosed()) {
            throw new IllegalAccessError("Had a alive Binded");
        }
        try {
            mSocket = new Socket(address, port);
            mSocket.setSoTimeout(mSocketTimeout);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkSocket();

        if (mReader == null) {
            try {
                mReader = new TcpSocketReader(this);
                new Thread(mReader, "TcpSocket-Reader").start();
            } catch (Exception e) {
                e.printStackTrace();
                if (mListener != null) {
                    mListener.onConnectFail(ITcpConnectListener.ERROR_READ_ERROR);
                }
                return null;
            }
        }

        if (mWriter == null) {
            try {
                mWriter = new TcpSocketWriter(this, mWriteDatas);
                new Thread(mWriter, "TcpSocket-Writer").start();
            } catch (Exception e) {
                e.printStackTrace();
                if (mListener != null) {
                    mListener.onConnectFail(ITcpConnectListener.ERROR_WRITE_ERROR);
                }
                return null;
            }
        }

        onConnect();
        return this;
    }

    protected void send(String data) {
        mWriteDatas.add(data);
    }

    private void checkSocket() {
        if (mSocket == null) {
            throw new RuntimeException("Socket must not be null");
        }

        if (mSocket.isClosed()) {
            throw new RuntimeException("Socket closed, please check it");
        }
    }

    public boolean isConnected() {
        return mSocket != null && !mStoped && !mSocket.isClosed();
    }

    protected void onReceive(String dp) {
        if (mListener != null) {
            mListener.onReceive(this, dp);
        }
    }

    protected void onConnect() {
        if (mListener != null) {
            mListener.onConnect(this);
        }
    }

    public void stop() {
        if (mWriter != null) {
            mWriter.stop();
        }
        if (mReader != null) {
            mReader.stop();
        }
        mStoped = true;
    }
}
