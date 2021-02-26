package com.nukkitx.proxypass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebsocketServer extends WebSocketServer {
    ProxyPass proxy;

    public WebsocketServer(int port, ProxyPass proxy) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.proxy = proxy;
    }

    public WebsocketServer(InetSocketAddress address, ProxyPass proxy) {
        super(address);
        this.proxy = proxy;
    }

    public WebsocketServer(int port, ProxyPass proxy, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.singletonList(draft));
        this.proxy = proxy;
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            this.proxy.handleEvent("filteringPackets", ProxyPlayerSession.getIdBiMap(Bedrock_v422.V422_CODEC));
        } catch (NoSuchFieldException|IllegalAccessException|JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected!");
    }

    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println(conn + " has disconnected!");
    }

    public void onMessage(WebSocket conn, String message) {
        System.out.println(conn + ": " + message);
        try {
            JsonNode messageNode = ProxyPlayerSession.jsonSerializer.readTree(message);
            switch (messageNode.get("type").textValue()) {
                case "inject":
                    ProxyPlayerSession.injectPacketStatic(messageNode.get("data"), messageNode
                            .get("className").textValue(), messageNode
                            .get("direction").textValue());
                    return;
            }
            System.out.println("Unknown message type " + messageNode.get("type").textValue());
        }
        catch (JsonProcessingException|ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println(conn + ": " + message);
    }

    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null);
    }

    public void onStart() {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}