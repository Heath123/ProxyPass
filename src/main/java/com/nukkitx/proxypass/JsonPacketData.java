package com.nukkitx.proxypass;

import com.nukkitx.protocol.bedrock.BedrockPacketType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonPacketData {
    // TODO: enum or boolean?
    public String direction;
    public String jsonData;
    public int packetId;
    public BedrockPacketType packetType;
    public String className;
    public byte[] bytes;
    // If a custom handler was called that blocked the packet and setting dontSendPackets wouldn't block the sending
    public boolean isHandled;

    // Used for things that aren't actually packets, but events
    public boolean isEvent;
    public String eventType;
    public String eventData;
}
