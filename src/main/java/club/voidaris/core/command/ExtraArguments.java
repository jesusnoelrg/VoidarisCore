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
 * Brigadier rejects anything typed after the last node of a command with the client's English "Incorrect argument
 * for command", so {@code /craft a} would fail. Every leaf that runs a command gets a greedy child that swallows the
 * extra text and runs the same command.
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
        if (node.getCommand() == null || !node.getChildren().isEmpty() || isGreedy(node)) {
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
