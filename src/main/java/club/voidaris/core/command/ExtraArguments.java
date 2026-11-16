package club.voidaris.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Brigadier rejects anything typed after the last node of a command, or a subcommand that does not exist, with the
 * client's English "Incorrect argument for command", so {@code /craft a} or {@code /voidaris foo} would fail. Every
 * node that runs a command and has no argument children gets a greedy child that swallows the rest of the input and
 * runs that same command. Literal children still win, because Brigadier only tries arguments when no literal matches.
 */
public final class ExtraArguments {

    private static final String NAME = "args";

    private ExtraArguments() {
    }

    /**
     * Wraps the registrar so every command tree registered through it ignores extra arguments, including commands
     * added in the future, without each module having to remember it.
     */
    @SuppressWarnings("unchecked")
    public static Commands ignoredBy(Commands registrar) {
        return (Commands) Proxy.newProxyInstance(Commands.class.getClassLoader(), new Class<?>[]{Commands.class},
                (proxy, method, args) -> {
                    if (args != null) {
                        for (Object arg : args) {
                            if (arg instanceof LiteralCommandNode<?> node) {
                                allow((CommandNode<CommandSourceStack>) node);
                            }
                        }
                    }
                    try {
                        return method.invoke(registrar, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void allow(CommandNode<CommandSourceStack> node) {
        for (var child : List.copyOf(node.getChildren())) {
            allow(child);
        }
        boolean hasArgumentChild = node.getChildren().stream().anyMatch(child -> child instanceof ArgumentCommandNode);
        if (node.getCommand() == null || hasArgumentChild || isGreedy(node)) {
            return;
        }
        node.addChild(Commands.argument(NAME, StringArgumentType.greedyString())
                .suggests((context, builder) -> builder.buildFuture())
                .executes(node.getCommand())
                .build());
    }

    private static boolean isGreedy(CommandNode<CommandSourceStack> node) {
        return node instanceof ArgumentCommandNode<?, ?> argument
                && argument.getType() instanceof StringArgumentType string
                && string.getType() == StringArgumentType.StringType.GREEDY_PHRASE;
    }
}
