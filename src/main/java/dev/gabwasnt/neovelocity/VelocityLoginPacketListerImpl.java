package dev.gabwasnt.neovelocity;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.gabwasnt.neovelocity.mixin.ConnectionAccessor;
import net.minecraft.CrashReportCategory;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.*;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class VelocityLoginPacketListerImpl implements ServerLoginPacketListener, TickablePacketListener {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");
    private final byte[] challenge;
    final MinecraftServer server;
    final Connection connection;
    private volatile State state = State.HELLO;
    /**
     * How long has player been trying to login into the server.
     */
    private int tick;
    @Nullable
    String requestedUsername;
    @Nullable
    public GameProfile authenticatedProfile;
    private final String serverId = "";

    private int velocityLoginMessageId = -1;

    public VelocityLoginPacketListerImpl(MinecraftServer pServer, Connection pConnection) {
        this.server = pServer;
        this.connection = pConnection;
        this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
        this.velocityLoginMessageId = java.util.concurrent.ThreadLocalRandom.current().nextInt();
    }

    @Override
    public void tick() {
        if (this.state == State.VERIFYING) {
            this.verifyLoginAndFinishConnectionSetup(Objects.requireNonNull(this.authenticatedProfile));
        }

        if (this.state == State.WAITING_FOR_DUPE_DISCONNECT
                && !this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile))) {
            this.finishLoginAndWaitForClient(this.authenticatedProfile);
        }

        if (this.tick++ == 1200) {
            this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
        }
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public void disconnect(Component pReason) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getUserName(), pReason.getString());
            this.connection.send(new ClientboundLoginDisconnectPacket(pReason));
            this.connection.disconnect(pReason);
        } catch (Exception exception) {
            LOGGER.error("Error whilst disconnecting player", (Throwable) exception);
        }
    }

    private boolean isPlayerAlreadyInWorld(GameProfile pProfile) {
        return this.server.getPlayerList().getPlayer(pProfile.getId()) != null;
    }

    /**
     * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
     */
    @Override
    public void onDisconnect(Component pReason) {
        LOGGER.info("{} lost connection: {}", this.getUserName(), pReason.getString());
    }

    public String getUserName() {
        String s = this.connection.getLoggableAddress(this.server.logIPs());
        return this.requestedUsername != null ? this.requestedUsername + " (" + s + ")" : s;
    }

    @Override
    public void handleHello(ServerboundHelloPacket pPacket) {
        Validate.validState(this.state == State.HELLO, "Unexpected hello packet");
        Validate.validState(Player.isValidUsername(pPacket.name()), "Invalid characters in username");
        ClientboundCustomQueryPacket packet1 = new ClientboundCustomQueryPacket(this.velocityLoginMessageId, new CustomQueryPayload() {
            @Override
            public @NotNull ResourceLocation id() {
                return VelocityProxy.PLAYER_INFO_CHANNEL;
            }

            @Override
            public void write(@NotNull FriendlyByteBuf pBuffer) {
                pBuffer.writeInt(VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
            }
        });
        this.connection.send(packet1);
        this.requestedUsername = pPacket.name();
    }

    void startClientVerification(GameProfile pAuthenticatedProfile) {
        //this.authenticatedProfile = pAuthenticatedProfile;
        this.state = State.VERIFYING;
    }

    private void verifyLoginAndFinishConnectionSetup(GameProfile pProfile) {
        PlayerList playerlist = this.server.getPlayerList();
        Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), pProfile);
        if (component != null) {
            this.disconnect(component);
        } else {
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection
                        .send(
                                new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()),
                                PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true))
                        );
            }

            boolean flag = playerlist.disconnectAllPlayersWithProfile(pProfile);
            if (flag) {
                this.state = State.WAITING_FOR_DUPE_DISCONNECT;
            } else {
                this.finishLoginAndWaitForClient(pProfile);
            }
        }
    }

    private void finishLoginAndWaitForClient(GameProfile pProfile) {
        this.state = State.PROTOCOL_SWITCHING;
        this.connection.send(new ClientboundGameProfilePacket(pProfile));
    }

    @Override
    public void handleKey(ServerboundKeyPacket pPacket) {
        //this.disconnect(Component.literal("This server requires you to connect with Velocity."));
//
//        final String s;
//        try {
//            PrivateKey privatekey = this.server.getKeyPair().getPrivate();
//            if (!pPacket.isChallengeValid(this.challenge, privatekey)) {
//                throw new IllegalStateException("Protocol error");
//            }
//
//            SecretKey secretkey = pPacket.getSecretKey(privatekey);
//            Cipher cipher = Crypt.getCipher(2, secretkey);
//            Cipher cipher1 = Crypt.getCipher(1, secretkey);
//            s = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretkey)).toString(16);
//            this.state = State.AUTHENTICATING;
//            this.connection.setEncryptionKey(cipher, cipher1);
//        } catch (CryptException cryptexception) {
//            throw new IllegalStateException("Protocol error", cryptexception);
//        }
//
//        Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
//            @Override
//            public void run() {
//                String s1 = Objects.requireNonNull(VelocityLoginPacketListerImpl.this.requestedUsername, "Player name not initialized");
//
//                try {
//                    ProfileResult profileresult = VelocityLoginPacketListerImpl.this.server.getSessionService().hasJoinedServer(s1, s, this.getAddress());
//                    if (profileresult != null) {
//                        GameProfile gameprofile = profileresult.profile();
//                        LOGGER.info("UUID of player {} is {}", gameprofile.getName(), gameprofile.getId());
//                        VelocityLoginPacketListerImpl.this.startClientVerification(gameprofile);
//                    } else if (VelocityLoginPacketListerImpl.this.server.isSingleplayer()) {
//                        LOGGER.warn("Failed to verify username but will let them in anyway!");
//                        VelocityLoginPacketListerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
//                    } else {
//                        VelocityLoginPacketListerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
//                        LOGGER.error("Username '{}' tried to join with an invalid session", s1);
//                    }
//                } catch (AuthenticationUnavailableException authenticationunavailableexception) {
//                    if (VelocityLoginPacketListerImpl.this.server.isSingleplayer()) {
//                        LOGGER.warn("Authentication servers are down but will let them in anyway!");
//                        VelocityLoginPacketListerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
//                    } else {
//                        VelocityLoginPacketListerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
//                        LOGGER.error("Couldn't verify username because servers are unavailable");
//                    }
//                }
//            }
//
//            @Nullable
//            private InetAddress getAddress() {
//                SocketAddress socketaddress = VelocityLoginPacketListerImpl.this.connection.getRemoteAddress();
//                return VelocityLoginPacketListerImpl.this.server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress
//                        ? ((InetSocketAddress) socketaddress).getAddress()
//                        : null;
//            }
//        };
//        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
//        thread.start();
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket pPacket) {
        if (pPacket.transactionId() != velocityLoginMessageId) this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
        try {
            QueryAnswerPayload payload = (QueryAnswerPayload) pPacket.payload();
            if (payload == null) {
                this.disconnect(Component.literal("This server requires you to connect with Velocity."));
                return;
            }

            FriendlyByteBuf buf = payload.buffer;

            if (!VelocityProxy.checkIntegrity(buf)) {
                this.disconnect(Component.literal("Unable to verify player details"));
                return;
            }

            int version = buf.readVarInt();
            if (version > VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION) {
                throw new IllegalStateException("Unsupported forwarding version " + version + ", wanted upto " + VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
            }

            SocketAddress listening = this.connection.getRemoteAddress();
            int port = 0;
            if (listening instanceof InetSocketAddress) {
                port = ((InetSocketAddress) listening).getPort();
            }

            InetSocketAddress address = new InetSocketAddress(VelocityProxy.readAddress(buf), port);
            ((ConnectionAccessor) this.connection).setAddress(address);
            this.authenticatedProfile = VelocityProxy.createProfile(buf);
            startClientVerification(this.authenticatedProfile);
        } catch (ClassCastException exception) {
            this.disconnect(Component.literal("Velocity Forwarding error pls report to sever admins"));
            NeoVelocity.LOGGER.error("Error from casting packet", exception);
        }
    }

    @Override
    public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket pPacket) {
        Validate.validState(this.state == State.PROTOCOL_SWITCHING, "Unexpected login acknowledgement packet");
        CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile));
        ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = new ServerConfigurationPacketListenerImpl(
                this.server, this.connection, commonlistenercookie
        );
        this.connection.setListener(serverconfigurationpacketlistenerimpl);
        serverconfigurationpacketlistenerimpl.startConfiguration();
        this.state = State.ACCEPTED;
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
        pCrashReportCategory.setDetail("Login phase", () -> this.state.toString());
    }


    static enum State {
        HELLO,
        KEY,
        AUTHENTICATING,
        NEGOTIATING,
        VERIFYING,
        WAITING_FOR_DUPE_DISCONNECT,
        PROTOCOL_SWITCHING,
        ACCEPTED;
    }
}