package io.wifi.bansaying;
// MyMod.java

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.tinyremapper.extension.mixin.common.Logger;
import net.fabricmc.tinyremapper.extension.mixin.common.Logger.Level;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;

import static net.minecraft.server.command.CommandManager.argument;

public class main implements ModInitializer {
    // 定义一个键绑定
    // ## 0 for nothing; 1 ban other team; 2 ban own team; 3 all banned; 4 ban
    // 命令：/tshout
    public static ScoreboardObjective ModObj = null;
    Logger LOGGER = new Logger(Level.INFO);

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("sshout").executes(context -> {
                context.getSource()
                        .sendFeedback(() -> Text.literal("Usage: /sshout <Contents>").formatted(Formatting.RED), false);
                return 1;
            }).then(argument("content", StringArgumentType.greedyString()).executes((ctx) -> {
                String content = StringArgumentType.getString(ctx, "content");
                ServerCommandSource source = ctx.getSource();
                if (source.hasPermissionLevel(2) || source.getPlayer() == null) {
                    sendChatMessageToAll(source, content);
                } else {
                    ServerPlayerEntity player = source.getPlayer();
                    AbstractTeam team = player.getScoreboardTeam();
                    String playerTeam = "#";
                    if (team != null) {
                        playerTeam = team.getName();
                    }
                    int chatType = player.getScoreboard().getPlayerScore(playerTeam, ModObj).getScore();
                    if ((chatType | 4) != 0) {
                        // 禁止 shout
                        source.sendFeedback(
                                () -> Text.literal("You cannot shout right now.").formatted(Formatting.RED),
                                false);
                        return 1;
                    }

                    sendChatMessageToAll(source, content);
                }
                return 0;
            })));
        });

    }

    private static void sendChatMessageToAll(ServerCommandSource source, String content) {
        ServerPlayerEntity player = source.getPlayer();
        MutableText text = Text.literal("");
        if (player == null) {
            text = text.append(Text.literal("[SHOUT][OP] ").formatted(Formatting.GOLD))
                    .append(Text.literal("CONSOLES").formatted(Formatting.GREEN))
                    .append(Text.literal(": ").formatted(Formatting.GRAY))
                    .append(Text.literal(content).formatted(Formatting.WHITE));
        } else {
            text = text.append(Text.literal("[SHOUT] ").formatted(Formatting.GOLD))
                    .append(player.getDisplayName())
                    .append(Text.literal(": ").formatted(Formatting.GRAY))
                    .append(Text.literal(content).formatted(Formatting.WHITE));
        }
        source.getServer().getPlayerManager().broadcast(text, false);
    }

    @Override
    public void onInitialize() {
        // 注册命令绑定
        registerCommands();
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            try {
                ModObj = server.getScoreboard().addObjective("BAMBOO_MOD_SAYING", ScoreboardCriterion.DUMMY,
                        Text.literal("[*MOD: Scoreboard Saying Control]"), ScoreboardCriterion.RenderType.INTEGER);
            } catch (IllegalArgumentException e) {
                // 已存在
                ModObj = server.getScoreboard().getNullableObjective("BAMBOO_MOD_SAYING");
                if (ModObj == null) {
                    LOGGER.error("Can't create or find objective called 'BAMBOO_MOD_SAYING'!");
                }
            }
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // System.out.println(message.getSignedContent()+" by
            // "+sender.getName().getString());
            AbstractTeam team = sender.getScoreboardTeam();
            String playerTeam = "#";
            if (team != null) {
                playerTeam = team.getName();
            }
            int chatType = sender.getScoreboard().getPlayerScore(playerTeam, ModObj).getScore();
            // ## 0 for nothing; 1 ban other team; 2 ban own team; 3 all banned; 4 ban shout

            if (chatType == 0) {
                return true;
            } else {
                if (chatType == 3) {
                    sender.sendMessage(Text.literal("You cannot speak right now. \nTry command instead: /sshout <Content>").formatted(Formatting.RED));
                } else if (chatType == 2) {
                    sendMessageToOwnTeam(message, sender, playerTeam);
                } else if (chatType == 1) {
                    if (team == null)
                        sendMessageToOtherTeam(message, sender, playerTeam, Text.literal("NORMAL"));
                    else {
                        Team team1 = (Team) team;
                        sendMessageToOtherTeam(message, sender, playerTeam, team1.getDisplayName());
                    }

                } else if (chatType == 4){
                    sender.sendMessage(Text.literal("You cannot speak or shout right now.").formatted(Formatting.RED));
                }
                return false;
            }
        });
    }

    private void sendMessageToOtherTeam(SignedMessage message, ServerPlayerEntity sender, String playerTeam,
            Text teamDisplay) {
        MutableText raw_message = Text.literal("").formatted(Formatting.WHITE)
                .append(Text.literal("[").formatted(Formatting.GRAY).append(teamDisplay)
                        .formatted(Formatting.WHITE).append("]").formatted(Formatting.GRAY))
                .append(Text.literal(" "))
                .append(sender.getDisplayName()).append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(((MutableText) message.getContent()).formatted(Formatting.WHITE));
        sender.sendMessage(raw_message.append(Text.literal("\nThis message can't be viewed by your teammates!")
                .formatted(Formatting.DARK_GRAY).formatted(Formatting.ITALIC)));
        sender.getServer().getPlayerManager().getPlayerList().forEach((player) -> {
            if (player.getScoreboardTeam().getName() != playerTeam) {
                player.sendMessage(raw_message, false);
            }
        });
    }

    private void sendMessageToOwnTeam(SignedMessage message, ServerPlayerEntity sender, String playerTeam) {
        Text raw_message = Text.literal("").formatted(Formatting.WHITE)
                .append(Text.literal("[TEAM]").formatted(Formatting.GRAY)).append(Text.literal(" "))
                .append(sender.getDisplayName()).append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(((MutableText) message.getContent()).formatted(Formatting.WHITE));
        sender.getServer().getPlayerManager().getPlayerList().forEach((player) -> {
            if (player.getScoreboardTeam().getName() == playerTeam) {
                player.sendMessage(raw_message, false);
            }
        });
    }
}
