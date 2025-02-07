package dev.g_ab.neovelocity;

import dev.g_ab.neovelocity.mixin.ConnectionAccessor;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

public class VelocityLoginPacketListerImpl extends ServerLoginPacketListenerImpl {
    private static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");

    public VelocityLoginPacketListerImpl(MinecraftServer server, Connection connection, boolean transferred) {
        super(server, connection, transferred);
    }

    @Override
    public void handleHello(ServerboundHelloPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet", new Object[0]);
        Validate.validState(StringUtil.isValidPlayerName(pPacket.name()), "Invalid characters in username", new Object[0]);

        int velocityLoginMessageId = ThreadLocalRandom.current().nextInt();
        ClientboundCustomQueryPacket packet = new ClientboundCustomQueryPacket(velocityLoginMessageId, new VelocityProxy.VersionPayload(VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION));
        this.connection.send(packet);
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet) {
        if (packet.transactionId() != packet.transactionId()) this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
        try {
            VelocityProxy.QueryAnswerPayload payload = (VelocityProxy.QueryAnswerPayload) packet.payload();
            if (payload == null) {
                this.disconnect(Component.literal("This server requires you to connect with Velocity."));
                return;
            }

            FriendlyByteBuf buf = payload.buffer();
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
            ((ConnectionAccessor) this.connection).neovelocity$setAddress(address);

            startClientVerification(VelocityProxy.createProfile(buf));

            NeoVelocity.getLogger().info("UUID of player {} is {}", this.authenticatedProfile.getName(), this.authenticatedProfile.getId());

        } catch (ClassCastException exception) {
            this.disconnect(Component.literal("Velocity Forwarding error pls report to sever admins"));
            NeoVelocity.LOGGER.error("Error from casting packet", exception);
        }
    }
}
