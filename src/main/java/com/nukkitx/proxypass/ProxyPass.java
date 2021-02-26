package com.nukkitx.proxypass;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.proxypass.network.ProxyBedrockEventHandler;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;
import io.netty.util.ResourceLeakDetector;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Getter
public class ProxyPass {
    // TODO: remove
    // Not an actual Queue because it's emptied all at once
    // Used for pakkit
    // public static ArrayList<JsonPacketData> packetQueue = new ArrayList<>();

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    public static final YAMLMapper YAML_MAPPER = (YAMLMapper) new YAMLMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    public static final String MINECRAFT_VERSION;
    public static final BedrockPacketCodec CODEC = Bedrock_v422.V422_CODEC;
    public static final int PROTOCOL_VERSION = CODEC.getProtocolVersion();
    private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter();

    static {
        DefaultIndenter indenter = new DefaultIndenter("    ", "\n");
        PRETTY_PRINTER.indentArraysWith(indenter);
        PRETTY_PRINTER.indentObjectsWith(indenter);
        String minecraftVersion;

        try {
            minecraftVersion = CODEC.getMinecraftVersion();
        } catch (NullPointerException e) {
            minecraftVersion = "0.0.0";
        }
        MINECRAFT_VERSION = minecraftVersion;
    }

    private final AtomicBoolean running = new AtomicBoolean(true);
    private BedrockServer bedrockServer;
    private final Set<BedrockClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private int maxClients = 0;
    @Getter(AccessLevel.NONE)
    private final Set<Class<?>> ignoredPackets = Collections.newSetFromMap(new IdentityHashMap<>());
    private InetSocketAddress targetAddress;
    private InetSocketAddress proxyAddress;
    private Configuration configuration;
    private Path baseDir;
    private Path sessionsDir;
    private Path dataDir;
    public static WebsocketServer websocketServer;

