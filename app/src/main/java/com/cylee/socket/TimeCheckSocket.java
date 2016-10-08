package com.cylee.socket;

import com.cylee.socket.tcp.BaseTimeSocketListener;
import com.cylee.socket.tcp.IConnectListener;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by cylee on 16/9/25.
 */
public class TimeCheckSocket extends UdpSocket {
    private static final int DEFAULT_TIMEOUT = 6000; // 6s
    private static final int ERROR_DATA_INVALID = -1;
    private static final int ERROR_SEND_ERROR = -2;
    private static final int ERROR_TIME_OUT = -3;
    private int mTimeOut = DEFAULT_TIMEOUT;

    public Map<String, PacketBindData> mBindDataMap = Collections.synchronizedMap(new HashMap<String, PacketBindData>());
    private int mId;
    private SocketAddress mTargetAddress;

    public TimeCheckSocket(boolean timeOutCheck) {
        if (timeOutCheck) {
            new Thread(new TimeOutChecker(), "TimeOutChecker").start();
        }
    }


    public static TimeCheckSocket client(SocketAddress target, IConnectListener listener, int localPort) {
        if (localPort <= 0) localPort = 0;
        TimeCheckSocket tcs = new TimeCheckSocket(true);
        tcs.mTargetAddress = target;
        return (TimeCheckSocket)tcs.connect(new InetSocketAddress(localPort), listener);
    }

    public void sendString(String data, TimeSocketListener listener) {
        if (data == null) {
            if (listener != null) {
                listener.onError(ERROR_DATA_INVALID);
            }
            return;
        }

        String id = createRequestId();
        PacketBindData pb = new PacketBindData();
        pb.senTime = System.currentTimeMillis();
        pb.mSendId = id;
        pb.mListener = listener;
        mBindDataMap.put(id, pb);

        data = correctLength(data, id);

        byte[] sendData = data.getBytes();
        try {
            if (mTargetAddress != null) {
                super.send(new DatagramPacket(sendData, sendData.length, mTargetAddress));
            } else {
                super.send(new DatagramPacket(sendData, sendData.length));
            }
        } catch (Exception e) {
            mBindDataMap.remove(pb.mSendId);
            if (listener != null) {
                listener.onError(ERROR_SEND_ERROR);
            }
        }
    }

    private String correctLength(String rawData, String id) {
        if (rawData == null || id == null) return  "";
        int len = rawData.length();
        if (len < 5) { // 不足5位,补齐
            for (int i = 0; i < 5 - len; i++) {
                rawData = rawData.concat("0");
            }
        }

        String op = rawData.substring(0, 5); // 前五位为指令码
        String data = rawData.substring(5);
        String result = op + id + data;
        if (len < 6) { // 不足6位,补齐
            for (int i = 0; i < 6 - len; i++) {
                result = result.concat("0");
            }
        }
        return result+"^";
    }

    public void setTargetAddress(SocketAddress address) {
        mTargetAddress = address;
    }

    @Override
    protected void onReceive(DatagramPacket dp) {
        super.onReceive(dp);
        String receiveData = new String(dp.getData(), dp.getOffset(), dp.getLength());
        if (receiveData.length() > 2) {
            String id = receiveData.substring(0, 2);
            PacketBindData pb = mBindDataMap.get(id);
            if (pb != null) {
                if (pb.mListener != null) {
                    pb.mListener.onRawData(dp);
                    int endIndex = receiveData.indexOf("^");
                    if (endIndex > 2) {
                        String result = receiveData.substring(2, endIndex);
                        pb.mListener.onSuccess(result);
                    }
                }
                mBindDataMap.remove(id);
            }
        }
    }

    private synchronized String createRequestId() {
        mId ++;
        mId %= 0xFF;
        return String.format("%02x", mId);
    }

    static class PacketBindData {
        public long senTime;
        public String mSendId;
        public TimeSocketListener mListener;
    }

    public interface TimeSocketListener extends BaseTimeSocketListener {
        void onRawData(DatagramPacket rawData);
    }

    public static class AbsTimeSocketListener implements TimeSocketListener{
        @Override
        public void onError(int errorCode) {

        }

        @Override
        public void onRawData(DatagramPacket rawData) {

        }

        @Override
        public void onSuccess(String data) {

        }
    }

    class TimeOutChecker implements Runnable {
        @Override
        public void run() {
            while (!mStoped) {
                try {
                    Thread.sleep(mTimeOut / 3);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                if (!mBindDataMap.isEmpty()) {
                    Iterator<String> iterator = mBindDataMap.keySet().iterator();
                    while (iterator.hasNext()) {
                        String id = iterator.next();
                        PacketBindData pb = mBindDataMap.get(id);
                        if (pb.senTime + mTimeOut <= System.currentTimeMillis()) {
                            iterator.remove();
                            if (pb.mListener != null) {
                                pb.mListener.onError(ERROR_TIME_OUT);
                            }
                        }
                    }
                }
            }
        }
    }
 }
