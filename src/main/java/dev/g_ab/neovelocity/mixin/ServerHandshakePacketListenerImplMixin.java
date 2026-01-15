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
     * @reason Inject the Velocity proxy handler
     */
    @Overwrite
    public void beginLogin(ClientIntentionPacket packet, boolean transferred) {
        this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
        int serverProtocol;
        String serverVersion;

        //? if >=1.21.6 {
        serverProtocol = SharedConstants.getCurrentVersion().protocolVersion();
        serverVersion = SharedConstants.getCurrentVersion().name();
        //?} else {
        /*serverProtocol = SharedConstants.getCurrentVersion().getProtocolVersion();
        serverVersion = SharedConstants.getCurrentVersion().getName();*/
        //?}

        if (packet.protocolVersion() != serverProtocol) {
            String key = (packet.protocolVersion() < 754)
                ? "multiplayer.disconnect.outdated_client"
                : "multiplayer.disconnect.incompatible";

            Component component = Component.translatable(key, serverVersion);

            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        } else {
            this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new VelocityLoginPacketListenerImpl(this.server, this.connection, transferred));
        }
    }
}
