package dev.g_ab.neovelocity.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;


@Mixin({Connection.class})
public interface ConnectionAccessor {
    @Accessor("address")
    void neovelocity$setAddress(SocketAddress address);
}
