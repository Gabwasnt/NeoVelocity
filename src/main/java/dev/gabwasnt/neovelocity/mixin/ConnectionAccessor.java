package dev.gabwasnt.neovelocity.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;


@Mixin(Connection.class)
public interface ConnectionAccessor {

    @Accessor("address")
    public void setAddress(SocketAddress address);

}
