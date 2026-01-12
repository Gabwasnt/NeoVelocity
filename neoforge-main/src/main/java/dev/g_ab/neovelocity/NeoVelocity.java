package dev.g_ab.neovelocity;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NeoVelocity.MODID)
public class NeoVelocity {
    public static final String MODID = "neovelocity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoVelocity(IEventBus bus, ModContainer mod) {
        NeoVelocityConfig.register(bus, mod);
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
