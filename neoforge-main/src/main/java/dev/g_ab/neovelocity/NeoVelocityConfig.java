package dev.g_ab.neovelocity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

// An gabwasnt config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
public class NeoVelocityConfig {
    public static final NeoVelocityConfig.Common COMMON;
    static final ModConfigSpec commonSpec;


    static {
        final Pair<NeoVelocityConfig.Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoVelocityConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        //VelocityConfig.getLogger().debug("Loaded Armadeus config file {}", configEvent.getConfig().getFileName());
    }

    public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
        //VelocityConfig.getLogger().debug("Armadeus config just got changed on the file system!");
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
