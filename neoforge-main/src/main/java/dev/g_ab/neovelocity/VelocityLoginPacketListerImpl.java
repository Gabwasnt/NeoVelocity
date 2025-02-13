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
import org.apache.commons.lang3.Validate;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

public class VelocityLoginPacketListerImpl extends ServerLoginPacketListenerImpl {
    private static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");
    private int velocityLoginMessageId = -1;

    public VelocityLoginPacketListerImpl(MinecraftServer server, Connection connection, boolean transferred) {
        super(server, connection, transferred);
    }

    @Override
    public void handleHello(ServerboundHelloPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        Validate.validState(StringUtil.isValidPlayerName(pPacket.name()), "Invalid characters in username");

        this.velocityLoginMessageId = ThreadLocalRandom.current().nextInt();
        this.connection.send(new ClientboundCustomQueryPacket(velocityLoginMessageId, new VelocityProxy.VersionPayload(VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION)));
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet) {
        if (packet.transactionId() != velocityLoginMessageId || velocityLoginMessageId == -1) {
            this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
            return;
        }

        try {
            VelocityProxy.QueryAnswerPayload payload = (VelocityProxy.QueryAnswerPayload) packet.payload();
            if (payload == null) {
                this.disconnect(Component.literal("This server requires you to connect with Velocity."));
                NeoVelocity.getLogger().warn("Someone/{} tried to login without proxy details!!!! Ports are exposed, they shouldn't be!!!!!!!", this.connection.getRemoteAddress());
                return;
            }

            FriendlyByteBuf buf = payload.buffer();
            if (!VelocityProxy.checkIntegrity(buf)) {
                this.disconnect(Component.literal("Unable to verify player details."));
                NeoVelocity.getLogger().warn("Someone/{} is trying to login with invalid secrets! Make sure ports are not exposed or that your secrets are corrects on both sides!", this.connection.getRemoteAddress());
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
            ((ConnectionAccessorMixin) this.connection).neovelocity$setAddress(address);

            this.startClientVerification(VelocityProxy.createProfile(buf));

            NeoVelocity.getLogger().info("Player {}({}) authenticated through the Velocity proxy", this.authenticatedProfile.getName(), this.authenticatedProfile.getId());
        } catch (ClassCastException exception) {
            this.disconnect(Component.literal("Velocity Forwarding error pls report to sever admins"));
            NeoVelocity.LOGGER.error("Error from casting packet", exception);
        }
    }
}
