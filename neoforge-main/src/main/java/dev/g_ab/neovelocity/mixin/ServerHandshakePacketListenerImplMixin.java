package dev.g_ab.neovelocity.mixin;

import dev.g_ab.neovelocity.VelocityLoginPacketListenerImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {
    @Shadow
    @Final
    private Connection connection;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "handleIntention", at = @At("HEAD"), cancellable = true)
    public void handleIntention(ClientIntentionPacket packet, CallbackInfo ci) {
        if (packet.intention() == ClientIntent.LOGIN) {
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
                this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new VelocityLoginPacketListenerImpl(this.server, this.connection, false));
            }
        } else if (packet.intention() == ClientIntent.TRANSFER) {
            Component component = Component.literal("NUH-uh");
            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        }
        ci.cancel();
    }
}
