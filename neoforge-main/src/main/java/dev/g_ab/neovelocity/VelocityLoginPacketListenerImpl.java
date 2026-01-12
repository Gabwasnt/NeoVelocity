package dev.g_ab.neovelocity;

import dev.g_ab.neovelocity.mixin.ConnectionAccessorMixin;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.Identifier;
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
        if (!NeoVelocityConfig.COMMON.secretValid) {
            this.disconnect(Component.literal("Invalid server secret configuration."));
            NeoVelocity.getLogger().warn("Invalid secret; failing integrity check.");
            return;
        }
        if (velocityLoginMessageId > 0 && packet.transactionId() == velocityLoginMessageId) {
            if (packet.payload() instanceof VelocityProxy.QueryAnswerPayload(FriendlyByteBuf buffer)) {
                if (!VelocityProxy.checkIntegrity(buffer)) {
                    this.disconnect(Component.literal("Unable to verify player details."));
                    NeoVelocity.getLogger().warn("Integrity check failed for {} (Invalid secret)", this.connection.getRemoteAddress());
                    return;
                }

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

                NeoVelocity.getLogger().info("Authenticated {} ({}) via Velocity proxy", this.authenticatedProfile.name(), this.authenticatedProfile.id());
            } else {
                StringBuilder modDump = new StringBuilder("Mod List:\n\tName Version (Mod Id)");
                ModList.get().getMods().forEach(mod -> modDump.append("\n\t").append(mod.getDisplayName()).append(" ").append(mod.getVersion().toString()).append(" (").append(mod.getModId()).append(")"));
                if (NeoVelocityConfig.COMMON.LOGIN_CUSTOM_PACKET_CATCHALL.get()) {
                    this.disconnect(Component.literal("Incompatible mod detected during login.\nThis is a server-side issue. Please contact an administrator."));
                    NeoVelocity.getLogger().error("Velocity authentication packets were modified unexpectedly. This is likely caused by an incompatible mod interfering with the login process. Please report this issue at https://github.com/Gabwasnt/NeoVelocity and include the following:\n{}", modDump);
                } else {
                    this.disconnect(Component.literal("Unable to verify player details.\nor\nIncompatible mod detected during login.\nThis is a server-side issue. Please contact an administrator."));
                    NeoVelocity.getLogger().error("Integrity check failed for {} (Invalid secret) or Velocity authentication packets were modified unexpectedly. This is likely caused by an incompatible mod interfering with the login process. Please report this issue at https://github.com/Gabwasnt/NeoVelocity and include the following:\n{}", this.connection.getRemoteAddress(), modDump);
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

            @SuppressWarnings("unchecked") Map<Integer, Identifier> channels = (Map<Integer, Identifier>) channelsField.get(addon);
            channels.remove(velocityLoginMessageId);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException |
                 InvocationTargetException e) {
            this.disconnect(Component.literal("Server encountered an error applying the Fabric Networking API workaround.\nThis is a server-side issue. Please contact an administrator."));
            NeoVelocity.getLogger().error("Server-side compatibility workaround for Fabric Networking API v1 failed.", e);
        }
    }
}
