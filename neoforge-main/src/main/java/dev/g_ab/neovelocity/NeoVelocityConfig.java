package dev.g_ab.neovelocity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
        COMMON.updateSecretFromConfig();
    }

    public static void onFileLoad(final ModConfigEvent.Loading configEvent) {
        COMMON.updateSecretFromConfig();
    }

    public static void register(IEventBus bus, ModContainer mod) {
        bus.addListener(NeoVelocityConfig::onFileChange);
        bus.addListener(NeoVelocityConfig::onFileLoad);
        mod.registerConfig(ModConfig.Type.COMMON, NeoVelocityConfig.commonSpec);
    }

    public static class Common {
        private ModConfigSpec.ConfigValue<String> SECRET;
        public byte @Nullable [] secret;

        Common(ModConfigSpec.Builder builder) {
            SECRET = builder
                .comment("The Forwarding secret of your velocity proxy.")
                .comment("If the provided string ends with `.secret`, it is treated as a path to a file in the running directory.")
                .comment("The file is expected to be UTF-8 encoded and not empty.")
                .define("forwarding-secret", "Insert-Here");
        }

        private void updateSecretFromConfig() {
            if (this.SECRET.get().endsWith(".secret")) {
                try {
                    Path path = Path.of(this.SECRET.get());
                    if (!Files.exists(path)) {
                        NeoVelocity.getLogger().warn("The secret file at {} , is not present!", path);
                    } else try {
                        this.secret = String.join("", Files.readAllLines(path)).getBytes(StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        this.secret = null;
                    }
                } catch (InvalidPathException e) {
                    this.secret = null;
                }
            } else this.secret = this.SECRET.get().getBytes(StandardCharsets.UTF_8);
        }
    }
}
