package com.nukkitx.proxypass;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.stream.NetworkDataOutputStream;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.proxypass.network.ProxyBedrockEventHandler;
import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v291.serializer.ResourcePacksInfoSerializer_v291;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import com.nukkitx.proxypass.deserializers.*;
import com.nukkitx.proxypass.network.ProxyBedrockEventHandler;
import com.nukkitx.proxypass.network.bedrock.session.ProxyPlayerSession;

import io.netty.util.ResourceLeakDetector;
import it.unimi.dsi.fastutil.longs.LongList;
import jakarta.xml.bind.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.Collections;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Getter
public class ProxyPass {
    // Not an actual Queue because it's emptied all at once
    // Used for pakkit
    public static ArrayList<JsonPacketData> packetQueue = new ArrayList<>();

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    public static final YAMLMapper YAML_MAPPER = (YAMLMapper) new YAMLMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    public static final String MINECRAFT_VERSION;
    public static final BedrockPacketCodec CODEC = Bedrock_v388.V388_CODEC;
    public static final int PROTOCOL_VERSION = CODEC.getProtocolVersion();
    private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter();

    static {
        DefaultIndenter indenter = new DefaultIndenter("    ", "\n");
        PRETTY_PRINTER.indentArraysWith(indenter);
        PRETTY_PRINTER.indentObjectsWith(indenter);
        String minecraftVersion;

        Package mainPackage = ProxyPass.class.getPackage();
        try {
            minecraftVersion = mainPackage.getImplementationVersion().split("-")[0];
        } catch (NullPointerException e) {
            minecraftVersion = "0.0.0";
        }
        MINECRAFT_VERSION = minecraftVersion;
    }

    private final AtomicBoolean running = new AtomicBoolean(true);
    private BedrockServer bedrockServer;
    private final Set<BedrockClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private InetSocketAddress targetAddress;
    private InetSocketAddress proxyAddress;
    private Configuration configuration;
    private Path baseDir;
    private Path sessionsDir;
    private Path dataDir;

    public static String marshall(Object packet){
        StringWriter writer = new StringWriter();
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(packet.getClass());
            Marshaller m = context.createMarshaller();

            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JAXBElement rootElement = new JAXBElement(new QName(packet.getClass().getSimpleName()), packet.getClass(), packet);
            m.marshal(rootElement, writer);
            return writer.toString();
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object unMarshall(String input, Class type){
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(type);
            Unmarshaller m = context.createUnmarshaller();
            StreamSource source = new StreamSource(new StringReader(input));
            return m.unmarshal(source, type).getValue();
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean testPacket(BedrockPacket packet) {
        /* String text = marshall(packet);
        BedrockPacket reserialized = (BedrockPacket) unMarshall(text, packet.getClass());

        boolean isEqual = packet.equals(reserialized);

        if (!isEqual) {
            System.out.println("--------------------------------------------");
            System.out.println("Packets not equal!");
            System.out.println(packet);
            System.out.println(reserialized);
        }

        return isEqual; */

        return false;
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

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        ProxyPass proxy = new ProxyPass();
        try {
            proxy.boot();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void startFromArgs(String proxyHost, int proxyPort, String destinationHost, int destinationPort,
                                     int maxClients, boolean usePacketQueue) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        ProxyPass proxy = new ProxyPass();
        try {
            proxy.bootFromArgs(proxyHost, proxyPort, destinationHost, destinationPort, maxClients, usePacketQueue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bootFromArgs(String proxyHost, int proxyPort, String destinationHost, int destinationPort,
                             int maxClients, boolean usePacketQueue) throws IOException  {
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

        baseDir = Paths.get(".").toAbsolutePath();
        sessionsDir = baseDir.resolve("sessions");
        dataDir = baseDir.resolve("data");
        Files.createDirectories(sessionsDir);
        Files.createDirectories(dataDir);

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
        if (running.compareAndSet(false, true)) {
            synchronized (this) {
                this.notify();
            }
        }
    }

    public void saveNBT(String dataName, Tag<?> dataTag) {
        Path path = dataDir.resolve(dataName + ".dat");
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             NBTOutputStream nbtOutputStream = NbtUtils.createNetworkWriter(outputStream)){
            nbtOutputStream.write(dataTag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Tag<?> loadNBT(String dataName) {
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
}
