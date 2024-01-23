package dev.gabwasnt.neovelocity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

// An gabwasnt config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
public class NeoVelocityConfig {
    public static class Server {
        public static ModConfigSpec.ConfigValue<Boolean> ENABLED;
        public static ModConfigSpec.ConfigValue<String> SECRET;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("Configure the NeoVelocity Server mod");
            ENABLED = builder
                    .comment("If the mod is enabled or not")
                    .define("enabled", false);
            SECRET = builder
                    .comment("The Forwarding secret of your velocity proxy")
                    .define("forwarding-secret", "secret!");
        }
    }

    static final ModConfigSpec serverSpec;
    public static final NeoVelocityConfig.Server SERVER;

    static {
        final Pair<NeoVelocityConfig.Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }
}
