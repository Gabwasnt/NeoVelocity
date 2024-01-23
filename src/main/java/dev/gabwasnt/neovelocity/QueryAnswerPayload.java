package dev.gabwasnt.neovelocity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

public class QueryAnswerPayload implements CustomQueryAnswerPayload {
    public final FriendlyByteBuf buffer;

    public QueryAnswerPayload(final net.minecraft.network.FriendlyByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(final net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBytes(this.buffer.copy());
    }
}
