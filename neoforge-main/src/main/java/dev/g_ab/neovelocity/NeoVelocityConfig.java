package dev.g_ab.neovelocity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NeoVelocityConfig {
    public static final NeoVelocityConfig.Common COMMON;
    static final ModConfigSpec commonSpec;


    static {
        final Pair<NeoVelocityConfig.Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoVelocityConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
        NeoVelocity.getLogger().debug("Detected new secret!");
        if (COMMON.SECRET.get().endsWith(".secret")) {
            Path path = FMLPaths.CONFIGDIR.get().resolve(COMMON.SECRET.get());
            if (!path.toFile().exists()) {
                NeoVelocity.getLogger().warn("The secret file {}, is not present!", path);
            } else try {
                COMMON.secret = Files.readAllBytes(path);
            } catch (IOException e) {
                COMMON.secret = null;
            }
        } else COMMON.secret = COMMON.SECRET.get().getBytes(StandardCharsets.UTF_8);
    }

    public static void register(IEventBus bus, ModContainer mod) {
        bus.addListener(NeoVelocityConfig::onFileChange);
        mod.registerConfig(ModConfig.Type.COMMON, NeoVelocityConfig.commonSpec);
    }

    public static class Common {
        private ModConfigSpec.ConfigValue<String> SECRET;
        public byte @Nullable [] secret;

        Common(ModConfigSpec.Builder builder) {
            SECRET = builder
                    .comment("The Forwarding secret of your velocity proxy, if the provided string ends with `.secret`, it considers it a file.")
                    .define("forwarding-secret", "forwarding.secret");
        }
    }
}
