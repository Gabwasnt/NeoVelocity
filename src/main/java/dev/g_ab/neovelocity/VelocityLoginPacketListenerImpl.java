package dev.g_ab.neovelocity;

import dev.g_ab.neovelocity.mixin.ConnectionAccessorMixin;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
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
import java.util.UUID;
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
        // 1. Check for valid local configuration
        if (!NeoVelocityConfig.COMMON.secretValid) {
            this.disconnect(Component.literal("NeoVelocity configuration error. Check server logs."));
            NeoVelocity.getLogger().error("Login rejected: The forwarding secret has not been configured in 'config/neovelocity-common.toml'.");
            return;
        }

        if (velocityLoginMessageId > 0 && packet.transactionId() == velocityLoginMessageId) {
            // 2. Check for Packet Loop-back (Empty Payload)
            // This happens if:
            // A. The player connected directly (bypassing Velocity).
            // B. The player connected via Velocity, but Velocity is set to 'player-info-forwarding-mode = "none"' or "legacy".
            if (packet.payload() == null) {
                this.disconnect(Component.literal("This server requires you to connect via a Velocity Proxy using Modern Forwarding."));
                NeoVelocity.getLogger().warn("Connection rejected from {}: Received empty forwarding payload.", this.connection.getRemoteAddress());
            } else if (packet.payload() instanceof VelocityProxy.QueryAnswerPayload(FriendlyByteBuf buffer)) {
                // 3. Check Integrity (HMAC Signature)
                if (!VelocityProxy.checkIntegrity(buffer)) {
                    this.disconnect(Component.literal("Unable to verify proxy data integrity. Check server logs."));
                    NeoVelocity.getLogger().error("Integrity check failed for {}. The forwarding secret in 'neovelocity-common.toml' likely does not match the secret in the Velocity Proxy configuration.", this.connection.getRemoteAddress());
                    return;
                }

                // 4. Version Compatibility
                int version = buffer.readVarInt();
                if (version > VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION) {
                    this.disconnect(Component.literal(String.format("Unsupported forwarding version %d, supported up to %d", version, VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION)));
                    return;
                }

                SocketAddress listening = this.connection.getRemoteAddress();
                int port = 0;
                if (listening instanceof InetSocketAddress) {
                    port = ((InetSocketAddress) listening).getPort();
                }

                InetSocketAddress address = new InetSocketAddress(VelocityProxy.readAddress(buffer), port);
                ((ConnectionAccessorMixin) this.connection).neovelocity$setAddress(address);

                if (ModList.get().isLoaded("fabric_networking_api_v1")) fixFabricNetworkingIssue();

                this.authenticatedProfile = VelocityProxy.createProfile(buffer);
                this.state = ServerLoginPacketListenerImpl.State.VERIFYING;
                String name;
                UUID id;
                //? if >=1.21.9 {
                name = this.authenticatedProfile.name();
                id = this.authenticatedProfile.id();
                //?} else {
                //name = this.authenticatedProfile.getName();
                //id = this.authenticatedProfile.getId();
                //?}

                NeoVelocity.getLogger().info("Authenticated {} ({}) via Velocity proxy", name, id);
            } else {
                // 5. Catch-All / Unknown Payload / Mod Interference
                this.disconnect(Component.literal("Incompatible mod detected during login handshake. Check server logs."));

                if (NeoVelocityConfig.COMMON.LOGIN_CUSTOM_PACKET_CATCHALL.get()) {
                    NeoVelocity.getLogger().error("Login failed: The Velocity authentication packet was modified unexpectedly.");
                } else {
                    NeoVelocity.getLogger().error("Login failed: Integrity check failed OR the packet was modified unexpectedly.");
                }
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
            //? if >=1.21.11 {
            @SuppressWarnings("unchecked") Map<Integer, net.minecraft.resources.Identifier> channels = (Map<Integer, net.minecraft.resources.Identifier>) channelsField.get(addon);
            //?} else {
            //@SuppressWarnings("unchecked") Map<Integer, net.minecraft.resources.ResourceLocation> channels = (Map<Integer, net.minecraft.resources.ResourceLocation>) channelsField.get(addon);
            //?}

            channels.remove(velocityLoginMessageId);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException |
                 InvocationTargetException e) {
            this.disconnect(Component.literal("Internal server error during login (Fabric Networking API)."));
            NeoVelocity.getLogger().error("Server-side compatibility workaround for Fabric Networking API v1 failed.", e);
        }
    }
}
