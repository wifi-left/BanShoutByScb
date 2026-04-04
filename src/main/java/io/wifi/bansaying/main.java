package io.wifi.bansaying;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

public class main implements ModInitializer {
    // 定义一个键绑定
    // ## 0 for nothing; 1 ban other team; 2 ban own team; 3 all banned; 4 ban
    // 命令：/tshout
    public static final Permission perm_2 = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    public static Objective ModObj = null;
    public static Logger LOGGER = LoggerFactory.getLogger("MuteByCmd");

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("sshout").executes(context -> {
                context.getSource()
                        .sendSuccess(() -> Component.literal("Usage: /sshout <Contents>").withStyle(ChatFormatting.RED), false);
                return 1;
            }).then(Commands.argument("content", StringArgumentType.greedyString()).executes((ctx) -> {
                String content = StringArgumentType.getString(ctx, "content");
                var source = ctx.getSource();
                if (source.permissions().hasPermission(perm_2) || source.getPlayer() == null) {
                    sendChatMessageToAll(source, content, true);
                } else {

                    ServerPlayer player = source.getPlayer();
                    PlayerTeam team = player.getTeam();
                    String playerTeam = "#";
                    if (team != null) {
                        playerTeam = team.getName();
                    }
                    int chatType = 0;
                    try {
                        chatType = player.level().getScoreboard()
                                .getPlayerScoreInfo(ScoreHolder.forNameOnly(playerTeam), ModObj).value();
                    } catch (Exception e) {
                        chatType = 0;
                    }
                    // (playerTeam, ModObj).getScore();

                    if ((chatType & 4) != 0) {
                        // 禁止 shout
                        source.sendFailure(
                                Component.literal("You cannot shout right now.").withStyle(ChatFormatting.RED));
                        return 1;
                    }
                    sendChatMessageToAll(source, content);

                }
                return 0;
            })));

        });

    }

    private static void sendChatMessageToAll(CommandSourceStack  source, String content) {
        sendChatMessageToAll(source, content, false);
    }

    private static void sendChatMessageToAll(CommandSourceStack  source, String content, boolean op) {
        ServerPlayer player = source.getPlayer();
        MutableComponent text = Component.literal("");
        if (player == null) {
            text = (Component.literal("[SHOUT][OP] ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("CONSOLES").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(content).withStyle(ChatFormatting.WHITE));
        } else {
            text = (Component.literal("[SHOUT]" + (op ? "[OP]" : "") + " ").withStyle(ChatFormatting.GOLD))
                    .append(player.getDisplayName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(content).withStyle(ChatFormatting.WHITE));
        }
        source.getServer().getPlayerList().broadcastSystemMessage(text, false);
    }

    @Override
    public void onInitialize() {
        // 注册命令绑定
        registerCommands();
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            try {
                ModObj = server.getScoreboard().addObjective("BAMBOO_MOD_SAYING", ObjectiveCriteria.DUMMY,
                        Component.literal("[*MOD: Scoreboard Saying Control]"), ObjectiveCriteria.RenderType.INTEGER,
                        false, null);
            } catch (IllegalArgumentException e) {
                // 已存在
                ModObj = server.getScoreboard().getObjective("BAMBOO_MOD_SAYING");
                if (ModObj == null) {
                    LOGGER.error("Can't create or find objective called 'BAMBOO_MOD_SAYING'!");
                }
            }
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // System.out.println(message.getSignedContent()+" by
            // "+sender.getName().getString());
            PlayerTeam team = sender.getTeam();
            String playerTeam = "#";
            if (team != null) {
                playerTeam = team.getName();
            }
            int chatType = 0;
            try {
                chatType = sender.level().getScoreboard().getPlayerScoreInfo( ScoreHolder.forNameOnly(playerTeam), ModObj)
                        .value();
            } catch (Exception e) {
                chatType = 0;
            }
            // ## 0 for nothing; 1 ban own team; 2 ban other team; 3 all banned; 4 ban shout

            if (chatType == 0 || chatType == 4) {
                return true;
            } else {
                if (chatType == 3) {
                    sender.sendSystemMessage(
                            Component.literal("You cannot speak right now. \nTry command instead: /sshout <Content>")
                                    .withStyle(ChatFormatting.RED));
                } else if (chatType == 2 || chatType == 6) {
                    LOGGER.info(
                            "[TEAM_ONLY] " + sender.getDisplayName().getString() + ": " + message.signedContent());
                    sendMessageToOwnTeam(message, sender, playerTeam);
                } else if (chatType == 1 || chatType == 5) {
                    if (team == null) {
                        sendMessageToOtherTeam(message, sender, playerTeam, Component.literal("NORMAL"));
                        LOGGER.info("[IN_TEAM_ONLY] " + sender.getDisplayName().getString() + ": "
                                + message.signedContent());
                    } else {
                        PlayerTeam team1 = team;
                        sendMessageToOtherTeam(message, sender, playerTeam, team1.getDisplayName());
                        LOGGER.info("[OTHER_TEAM_ONLY] " + sender.getDisplayName().getString() + ": "
                                + message.signedContent());

                    }

                } else if (chatType == 7) {
                    sender.sendSystemMessage(Component.literal("You cannot speak or shout right now.").withStyle(ChatFormatting.RED));
                }
                return false;
            }
        });
    }

    private void sendMessageToOtherTeam(PlayerChatMessage message, ServerPlayer sender, String playerTeam,
            Component teamDisplay) {
        MutableComponent raw_message = Component.literal("").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("[").withStyle(ChatFormatting.GRAY).append(teamDisplay)
                        .withStyle(ChatFormatting.WHITE).append("]").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(sender.getDisplayName()).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(((MutableComponent) message.decoratedContent()).withStyle(ChatFormatting.WHITE));
        sender.sendSystemMessage(raw_message.append(Component.literal("\nThis message can't be viewed by your teammates!")
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC)));
        sender.level().getServer().getPlayerList().getPlayers().forEach((player) -> {
            PlayerTeam scoreTeam = player.getTeam();
            String teamName = "#";
            if (scoreTeam != null)
                teamName = scoreTeam.getName();
            if (teamName != playerTeam) {
                player.sendSystemMessage(raw_message, false);
            }
        });
    }

    private void sendMessageToOwnTeam(PlayerChatMessage message, ServerPlayer sender, String playerTeam) {
        Component raw_message = Component.literal("").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("[TEAM]").withStyle(ChatFormatting.GRAY)).append(Component.literal(" "))
                .append(sender.getDisplayName()).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append((message.decoratedContent().copy()).withStyle(ChatFormatting.WHITE));
        sender.level().getServer().getPlayerList().getPlayers().forEach((player) -> {
            PlayerTeam scoreTeam = player.getTeam();
            String teamName = "#";
            if (scoreTeam != null)
                teamName = scoreTeam.getName();
            if (teamName == playerTeam) {
                player.sendSystemMessage(raw_message, false);
            }
        });
    }
}