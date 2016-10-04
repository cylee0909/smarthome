package com.cylee.socket.tcp;

import com.cylee.socket.UdpSocket;

/**
 * Created by cylee on 16/9/25.
 */
public interface IConnectListener {
    void onConnect(UdpSocket socket);

    void onReceive(UdpSocket socket, byte[] data);
}