    public void handlePacket(JsonPacketData packet) {
        ObjectNode rootNode = ProxyPlayerSession.jsonSerializer.createObjectNode();
        rootNode.put("type", "packet");
        rootNode.set("data", ProxyPlayerSession.jsonSerializer.valueToTree(packet));
        try {
            websocketServer.broadcast(ProxyPlayerSession.jsonSerializer.writeValueAsString(rootNode));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void handleEvent(String eventType, String eventData) {
        ObjectNode rootNode = ProxyPlayerSession.jsonSerializer.createObjectNode();
        rootNode.put("type", "event");
        rootNode.put("eventType", eventType);
        rootNode.put("eventData", eventData);
        try {
            websocketServer.broadcast(ProxyPlayerSession.jsonSerializer.writeValueAsString(rootNode));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    abstract class Vector3iMixin {

        // private final int x;
        // private final int y;
        // private final int z;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        private Vector3iMixin(@JsonProperty("x") int x, @JsonProperty("y") int y, @JsonProperty("z") int z) {

        }
    }

    // Vector3i test = new Vector3i();

    public static void main(String[] args) throws IOException {
        /*
        // String serializedObject = "";
        // BedrockPacket packet = new StartGamePacket();
        // System.out.println(testPacket(packet));
        // PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().build();
        // ProxyPlayerSession.jsonSerializer.activateDefaultTyping(ptv); // default to using DefaultTyping.OBJECT_AND_NON_CONCRETE
        // ProxyPlayerSession.jsonSerializer.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        ProxyPlayerSession.jsonSerializer.activateDefaultTyping(new LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
        // ProxyPlayerSession.jsonSerializer.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        /*

       .activateDefaultTypingAsProperty(BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(JobState.class)
                                .allowIfBaseType(Map.class)
                                .allowIfBaseType(JobContext.Metadata.class)
                                .build(),

         */
        /* ProxyPlayerSession.jsonSerializer.addMixIn(Vector3i.class, Vector3iMixin.class);

        ProxyPlayerSession.jsonSerializer.setVisibility(ProxyPlayerSession.jsonSerializer.getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)); */ /*

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
        module.addDeserializer(InventoryActionData.class, new InventoryActionDataSerializer());
        module.addDeserializer(PlayerListPacket.Entry.class, new PlayerListPacket$EntryDeserializer());
        // TODO: SerializedSkin
        // TODO: StartGamePacket$ItemEntry

        // https://www.baeldung.com/jackson-custom-serialization
        module.addSerializer(EntityFlags.class, new EntityFlagsSerializer());

        ProxyPlayerSession.jsonSerializer.registerModule(module);
        // TODO: Only ignore some like packetType
        ProxyPlayerSession.jsonSerializer.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        /* MessagePack msgpack = new MessagePack();
        msgpack.register(BedrockPacket.class);
        byte[] raw = msgpack.write(packet); */

        // serialize the object
        /* try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(packet);
            so.flush();
            serializedObject = bo.toString();
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println(serializedObject); */

        /* try {
            System.out.println(ProxyPlayerSession.jsonSerializer.writeValueAsString(new StartGamePacket()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } */

        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"x\": 10.2, \"y\": 10.6, \"z\": 10.9}", Vector3f.class));
        // {"id":0,"damage":0,"count":0,"tag":null,"canPlace":[],"canBreak":[],"blockingTicks":0,"netId":0,"valid":false,"null":true}
        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"id\":0,\"damage\":0,\"count\":0,\"tag\":null,\"canPlace\":[],\"canBreak\":[],\"blockingTicks\":0,\"netId\":0,\"valid\":false,\"null\":true}", ItemData.class));
        // {"name":"minecraft:health","minimum":0.0,"maximum":20.0,"value":20.0,"defaultValue":20.0}
        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"name\":\"minecraft:health\",\"minimum\":0.0,\"maximum\":20.0,\"value\":20.0,\"defaultValue\":20.0}", AttributeData.class));
        /*
        EntityFlags testFlags = new EntityFlags();

        testFlags.setFlag(EntityFlag.ADMIRING, true);
        testFlags.setFlag(EntityFlag.BABY, true);

        /* String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(testFlags);

        System.out.println(jsonData);

        System.out.println(ProxyPlayerSession.jsonSerializer.readValue(jsonData, EntityFlags.class)); */

        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"name\":\"commandblockoutput\",\"value\":true}", GameRuleData.class));

        // {"packetId":58,"senderId":0,"clientId":0,"chunkX":-3,"chunkZ":9,"subChunksLength":6,"cachingEnabled":true,"blobIds":[3347041282475867671,-3513989425754612621,3270511442658853108,8130671249425344037,-336159327604660822,-1805625525965675367,7369432827740918248],"data":"AA==","packetType":"LEVEL_CHUNK"}

        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"packetId\":58,\"senderId\":0,\"clientId\":0,\"chunkX\":-3,\"chunkZ\":9,\"subChunksLength\":6,\"cachingEnabled\":true,\"blobIds\":[3347041282475867671,-3513989425754612621,3270511442658853108,8130671249425344037,-336159327604660822,-1805625525965675367,7369432827740918248],\"data\":\"AA==\",\"packetType\":\"LEVEL_CHUNK\"}", LevelChunkPacket.class));

        // Data:            {"packetId":13,"senderId":0,"clientId":0,"attributes":[],"metadata":{"FLAGS":["INVISIBLE","HAS_COLLISION","HAS_GRAVITY"],"FLAGS_2":["INVISIBLE","HAS_COLLISION","HAS_GRAVITY"],"HEALTH":1,"VARIANT":0,"COLOR":0,"NAMETAG":"","OWNER_EID":-21474836469,"AIR_SUPPLY":300,"DISPLAY_OFFSET":-21474836469,"CUSTOM_DISPLAY":0,"CHARGE_AMOUNT":0,"LEASH_HOLDER_EID":0,"SCALE":1.0,"HAS_NPC_COMPONENT":0,"MAX_AIR_SUPPLY":300,"MARK_VARIANT":0,"CONTAINER_TYPE":0,"CONTAINER_BASE_SIZE":0,"CONTAINER_STRENGTH_MODIFIER":0,"BOUNDING_BOX_WIDTH":0.25,"BOUNDING_BOX_HEIGHT":0.25,"RIDER_SEAT_POSITION":{"x":0.0,"y":0.0,"z":0.0,"minAxis":2,"maxAxis":0,"floorZ":0,"floorX":0,"floorY":0},"RIDER_ROTATION_LOCKED":0,"RIDER_MAX_ROTATION":0.0,"RIDER_MIN_ROTATION":0.0,"COMMAND_BLOCK_ENABLED":0,"COMMAND_BLOCK_COMMAND":"","COMMAND_BLOCK_LAST_OUTPUT":"","COMMAND_BLOCK_TRACK_OUTPUT":1,"CONTROLLING_RIDER_SEAT_INDEX":0,"STRENGTH":0,"MAX_STRENGTH":0,"EVOKER_SPELL_COLOR":0,"LIMITED_LIFE":-1,"NAMETAG_ALWAYS_SHOW":-1,"COLOR_2":0,"TRADE_TIER":0,"MAX_TRADE_TIER":0,"TRADE_XP":0,"SKIN_ID":0,"COMMAND_BLOCK_TICK_DELAY":3,"COMMAND_BLOCK_EXECUTE_ON_FIRST_TICK":1,"AMBIENT_SOUND_INTERVAL":8.0,"AMBIENT_SOUND_INTERVAL_RANGE":16.0,"AMBIENT_SOUND_EVENT_NAME":"ambient","FALL_DAMAGE_MULTIPLIER":1.0,"CAN_RIDE_TARGET":0,"LOW_TIER_CURED_TRADE_DISCOUNT":0,"HIGH_TIER_CURED_TRADE_DISCOUNT":0,"NEARBY_CURED_TRADE_DISCOUNT":0,"NEARBY_CURED_DISCOUNT_TIME_STAMP":0,"HITBOX":{},"IS_BUOYANT":0},"entityLinks":[],"uniqueEntityId":-21474835947,"runtimeEntityId":1294,"identifier":"minecraft:arrow","entityType":0,"position":{"x":42.164177,"y":69.0,"z":7.9501715,"minAxis":2,"maxAxis":1,"floorZ":7,"floorX":42,"floorY":69},"motion":{"x":0.0,"y":0.0,"z":0.0,"minAxis":2,"maxAxis":0,"floorZ":0,"floorX":0,"floorY":0},"rotation":{"x":-64.6875,"y":15.46875,"z":0.0,"minAxis":0,"maxAxis":1,"floorZ":0,"floorX":-65,"floorY":15},"packetType":"ADD_ENTITY"}

        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"packetId\":13,\"senderId\":0,\"clientId\":0,\"attributes\":[],\"metadata\":{\"FLAGS\":[\"INVISIBLE\",\"HAS_COLLISION\",\"HAS_GRAVITY\"],\"FLAGS_2\":[\"INVISIBLE\",\"HAS_COLLISION\",\"HAS_GRAVITY\"],\"HEALTH\":1,\"VARIANT\":0,\"COLOR\":0,\"NAMETAG\":\"\",\"OWNER_EID\":-21474836469,\"AIR_SUPPLY\":300,\"DISPLAY_OFFSET\":-21474836469,\"CUSTOM_DISPLAY\":0,\"CHARGE_AMOUNT\":0,\"LEASH_HOLDER_EID\":0,\"SCALE\":1.0,\"HAS_NPC_COMPONENT\":0,\"MAX_AIR_SUPPLY\":300,\"MARK_VARIANT\":0,\"CONTAINER_TYPE\":0,\"CONTAINER_BASE_SIZE\":0,\"CONTAINER_STRENGTH_MODIFIER\":0,\"BOUNDING_BOX_WIDTH\":0.25,\"BOUNDING_BOX_HEIGHT\":0.25,\"RIDER_SEAT_POSITION\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"minAxis\":2,\"maxAxis\":0,\"floorZ\":0,\"floorX\":0,\"floorY\":0},\"RIDER_ROTATION_LOCKED\":0,\"RIDER_MAX_ROTATION\":0.0,\"RIDER_MIN_ROTATION\":0.0,\"COMMAND_BLOCK_ENABLED\":0,\"COMMAND_BLOCK_COMMAND\":\"\",\"COMMAND_BLOCK_LAST_OUTPUT\":\"\",\"COMMAND_BLOCK_TRACK_OUTPUT\":1,\"CONTROLLING_RIDER_SEAT_INDEX\":0,\"STRENGTH\":0,\"MAX_STRENGTH\":0,\"EVOKER_SPELL_COLOR\":0,\"LIMITED_LIFE\":-1,\"NAMETAG_ALWAYS_SHOW\":-1,\"COLOR_2\":0,\"TRADE_TIER\":0,\"MAX_TRADE_TIER\":0,\"TRADE_XP\":0,\"SKIN_ID\":0,\"COMMAND_BLOCK_TICK_DELAY\":3,\"COMMAND_BLOCK_EXECUTE_ON_FIRST_TICK\":1,\"AMBIENT_SOUND_INTERVAL\":8.0,\"AMBIENT_SOUND_INTERVAL_RANGE\":16.0,\"AMBIENT_SOUND_EVENT_NAME\":\"ambient\",\"FALL_DAMAGE_MULTIPLIER\":1.0,\"CAN_RIDE_TARGET\":0,\"LOW_TIER_CURED_TRADE_DISCOUNT\":0,\"HIGH_TIER_CURED_TRADE_DISCOUNT\":0,\"NEARBY_CURED_TRADE_DISCOUNT\":0,\"NEARBY_CURED_DISCOUNT_TIME_STAMP\":0,\"HITBOX\":{},\"IS_BUOYANT\":0},\"entityLinks\":[],\"uniqueEntityId\":-21474835947,\"runtimeEntityId\":1294,\"identifier\":\"minecraft:arrow\",\"entityType\":0,\"position\":{\"x\":42.164177,\"y\":69.0,\"z\":7.9501715,\"minAxis\":2,\"maxAxis\":1,\"floorZ\":7,\"floorX\":42,\"floorY\":69},\"motion\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"minAxis\":2,\"maxAxis\":0,\"floorZ\":0,\"floorX\":0,\"floorY\":0},\"rotation\":{\"x\":-64.6875,\"y\":15.46875,\"z\":0.0,\"minAxis\":0,\"maxAxis\":1,\"floorZ\":0,\"floorX\":-65,\"floorY\":15},\"packetType\":\"ADD_ENTITY\"}", AddEntityPacket.class));

        // System.out.println(ProxyPlayerSession.jsonSerializer.readValue("{\"packetId\":13,\"senderId\":0,\"clientId\":0,\"attributes\":[]," +
        //         "\"metadata\":{\"FLAGS\":[\"INVISIBLE\",\"HAS_COLLISION\",\"HAS_GRAVITY\"]}" +
        //         ",\"entityLinks\":[],\"uniqueEntityId\":-21474835947,\"runtimeEntityId\":1294,\"identifier\":\"minecraft:arrow\",\"entityType\":0,\"position\":{\"x\":42.164177,\"y\":69.0,\"z\":7.9501715,\"minAxis\":2,\"maxAxis\":1,\"floorZ\":7,\"floorX\":42,\"floorY\":69},\"motion\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"minAxis\":2,\"maxAxis\":0,\"floorZ\":0,\"floorX\":0,\"floorY\":0},\"rotation\":{\"x\":-64.6875,\"y\":15.46875,\"z\":0.0,\"minAxis\":0,\"maxAxis\":1,\"floorZ\":0,\"floorX\":-65,\"floorY\":15},\"packetType\":\"ADD_ENTITY\"}", AddEntityPacket.class));

        /*
        AddEntityPacket test = new AddEntityPacket();
        test.getMetadata().putFlags(testFlags);
        String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(test);
        System.out.println(jsonData);

        System.out.println(ProxyPlayerSession.jsonSerializer.readValue(jsonData, AddEntityPacket.class)); */

        /* ClientCacheMissResponsePacket test = new ClientCacheMissResponsePacket();
        String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(test);
        System.out.println(jsonData);

        ClientCacheMissResponsePacket test2 = ProxyPlayerSession.jsonSerializer.readValue(jsonData, ClientCacheMissResponsePacket.class);
        System.out.println(test2.equals(test)); */

        /* Vector3i test = Vector3i.from(5, 6, 7);
        String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(test);
        System.out.println(jsonData);

        Vector3f test2 = ProxyPlayerSession.jsonSerializer.readValue(jsonData, Vector3f.class);
        System.out.println(test2);
        System.out.println(test2.equals(test)); */

        /* SerializedSkin test = SerializedSkin.of("test", "{\"geometry\":{\"default\":\"test\"}}", ImageData.of(1, 1, new byte[]{1, 1, 1}),
                new ArrayList<AnimationData>(), ImageData.of(1, 1, new byte[]{1, 1, 1}), "test3",
                "test4", true, true, true,
                "test5", "test6", "test7", "test8",
                new ArrayList<PersonaPieceData>(), new ArrayList<PersonaPieceTintData>());
        System.out.println(test);
        String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(test);
        System.out.println(jsonData);

        SerializedSkin test2 = ProxyPlayerSession.jsonSerializer.readValue(jsonData, SerializedSkin.class);
        System.out.println(test2);
        System.out.println(test2.equals(test)); */

        /* NbtList test = new NbtList(NbtType.FLOAT, 1f, 2f, 3f);
        System.out.println(test);
        String jsonData = ProxyPlayerSession.jsonSerializer.writeValueAsString(test);
        System.out.println(jsonData);

        NbtList test2 = ProxyPlayerSession.jsonSerializer.readValue(jsonData, NbtList.class);
        System.out.println(test2);
        System.out.println(test2.equals(test)); */

        System.out.println(Arrays.toString(args) + args.length);

        if (args.length > 0 && args[0].equals("--start-from-args")) {
            if (args.length != 12) {
                System.out.println(args.length + " argument(s) were provided (12 required)");

                return;
            }
            startFromArgs(args[1], Integer.parseInt(args[2]), args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]), args[6].equals("true"), args[7].equals("true"), args[8], args[9], args[10].equals("true"), Integer.parseInt(args[11]));
        } else {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
            ProxyPass proxy = new ProxyPass();
            try {
                proxy.boot();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void startFromArgs(String proxyHost, int proxyPort, String destinationHost, int destinationPort,
                                     int maxClients, boolean usePacketQueue, boolean avoidFileCreation, String motd,
                                     String subMotd, boolean useWebsocketServer, int websocketPort) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        ProxyPass proxy = new ProxyPass();
        try {
            proxy.bootFromArgs(proxyHost, proxyPort, destinationHost, destinationPort, maxClients, usePacketQueue,
                    avoidFileCreation, motd, subMotd, useWebsocketServer, websocketPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bootFromArgs(String proxyHost, int proxyPort, String destinationHost, int destinationPort,
                             int maxClients, boolean usePacketQueue, boolean avoidFileCreation, String motd,
                             String subMotd, boolean useWebsocketServer, int websocketPort) throws IOException  {
        configuration = new Configuration();

        Configuration.Address proxyAddress = new Configuration.Address();
        proxyAddress.setHost(proxyHost);
        proxyAddress.setPort(proxyPort);
        configuration.setProxy(proxyAddress);

        Configuration.Address destinationAddress = new Configuration.Address();
        destinationAddress.setHost(destinationHost);
        destinationAddress.setPort(destinationPort);
        configuration.setDestination(destinationAddress);

        configuration.setMaxClients(maxClients);
        configuration.setUsingPacketQueue(usePacketQueue);
        configuration.setAvoidingFileCreation(avoidFileCreation);
        configuration.setMotd(motd);
        configuration.setSubMotd(subMotd);

        if (useWebsocketServer) {
            websocketServer = new WebsocketServer(websocketPort, this);
            websocketServer.start();
            System.out.println("ProxyPass - Websocket started on port: " + websocketServer.getPort());
        }

        actualBoot(configuration);
    }

    public void boot() throws IOException {
        log.info("Loading configuration...");
        Path configPath = Paths.get(".").resolve("config.yml");
        if (Files.notExists(configPath) || !Files.isRegularFile(configPath)) {
            Files.copy(ProxyPass.class.getClassLoader().getResourceAsStream("config.yml"), configPath, StandardCopyOption.REPLACE_EXISTING);
        }

        configuration = Configuration.load(configPath);

        actualBoot(configuration);
    }

    public void actualBoot(Configuration configuration) throws IOException {
        // To shut down from a static content
        instance = this;

        proxyAddress = configuration.getProxy().getAddress();
        targetAddress = configuration.getDestination().getAddress();
        maxClients = configuration.getMaxClients();

        configuration.getIgnoredPackets().forEach(s -> {
            try {
                ignoredPackets.add(Class.forName("com.nukkitx.protocol.bedrock.packet." + s));
            } catch (ClassNotFoundException e) {
                log.warn("No packet with name {}", s);
            }
        });

        baseDir = Paths.get(".").toAbsolutePath();
        sessionsDir = baseDir.resolve("sessions");
        dataDir = baseDir.resolve("data");
        if (!configuration.isAvoidingFileCreation()) {
            Files.createDirectories(sessionsDir);
            Files.createDirectories(dataDir);
        }

        log.info("Loading server...");
        this.bedrockServer = new BedrockServer(this.proxyAddress);
        this.bedrockServer.setHandler(new ProxyBedrockEventHandler(this));
        this.bedrockServer.bind().join();
        log.info("RakNet server started on {}", proxyAddress);

        loop();
    }

    public BedrockClient newClient() {
        InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", ThreadLocalRandom.current().nextInt(20000, 60000));
        BedrockClient client = new BedrockClient(bindAddress);
        this.clients.add(client);
        client.bind().join();
        return client;
    }

    private void loop() {
        while (running.get()) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                // ignore
            }

        }
        // Shutdown
        this.clients.forEach(BedrockClient::close);
        this.bedrockServer.close();
    }

    // To shut down from a static content
    private static ProxyPass instance;

    public static void shutdownStatic() {
        instance.shutdown();
    }

    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            synchronized (this) {
                this.notify();
            }
        }
    }

    public void saveNBT(String dataName, Object dataTag) {
        Path path = dataDir.resolve(dataName + ".dat");
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             NBTOutputStream nbtOutputStream = NbtUtils.createNetworkWriter(outputStream)){
            nbtOutputStream.writeTag(dataTag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object loadNBT(String dataName) {
        Path path = dataDir.resolve(dataName + ".dat");
        try (InputStream inputStream = Files.newInputStream(path);
             NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(inputStream)){
            return nbtInputStream.readTag();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveJson(String name, Object object) {
        Path outPath = dataDir.resolve(name);
        try (OutputStream outputStream = Files.newOutputStream(outPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            ProxyPass.JSON_MAPPER.writer(PRETTY_PRINTER).writeValue(outputStream, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T loadJson(String name, TypeReference<T> reference) {
        Path path = dataDir.resolve(name);
        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            return ProxyPass.JSON_MAPPER.readValue(inputStream, reference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMojangson(String name, NbtMap nbt) {
        Path outPath = dataDir.resolve(name);
        try {
            Files.write(outPath, nbt.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isIgnoredPacket(Class<?> clazz) {
        return this.ignoredPackets.contains(clazz);
    }
    
    public boolean isFull() {
        return maxClients > 0 ? this.clients.size() >= maxClients : false;
    }
}
