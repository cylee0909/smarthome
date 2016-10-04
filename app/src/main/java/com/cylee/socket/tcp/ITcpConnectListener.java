package com.cylee.socket.tcp;

/**
 * Created by cylee on 16/9/25.
 */
public interface ITcpConnectListener {
    int ERROR_READ_ERROR = -1;
    int ERROR_WRITE_ERROR = -2;

    void onConnect(TcpSocket socket);

    void onConnectFail(int errCode);

    void onReceive(TcpSocket socket, String data);
}
