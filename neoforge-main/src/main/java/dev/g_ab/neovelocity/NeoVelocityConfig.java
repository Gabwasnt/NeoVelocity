package dev.g_ab.neovelocity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class NeoVelocityConfig {
    public static final NeoVelocityConfig.Common COMMON;
    static final ModConfigSpec commonSpec;


    static {
        final Pair<NeoVelocityConfig.Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoVelocityConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        NeoVelocity.getLogger().debug("NeoVelocity is {}", COMMON.ENABLED.get() ? "enabled" : "disabled");
    }

    public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
        NeoVelocity.getLogger().debug("NeoVelocity was {}", COMMON.ENABLED.get() ? "enabled" : "disabled");
    }

    public static class Common {
        public ModConfigSpec.ConfigValue<Boolean> ENABLED;
        public ModConfigSpec.ConfigValue<String> SECRET;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Configure the NeoVelocity Server mod");
            ENABLED = builder
                .comment("If the mod is enabled or not")
                .define("enabled", false);
            SECRET = builder
                .comment("The Forwarding secret of your velocity proxy")
                .define("forwarding-secret", "secret!");
        }
    }

    public static void register(IEventBus bus, ModContainer mod) {
        bus.addListener(NeoVelocityConfig::onLoad);
        bus.addListener(NeoVelocityConfig::onFileChange);
        mod.registerConfig(ModConfig.Type.COMMON, NeoVelocityConfig.commonSpec);
    }
}
