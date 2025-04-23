package dev.g_ab.neovelocity.mixin;

import dev.g_ab.neovelocity.VelocityLoginPacketListenerImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {
    @Shadow
    @Final
    private Connection connection;

    @Shadow
    @Final
    private MinecraftServer server;

    /**
     * @author GabWasnt
     * @reason inject the Velocity proxy handler
     */
    @Overwrite
    public void beginLogin(ClientIntentionPacket packet, boolean transferred) {
        this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
        if (packet.protocolVersion() != SharedConstants.getCurrentVersion().getProtocolVersion()) {
            Component component;
            if (packet.protocolVersion() < 754) {
                component = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().getName());
            } else {
                component = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().getName());
            }

            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        } else {
            this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new VelocityLoginPacketListenerImpl(this.server, this.connection, transferred));
        }
    }
}
