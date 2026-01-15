package dev.g_ab.neovelocity.mixin;

import dev.g_ab.neovelocity.NeoVelocityConfig;
import dev.g_ab.neovelocity.VelocityProxy;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin {
    @Shadow
    @Final
    private static int MAX_PAYLOAD_SIZE;

    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true, order = 999)
    private static void readPayload(int pTransactionId, FriendlyByteBuf pBuffer, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        pBuffer.markReaderIndex();

        FriendlyByteBuf payload = neovelocity$readNullablePayload(pBuffer);

        boolean isConfigCatchAll = NeoVelocityConfig.COMMON.LOGIN_CUSTOM_PACKET_CATCHALL.get();
        boolean shouldCatch;

        if (!isConfigCatchAll && payload != null && VelocityProxy.checkIntegrity(payload)) {
            shouldCatch = true;
            payload.resetReaderIndex();
        } else shouldCatch = isConfigCatchAll;

        if (shouldCatch) {
            cir.setReturnValue(payload == null ? null : new VelocityProxy.QueryAnswerPayload(payload));
            cir.cancel();
        } else {
            pBuffer.resetReaderIndex();
        }
    }

    @Unique
    private static FriendlyByteBuf neovelocity$readNullablePayload(FriendlyByteBuf buffer) {
        return buffer.readNullable((buf2) -> {
            int i = buf2.readableBytes();
            if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
                ByteBuf byteBuf = buf2.readBytes(i);
                return new FriendlyByteBuf(byteBuf);
            } else {
                throw new IllegalArgumentException("Payload may not be larger than " + MAX_PAYLOAD_SIZE + " bytes");
            }
        });
    }
}
