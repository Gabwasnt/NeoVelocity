package dev.g_ab.neovelocity.mixin;

import dev.g_ab.neovelocity.NeoVelocityConfig;
import dev.g_ab.neovelocity.VelocityProxy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin {
    @Shadow
    @Final
    private static int MAX_PAYLOAD_SIZE;

    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void readPayload(int pTransactionId, FriendlyByteBuf pBuffer, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        FriendlyByteBuf buffer = pBuffer.readNullable((buf2) -> {
            int i = buf2.readableBytes();
            if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
                return new FriendlyByteBuf(buf2.readBytes(i));
            } else {
                throw new IllegalArgumentException("Payload may not be larger than " + MAX_PAYLOAD_SIZE + " bytes");
            }
        });
        cir.setReturnValue(buffer == null ? null : new VelocityProxy.QueryAnswerPayload(buffer));
        cir.cancel();
    }
}
