package com.nukkitx.proxypass.network.bedrock.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.exception.PacketSerializeException;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import com.nukkitx.proxypass.ProxyPass;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Log4j2
@Getter
public class ProxyPlayerSession {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final BedrockServerSession upstream;
    private final BedrockClientSession downstream;
    private final ProxyPass proxy;
    private final AuthData authData;
    private final Path dataPath;
    private final Path logPath;
    private final long timestamp = System.currentTimeMillis();
    @Getter(AccessLevel.PACKAGE)
    private final KeyPair proxyKeyPair = EncryptionUtils.createKeyPair();
    private final Deque<String> logBuffer = new ArrayDeque<>();
    private volatile boolean closed = false;
    // TODO: private
    public static ObjectMapper jsonSerializer = new ObjectMapper();

    public ProxyPlayerSession(BedrockServerSession upstream, BedrockClientSession downstream, ProxyPass proxy, AuthData authData) {
        this.upstream = upstream;
        this.downstream = downstream;
        this.proxy = proxy;
        this.authData = authData;
        this.dataPath = proxy.getSessionsDir().resolve(this.authData.getDisplayName() + '-' + timestamp);
        this.logPath = dataPath.resolve("packets.log");
        if (proxy.getConfiguration().isLoggingPackets() &&
                proxy.getConfiguration().getLogTo().logToFile) {
            log.debug("Packets will be logged under " + logPath.toString());
            try {
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.upstream.addDisconnectHandler(reason -> {
            if (reason != DisconnectReason.DISCONNECTED) {
                this.downstream.disconnect();
            }
        });
        if (proxy.getConfiguration().isLoggingPackets()) {
            executor.scheduleAtFixedRate(this::flushLogBuffer, 5, 5, TimeUnit.SECONDS);
        }
    }

    public BatchHandler getUpstreamBatchHandler() {
        return new ProxyBatchHandler(downstream, true);
    }

    public BatchHandler getDownstreamTailHandler() {
        return new ProxyBatchHandler(upstream, false);
    }

    private void log(Supplier<String> supplier) {
        if (proxy.getConfiguration().isLoggingPackets()) {
            synchronized (logBuffer) {
                logBuffer.addLast(supplier.get());
            }
        }
    }

    private void flushLogBuffer() {
        synchronized (logBuffer) {
            try {
                if (proxy.getConfiguration().getLogTo().logToFile) {
                    Files.write(logPath, logBuffer, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                }
                logBuffer.clear();
            } catch (IOException e) {
                log.error("Unable to flush packet log", e);
            }
        }
    }

    private class ProxyBatchHandler implements BatchHandler {
        private final BedrockSession session;
        private final String logPrefix;

        private ProxyBatchHandler(BedrockSession session, boolean upstream) {
            this.session = session;
            this.logPrefix = upstream ? "[SERVER BOUND]  -  " : "[CLIENT BOUND]  -  ";
        }

        @Override
        public void handle(BedrockSession session, ByteBuf compressed, Collection<BedrockPacket> packets) {
            boolean packetTesting = ProxyPlayerSession.this.proxy.getConfiguration().isPacketTesting();
            boolean batchHandled = false;
            List<BedrockPacket> unhandled = new ArrayList<>();
            for (BedrockPacket packet : packets) {
                if (!proxy.isIgnoredPacket(packet.getClass())) {
                    if (session.isLogging() && log.isTraceEnabled()) {
                        log.trace(this.logPrefix + " {}: {}", session.getAddress(), packet);
                    }
                    ProxyPlayerSession.this.log(() -> logPrefix + packet.toString());
                    if (proxy.getConfiguration().isLoggingPackets() &&
                            proxy.getConfiguration().getLogTo().logToConsole) {
                        // System.out.println(logPrefix + packet.toString());
                        try {
                            System.out.println(logPrefix + jsonSerializer.writeValueAsString(packet));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                }

                BedrockPacketHandler handler = session.getPacketHandler();

                if (handler != null && packet.handle(handler)) {
                    batchHandled = true;
                } else {
                    unhandled.add(packet);
                }

                if (packetTesting) {
                    int packetId = ProxyPass.CODEC.getId(packet.getClass());
                    ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                    try {
                        ProxyPass.CODEC.tryEncode(buffer, packet, session);
                        BedrockPacket packet2 = ProxyPass.CODEC.tryDecode(buffer, packetId, session);
                        // buffer.release();

                        // TODO: Separate from main testing?
                        String jsonData = jsonSerializer.writeValueAsString(packet);
                        // BedrockPacket packet2 = null;
                        try {
                            packet2 = jsonSerializer.readValue(jsonData, packet.getClass());
                        } catch (JsonMappingException e) {
                            System.out.println("Error in packet: " + packet.getClass());
                            System.out.println("Data:            " + jsonData);
                            e.printStackTrace();
                        }

                        // Reencode json thingy
                        /* buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                        ProxyPass.CODEC.tryEncode(buffer, packet2, session);
                        packet2 = ProxyPass.CODEC.tryDecode(buffer, packetId, session); */

                        if (!Objects.equals(packet, packet2)) {
                            // Something went wrong in serialization.
                            log.warn("Packets instances not equal:\n Original  : {}\nRe-encoded : {}",
                                    packet, packet2);
                        } else {
                            /* log.warn("Equal!!!!!!!!!:\n Original  : {}\nRe-encoded : {}",
                                    packet.getClass(), packet2.getClass()); */
                        }
                    } catch (PacketSerializeException e) {
                        //ignore
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    } finally {
                        buffer.release();
                    }
                    // System.out.println(ProxyPass.testPacket(packet));
                }
            }

            if (!batchHandled) {
                compressed.resetReaderIndex();
                this.session.sendWrapped(compressed, true);
            } else if (!unhandled.isEmpty()) {
                this.session.sendWrapped(unhandled, true);
            }
        }
    }
}
