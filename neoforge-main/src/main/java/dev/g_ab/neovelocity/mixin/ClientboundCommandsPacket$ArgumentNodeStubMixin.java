package dev.g_ab.neovelocity.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * @author G_ab/Gabwasnt
 * @implNote Ported from CrossStitch 1.19.3 to NeoForge 1.21.1
 * @see <a href="https://github.com/VelocityPowered/CrossStitch/blob/master/src/main/java/com/velocitypowered/crossstitch/mixin/command/CommandTreeSerializationMixin.java">com.velocitypowered.crossstitch.mixin.command.CommandTreeSerializationMixin</a>
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public abstract class ClientboundCommandsPacket$ArgumentNodeStubMixin {
    @Unique
    private static final int MOD_ARGUMENT_INDICATOR = -256;

    @Unique
    private static final Set<String> neoVelocity$vanillaArguments = Set.of(
        "brigadier:bool",
        "brigadier:float",
        "brigadier:double",
        "brigadier:integer",
        "brigadier:long",
        "brigadier:string",

        "minecraft:entity",
        "minecraft:game_profile",
        "minecraft:block_pos",
        "minecraft:column_pos",
        "minecraft:vec3",
        "minecraft:vec2",
        "minecraft:block_state",
        "minecraft:block_predicate",
        "minecraft:item_stack",
        "minecraft:item_predicate",
        "minecraft:color",
        "minecraft:component",
        "minecraft:style",
        "minecraft:message",
        "minecraft:nbt_compound_tag",
        "minecraft:nbt_tag",
        "minecraft:nbt_path",
        "minecraft:objective",
        "minecraft:objective_criteria",
        "minecraft:operation",
        "minecraft:particle",
        "minecraft:angle",
        "minecraft:rotation",
        "minecraft:scoreboard_slot",
        "minecraft:score_holder",
        "minecraft:swizzle",
        "minecraft:team",
        "minecraft:item_slot",
        "minecraft:item_slots",
        "minecraft:resource_location",
        "minecraft:mob_effect",
        "minecraft:function",
        "minecraft:entity_anchor",
        "minecraft:int_range",
        "minecraft:float_range",
        "minecraft:item_enchantment",
        "minecraft:entity_summon",
        "minecraft:dimension",
        "minecraft:gamemode",

        "minecraft:time",

        "minecraft:resource_or_tag",
        "minecraft:resource_or_tag_key",
        "minecraft:resource",
        "minecraft:resource_key",
        "minecraft:resource_selector",

        "minecraft:template_mirror",
        "minecraft:template_rotation",
        "minecraft:heightmap",

        "minecraft:uuid",

        "minecraft:loot_table",
        "minecraft:loot_predicate",
        "minecraft:loot_modifier"
    );


    @SuppressWarnings("unchecked")
    @Inject(method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V", at = @At("HEAD"), cancellable = true)
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(FriendlyByteBuf buffer, ArgumentTypeInfo<A, T> argumentInfo, ArgumentTypeInfo.Template<A> argumentInfoTemplate, CallbackInfo ci) {
        Identifier key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(argumentInfo);
        if (key == null || !neoVelocity$vanillaArguments.contains(key.toString())) {
            ci.cancel();
            buffer.writeVarInt(MOD_ARGUMENT_INDICATOR);
            buffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(argumentInfo));

            FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
            argumentInfo.serializeToNetwork((T) argumentInfoTemplate, extraData);

            buffer.writeVarInt(extraData.readableBytes());
            buffer.writeBytes(extraData);

            extraData.release();
        }
    }
}
