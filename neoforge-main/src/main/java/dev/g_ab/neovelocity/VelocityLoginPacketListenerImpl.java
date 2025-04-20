package dev.g_ab.neovelocity;

import dev.g_ab.neovelocity.mixin.ConnectionAccessorMixin;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.StringUtil;
import net.neoforged.fml.ModList;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class VelocityLoginPacketListenerImpl extends ServerLoginPacketListenerImpl {
    private int velocityLoginMessageId = -1;

    public VelocityLoginPacketListenerImpl(MinecraftServer server, Connection connection, boolean transferred) {
        super(server, connection, transferred);
    }

    @Override
    public void handleHello(ServerboundHelloPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        Validate.validState(StringUtil.isValidPlayerName(pPacket.name()), "Invalid characters in username");
        this.velocityLoginMessageId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        this.connection.send(new ClientboundCustomQueryPacket(velocityLoginMessageId, new VelocityProxy.VersionPayload(VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION)));
    }

    @Override
    public void handleCustomQueryPacket(@NotNull ServerboundCustomQueryAnswerPacket packet) {
        if (velocityLoginMessageId > 0 && packet.transactionId() == velocityLoginMessageId) {
            if (!NeoVelocityConfig.COMMON.secretValid) {
                this.disconnect(Component.literal("Invalid server secret configuration."));
                NeoVelocity.getLogger().warn("Invalid secret; failing integrity check.");
                return;
            }

            if (packet.payload() instanceof VelocityProxy.QueryAnswerPayload payload) {
                FriendlyByteBuf buf = payload.buffer();
                if (!VelocityProxy.checkIntegrity(buf)) {
                    this.disconnect(Component.literal("Unable to verify player details."));
                    NeoVelocity.getLogger().warn("Integrity check failed for {}", this.connection.getRemoteAddress());
                    return;
                }

                int version = buf.readVarInt();
                if (version > VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION) {
                    this.disconnect(Component.literal(String.format("Unsupported forwarding version %d, supported up to %d", version, VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION)));
                    return;
                }

                SocketAddress listening = this.connection.getRemoteAddress();
                int port = 0;
                if (listening instanceof InetSocketAddress) {
                    port = ((InetSocketAddress) listening).getPort();
                }

                InetSocketAddress address = new InetSocketAddress(VelocityProxy.readAddress(buf), port);
                ((ConnectionAccessorMixin) this.connection).neovelocity$setAddress(address);

                if (ModList.get().isLoaded("fabric_networking_api_v1")) fixFabricNetworkingIssue();

                this.authenticatedProfile = VelocityProxy.createProfile(buf);
                this.state = ServerLoginPacketListenerImpl.State.VERIFYING;

                NeoVelocity.getLogger().info("Authenticated {} ({}) via Velocity proxy", this.authenticatedProfile.getName(), this.authenticatedProfile.getId());
            } else {
                this.disconnect(Component.literal("A mod on the server is incompatible, check logs!"));
                NeoVelocity.getLogger().error("A mod on the server is changing Velocity authentication packets!");
            }
        } else super.handleCustomQueryPacket(packet);
    }

    private void fixFabricNetworkingIssue() {
        try {
            Class<?> extClass = Class.forName("net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions");
            Method getAddon = extClass.getMethod("getAddon");
            Object addon = getAddon.invoke(this);

            Class<?> addonClass = Class.forName("net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon");
            Field channelsField = addonClass.getDeclaredField("channels");
            channelsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<Integer, ResourceLocation> channels = (Map<Integer, ResourceLocation>) channelsField.get(addon);
            channels.remove(velocityLoginMessageId);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException |
                 InvocationTargetException e) {
            this.disconnect(Component.literal("Connection terminated: server encountered an error applying the Fabric Networking API workaround.\nThis is a server-side issue; please try reconnecting later or contact an administrator."));
            NeoVelocity.getLogger().error("Server-side compatibility workaround for Fabric Networking API v1 failed.", e);
        }
    }
}
