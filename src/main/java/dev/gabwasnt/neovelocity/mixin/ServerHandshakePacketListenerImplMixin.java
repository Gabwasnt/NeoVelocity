package dev.gabwasnt.neovelocity.mixin;

import dev.gabwasnt.neovelocity.NeoVelocityConfig;
import dev.gabwasnt.neovelocity.VelocityLoginPacketListerImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
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
    public void handleIntention(ClientIntentionPacket pPacket, CallbackInfo ci) {
        if (!NeoVelocityConfig.Server.ENABLED.get()) return;
        if (pPacket.intention() == ClientIntent.LOGIN) {
            this.connection.setClientboundProtocolAfterHandshake(ClientIntent.LOGIN);
            if (pPacket.protocolVersion() != SharedConstants.getCurrentVersion().getProtocolVersion()) {
                Component component;
                if (pPacket.protocolVersion() < 754) {
                    component = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().getName());
                } else {
                    component = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().getName());
                }

                this.connection.send(new ClientboundLoginDisconnectPacket(component));
                this.connection.disconnect(component);
            } else {
                this.connection.setListener(new VelocityLoginPacketListerImpl(this.server, this.connection));
            }
            ci.cancel();
        }
    }
}
