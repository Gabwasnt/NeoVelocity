package dev.gabwasnt.neovelocity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An gabwasnt config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = NeoVelocity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> SECRET = BUILDER
            .comment("The Forwarding secret of your velocity proxy")
            .define("forwarding-secret", "secret!");

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String forwardingSecret;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        forwardingSecret = SECRET.get();
    }
}
