package dev.g_ab.neovelocity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

public class NeoVelocityConfig {
    public static final NeoVelocityConfig.Common COMMON;
    static final ModConfigSpec commonSpec;

    static {
        final Pair<NeoVelocityConfig.Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(NeoVelocityConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

//    public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
//        COMMON.updateSecretFromConfig();
//    }
//
//    public static void onFileLoad(final ModConfigEvent.Loading configEvent) {
//        COMMON.updateSecretFromConfig();
//    }

    public static void register(IEventBus bus, ModContainer mod) {
        bus.addListener(ModConfigEvent.Loading.class, loading -> COMMON.updateSecretFromConfig());
        bus.addListener(ModConfigEvent.Reloading.class, loading -> COMMON.updateSecretFromConfig());
//        bus.addListener(NeoVelocityConfig::onFileChange);
//        bus.addListener(NeoVelocityConfig::onFileLoad);
        mod.registerConfig(ModConfig.Type.COMMON, NeoVelocityConfig.commonSpec);
    }

    public enum SecretType {
        IN_LINE,
        FILE
    }

    public static class Common {
        private static final String PLACEHOLDER = "<YOUR_SECRET_HERE>";
        public final ModConfigSpec.BooleanValue LOGIN_CUSTOM_PACKET_CATCHALL;
        private final ModConfigSpec.ConfigValue<String> SECRET;
        private final ModConfigSpec.EnumValue<SecretType> TYPE;
        public boolean secretValid = false;
        public byte[] secret = new byte[0];

        Common(ModConfigSpec.Builder builder) {
            builder
                .comment("""
                    The forwarding secret is used to authenticate with your Velocity proxy.
                    Configuration for the forwarding secret:
                      - IN_LINE: Use the secret value directly.
                      - FILE   : Load secret from a UTF-8 encoded file, value is a path relative to run directory.""");
            builder.push("forwarding");
            SECRET = builder.define("forwarding-secret", PLACEHOLDER);
            TYPE = builder.defineEnum("forwarding-secret-type", SecretType.IN_LINE);
            builder.pop();

            builder.push("compatibility");
            LOGIN_CUSTOM_PACKET_CATCHALL = builder
                .comment("""
                    Configuration for login-custom-packet-catchall:
                      - true : NeoVelocity will treat all login packets as proxy authentication packets (recommended).
                      - false: Only packets signed with your secret are considered proxy authentication packets.
                               This allows unconventional login‑phase mods to work, but if your secret is wrong,
                               the server won’t show a clear error.
                    If you see `Took too long to log in` errors, try setting it to `false` to see if it fixes the issue.""")
                .define("login-custom-packet-catchall", true);
            builder.pop();
        }

        private void updateSecretFromConfig() {
            secretValid = false;
            Arrays.fill(secret, (byte) 0);

            SecretType type = TYPE.get();
            String raw = SECRET.get();

            if (raw.equals(PLACEHOLDER)) {
                NeoVelocity.getLogger().warn("Config key 'forwarding-secret' is still set to the placeholder default ({}) – please replace it with your actual Velocity forwarding secret.", PLACEHOLDER);
                return;
            } else if (raw.isEmpty()) {
                NeoVelocity.getLogger().warn("Config key 'forwarding-secret' is empty – you must provide your Velocity forwarding secret in the config before running.");
                return;
            }

            if (type == SecretType.FILE) {
                try {
                    Path path = Path.of(raw);
                    if (Files.exists(path)) {
                        String content = String.join("", Files.readAllLines(path));
                        if (!content.isEmpty()) {
                            this.secret = content.getBytes(StandardCharsets.UTF_8);
                            this.secretValid = true;
                            NeoVelocity.getLogger().info("Loaded secret from file {}", path);
                        } else {
                            NeoVelocity.getLogger().warn("Secret file {} was empty!", path);
                        }
                    } else NeoVelocity.getLogger().warn("The secret file at {} , is not present!", path);
                } catch (InvalidPathException | IOException e) {
                    NeoVelocity.getLogger().warn("Could not load secret from file {}: {}", raw, e.getMessage());
                }
            } else {
                this.secret = raw.getBytes(StandardCharsets.UTF_8);
                this.secretValid = true;
                NeoVelocity.getLogger().info("Loaded inline secret from config.");
            }
        }
    }
}
