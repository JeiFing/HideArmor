package cn.jeifing.hidearmor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

final class ArmorCommand implements TabExecutor {
    static final String USAGE_TEXT = "用法：/hide <all none 四位0/1>";

    private static final Component PLAYER_ONLY_MESSAGE = Component.text(
            "这个命令只能由玩家使用。", NamedTextColor.RED
    );
    private static final Component USAGE_MESSAGE = Component.text(
            USAGE_TEXT, NamedTextColor.RED
    );
    private static final List<String> COMPLETIONS = List.of(
            "all", "none",
            "0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111",
            "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111"
    );

    private final ArmorVisibilityManager visibilityManager;

    ArmorCommand(ArmorVisibilityManager visibilityManager) {
        this.visibilityManager = visibilityManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY_MESSAGE);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(USAGE_MESSAGE);
            return true;
        }

        ArmorVisibilitySetting setting = ArmorVisibilitySetting.fromCommandArgument(args[0]);
        if (setting == null) {
            sender.sendMessage(USAGE_MESSAGE);
            return true;
        }

        visibilityManager.setVisibility(player, setting);
        player.sendMessage(Component.text(setting.successText(), NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return COMPLETIONS.stream()
                .filter(completion -> completion.startsWith(prefix))
                .toList();
    }
}
