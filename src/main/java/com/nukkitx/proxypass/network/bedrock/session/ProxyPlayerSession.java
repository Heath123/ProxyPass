package com.nukkitx.proxypass.network.bedrock.session;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.NetworkStackLatencyPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumConstraintData;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.data.inventory.*;
import com.nukkitx.protocol.bedrock.data.skin.AnimationData;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.PersonaPieceData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.exception.PacketSerializeException;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.protocol.util.Int2ObjectBiMap;
import com.nukkitx.proxypass.JsonPacketData;
import com.nukkitx.proxypass.ProxyPass;
import io.netty.buffer.ByteBuf;
import com.nukkitx.proxypass.deserializers.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
    // Used for packet injecting from a static context when one client is connected
    // TODO: Set to null when not connected
    private static ProxyPlayerSession aSession;

    @Setter
    public static boolean dontSendPackets = false;

    /**
     * Should only be used when one client is connected
     */
    public static void injectPacketStatic(String jsonData, String className, String writeTo) throws JsonProcessingException, ClassNotFoundException {
        Class packetClass = Class.forName(className);
        BedrockPacket packet = (BedrockPacket) jsonSerializer.readValue(jsonData, packetClass);
        System.out.println("Sending " + packet.toString() + " to " + writeTo);
        if (writeTo == "client") {
            aSession.downstream.sendPacketImmediately(packet);
        } else {
            aSession.upstream.sendPacketImmediately(packet);
        }
    }

    /**
     * Should only be used when one client is connected
     * @return TODO
     */
    public static String getIdBiMapStatic() throws NoSuchFieldException, IllegalAccessException, JsonProcessingException {
        Field biMapField = BedrockPacketCodec.class.getDeclaredField("idBiMap");
        biMapField.setAccessible(true);
        // TODO: dynamic
        BedrockPacketCodec codec = Bedrock_v422.V422_CODEC;
        Int2ObjectBiMap<Class<? extends BedrockPacket>> idBiMap = (Int2ObjectBiMap<Class<? extends BedrockPacket>>) biMapField.get(codec);
        // Convert
        Map<Integer, String> idTypeMap = new HashMap<>();
        idBiMap.forEach((Class<? extends BedrockPacket> value, int key) -> {
            try {
                idTypeMap.put(key,
                        value.getDeclaredConstructor().newInstance()
                                .getPacketType().toString()
                );
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        return jsonSerializer.writeValueAsString(idTypeMap);
    }

    @JsonFilter("VectorFilter")
    class VectorMixIn {}

    static {
        // jsonSerializer.activateDefaultTyping(new LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.WRAPPER_OBJECT);

        TypeResolverBuilder<?> typer = ObjectMapper.DefaultTypeResolverBuilder.construct(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, new LaissezFaireSubTypeValidator());
        // it.unimi.dsi.fastutil.objects.ObjectArrayList
        typer = typer.init(JsonTypeInfo.Id.MINIMAL_CLASS, null);
        typer = typer.inclusion(JsonTypeInfo.As.WRAPPER_OBJECT);
        jsonSerializer.setDefaultTyping(typer);

        jsonSerializer.addMixIn(Vector3f.class, VectorMixIn.class);
        jsonSerializer.addMixIn(Vector3i.class, VectorMixIn.class);
        jsonSerializer.addMixIn(Vector2f.class, VectorMixIn.class);

        // https://www.tutorialspoint.com/jackson_annotations/jackson_annotations_jsonfilter.htm
        FilterProvider filters = new SimpleFilterProvider().addFilter(
                "VectorFilter", SimpleBeanPropertyFilter.filterOutAllExcept("x", "y", "z"));

        ProxyPlayerSession.jsonSerializer.setFilterProvider(filters);

        SimpleModule module = new SimpleModule();
        // https://www.baeldung.com/jackson-deserialization
        module.addDeserializer(Vector3i.class, new Vector3iDeserializer());
        module.addDeserializer(Vector3f.class, new Vector3fDeserializer());
        module.addDeserializer(Vector2f.class, new Vector2fDeserializer());
        module.addDeserializer(ItemData.class, new ItemDataDeserializer());
        module.addDeserializer(AttributeData.class, new AttributeDataDeserializer());
        module.addDeserializer(EntityFlags.class, new EntityFlagsDeserializer());
        module.addDeserializer(GameRuleData.class, new GameRuleDataDeserializer());
        module.addDeserializer(LongList.class, new LongListDeserializer());
        module.addDeserializer(InventoryActionData.class, new InventoryActionDataDeserializer());
        module.addDeserializer(PlayerListPacket.Entry.class, new PlayerListPacket$EntryDeserializer());
        module.addDeserializer(SerializedSkin.class, new SerializedSkinDeserializer());
        module.addDeserializer(ImageData.class, new ImageDataDeserializer());
        module.addDeserializer(StartGamePacket.ItemEntry.class, new StartGamePacket$ItemEntryDeserializer());
        module.addDeserializer(NbtList.class, new NbtListDeserializer());
        module.addDeserializer(InventorySource.class, new InventorySourceDeserializer());
        module.addDeserializer(CraftingData.class, new CraftingDataDeserializer());
        module.addDeserializer(NbtMap.class, new NbtMapDeserializer());
        module.addDeserializer(PotionMixData.class, new PotionMixDataDeserializer());
        module.addDeserializer(ContainerMixData.class, new ContainerMixDataDeserializer());
        module.addDeserializer(AnimationData.class, new AnimationDataDeserializer());
        module.addDeserializer(CommandData.class, new CommandDataDeserializer());
        module.addDeserializer(PersonaPieceData.class, new PersonaPieceDataDeserializer());
        module.addDeserializer(CommandEnumData.class, new CommandEnumDataDeserializer());

        // https://www.baeldung.com/jackson-custom-serialization
        module.addSerializer(EntityFlags.class, new EntityFlagsSerializer());
        module.addSerializer(NbtList.class, new NbtListSerializer());

        jsonSerializer.registerModule(module);
        // TODO: Only ignore some like packetType
        jsonSerializer.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ProxyPlayerSession(BedrockServerSession upstream, BedrockClientSession downstream, ProxyPass proxy, AuthData authData) {
        aSession = this;
        this.upstream = upstream;
        this.downstream = downstream;
        this.proxy = proxy;
        this.authData = authData;
        this.dataPath = proxy.getSessionsDir().resolve(this.authData.getDisplayName() + '-' + timestamp);
        this.logPath = dataPath.resolve("packets.log");
        this.upstream.addDisconnectHandler(reason -> {
            // System.out.println("Disconnect!!!");
            if (reason != DisconnectReason.DISCONNECTED) {
                this.downstream.disconnect();
            }

            if (proxy.getConfiguration().isUsingPacketQueue()) {
                proxy.getConfiguration().getCallback().handlePacket(new JsonPacketData(
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        false,
                        true,
                        "disconnect",
                        ""));
            }

            /*

            // Relaunch (hack to fix rejoining)
            proxy.shutdown();
            try {
                proxy.actualBoot(proxy.getConfiguration());
            } catch (IOException e) {
                e.printStackTrace();
            }
             */
        });
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                Files.write(logPath, logBuffer, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                logBuffer.clear();
            } catch (IOException e) {
                log.error("Unable to flush packet log", e);
            }
        }
    }

    private class ProxyBatchHandler implements BatchHandler {
        private final BedrockSession session;
        private final String logPrefix;
        private final String direction;

        private ProxyBatchHandler(BedrockSession session, boolean upstream) {
            this.session = session;
            this.logPrefix = upstream ? "[SERVER BOUND]  -  " : "[CLIENT BOUND]  -  ";
            this.direction = upstream ? "serverbound" : "clientbound";
        }

        @Override
        public void handle(BedrockSession session, ByteBuf compressed, Collection<BedrockPacket> packets) {
            boolean wrapperHandled = !ProxyPlayerSession.this.proxy.getConfiguration().isPassingThrough();
            List<BedrockPacket> unhandled = new ArrayList<>();
            for (BedrockPacket packet : packets) {
                boolean thisPacketHandled = false;
                BedrockPacketHandler handler = session.getPacketHandler();

                if (handler != null && packet.handle(handler)) {
                    batchHandled = true;
                    thisPacketHandled = true;
                } else {
                    unhandled.add(packet);
                }

                if (!proxy.isIgnoredPacket(packet.getClass())) {
                    if (session.isLogging() && log.isTraceEnabled()) {
                        log.trace(this.logPrefix + " {}: {}", session.getAddress(), packet);
                    }
                    ProxyPlayerSession.this.log(() -> logPrefix + packet.toString());
                    if (proxy.getConfiguration().isLoggingPackets() &&
                            proxy.getConfiguration().getLogTo().logToConsole) {
                        System.out.println(logPrefix + packet.toString());
                    }

                    if (proxy.getConfiguration().isUsingPacketQueue()) {
                        try {

                            byte[] bytes = new byte[0];
                            ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                            try {
                                // TODO: Handle (add error to data structure?)
                                ProxyPass.CODEC.tryEncode(buffer, packet, session);
                                bytes = new byte[buffer.readableBytes()];
                                buffer.readBytes(bytes);
                            } catch (PacketSerializeException e) {
                                e.printStackTrace();
                            } finally {
                                buffer.release();
                            }

                            proxy.getConfiguration().getCallback().handlePacket(new JsonPacketData(
                                    direction,
                                    jsonSerializer.writeValueAsString(packet),
                                    packet.getPacketId(),
                                    packet.getPacketType(),
                                    packet.getClass().getName(),
                                    bytes,
                                    thisPacketHandled,
                                    false,
                                    "",
                                    ""));

                        } catch (JsonProcessingException e) {
                            // TODO: Handle (add error to data structure?)
                            e.printStackTrace();
                        }
                    }
                }
                ProxyPlayerSession.this.log(() -> logPrefix + packet.toString());

                BedrockPacketHandler handler = session.getPacketHandler();

                if (handler != null && packet.handle(handler)) {
                    wrapperHandled = true;
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

            if (!dontSendPackets) {
                if (!wrapperHandled) {
                    compressed.resetReaderIndex();
                    this.session.sendWrapped(compressed, true);
                } else if (!unhandled.isEmpty()) {
                    this.session.sendWrapped(unhandled, true);
                }
            }
        }
    }
}
