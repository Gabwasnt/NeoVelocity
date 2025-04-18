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

    public enum SecretType {
        IN_LINE,
        FILE
    }

    public static class Common {
        private final ModConfigSpec.ConfigValue<String> SECRET;
        private final ModConfigSpec.EnumValue<SecretType> TYPE;
        public byte @Nullable [] secret;

        Common(ModConfigSpec.Builder builder) {
            builder
                .comment("The forwarding secret is used to authenticate with your Velocity proxy.")
                .comment("Configuration for the forwarding secret:")
                .comment("  - IN_LINE: Use the secret value directly.")
                .comment("  - FILE: Load secret from a UTF-8 encoded file, value is a path relative to run directory.");
            SECRET = builder.define("forwarding-secret", "secret!");
            TYPE = builder.defineEnum("forwarding-secret-type", SecretType.IN_LINE);
        }

        private void updateSecretFromConfig() {
            if (TYPE.get() == SecretType.FILE) {
                try {
                    Path path = Path.of(this.SECRET.get());
                    if (!Files.exists(path)) {
                        NeoVelocity.getLogger().warn("The secret file at {} , is not present!", path);
                    } else try {
                        NeoVelocity.getLogger().info("Secret loaded from {}", path);
                        this.secret = String.join("", Files.readAllLines(path)).getBytes(StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        NeoVelocity.getLogger().warn("Error reading {} , make sure it is UTF-8 encoded and not empty!", path);
                        this.secret = null;
                    }
                } catch (InvalidPathException e) {
                    NeoVelocity.getLogger().warn("The provided file path for the secret file is invalid!");
                    this.secret = null;
                }
            } else if (TYPE.get() == SecretType.IN_LINE) {
                NeoVelocity.getLogger().info("Secret loaded from the config.");
                this.secret = this.SECRET.get().getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
