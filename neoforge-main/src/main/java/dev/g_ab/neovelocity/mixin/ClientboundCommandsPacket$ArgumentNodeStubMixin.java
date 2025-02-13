package dev.g_ab.neovelocity.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author G_ab/Gabwasnt
 * @implNote Ported from CrossStitch 1.19.3 to NeoForge 1.21.1
 * @see <a href="https://github.com/VelocityPowered/CrossStitch/blob/master/src/main/java/com/velocitypowered/crossstitch/mixin/command/CommandTreeSerializationMixin.java">com.velocitypowered.crossstitch.mixin.command.CommandTreeSerializationMixin</a>
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public abstract class ClientboundCommandsPacket$ArgumentNodeStubMixin {
    @Unique
    private static final int MOD_ARGUMENT_INDICATOR = -256;

    @Inject(method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V", at = @At("HEAD"), cancellable = true)
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void wrapInVelocityModArgument(FriendlyByteBuf buf, ArgumentTypeInfo<A, T> serializer, ArgumentTypeInfo.Template<A> properties, CallbackInfo ci) {
        ResourceLocation key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(serializer);
        boolean flag = RegistryManager.getVanillaRegistryKeys().contains(key);

        if ((key != null && !flag)) {
            ci.cancel();
            neovelocity$serializeWrappedArgumentType(buf, serializer, properties);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void neovelocity$serializeWrappedArgumentType(FriendlyByteBuf packetByteBuf, ArgumentTypeInfo<A, T> serializer, ArgumentTypeInfo.Template<A> properties) {
        packetByteBuf.writeVarInt(MOD_ARGUMENT_INDICATOR);
        packetByteBuf.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(serializer));

        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        serializer.serializeToNetwork((T) properties, extraData);

        packetByteBuf.writeVarInt(extraData.readableBytes());
        packetByteBuf.writeBytes(extraData);
    }
}
