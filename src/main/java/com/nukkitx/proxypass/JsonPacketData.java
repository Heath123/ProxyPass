package com.nukkitx.proxypass;

import com.nukkitx.protocol.bedrock.BedrockPacketType;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class JsonPacketData {
    // TODO: enum or boolean?
    public String direction;
    public String jsonData;
    public int packetId;
    public BedrockPacketType packetType;
    public String className;
    @NonNull
    public byte[] bytes;
    // If a custom handler was called that blocked the packet and setting dontSendPackets wouldn't block the sending
    public boolean isHandled;
}
