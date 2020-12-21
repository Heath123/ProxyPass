package com.nukkitx.proxypass;

import com.nukkitx.protocol.bedrock.BedrockPacketType;
import io.netty.buffer.ByteBuf;
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
}
