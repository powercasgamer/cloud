//
// MIT License
//
// Copyright (c) 2020 Alexander Söderberg
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package com.intellectualsites.commands.jline;

import com.intellectualsites.commands.CommandManager;
import com.intellectualsites.commands.CommandTree;
import com.intellectualsites.commands.arguments.StaticArgument;
import com.intellectualsites.commands.arguments.parser.ArgumentParseResult;
import com.intellectualsites.commands.exceptions.InvalidSyntaxException;
import com.intellectualsites.commands.exceptions.NoSuchCommandException;
import com.intellectualsites.commands.execution.CommandExecutionCoordinator;
import com.intellectualsites.commands.internal.CommandRegistrationHandler;
import com.intellectualsites.commands.meta.SimpleCommandMeta;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

/**
 * Command manager for use with JLine
 */
public class JLineCommandManager extends CommandManager<JLineCommandSender, SimpleCommandMeta> implements Completer {

    /**
     * Construct a new JLine command manager
     *
     * @param executionCoordinatorFunction Function producing a new coordinator
     */
    public JLineCommandManager(@Nonnull final Function<CommandTree<JLineCommandSender, SimpleCommandMeta>,
            CommandExecutionCoordinator<JLineCommandSender, SimpleCommandMeta>> executionCoordinatorFunction) {
        super(executionCoordinatorFunction, CommandRegistrationHandler.nullCommandRegistrationHandler());
    }

    /**
     * Main method
     *
     * @param args Arguments
     * @throws Exception Any and all exceptions
     */
    public static void main(final String[] args) throws Exception {
        final JLineCommandManager jLineCommandManager = new JLineCommandManager(CommandExecutionCoordinator.simpleCoordinator());
        final Terminal terminal = TerminalBuilder.builder().build();
        LineReader lineReader = LineReaderBuilder.builder()
                                                 .completer(jLineCommandManager)
                                                 .option(LineReader.Option.INSERT_TAB, false)
                                                 .terminal(terminal)
                                                 .appName("Test")
                                                 .build();
        boolean[] shouldStop = new boolean[]{false};
        jLineCommandManager.command(
                jLineCommandManager.commandBuilder("stop", SimpleCommandMeta.empty())
                                   .handler(commandContext ->
                                                        shouldStop[0] = true)
                                   .build())
                           .command(jLineCommandManager.commandBuilder("echo", SimpleCommandMeta.empty())
                                                               .argument(String.class, "string", builder ->
                                                                       builder.asRequired()
                                                                              .withParser(((commandContext, inputQueue) -> {
                                                                                  final StringBuilder stringBuilder =
                                                                                          new StringBuilder();
                                                                                  while (!inputQueue.isEmpty()) {
                                                                                      stringBuilder.append(inputQueue.remove());
                                                                                      if (!inputQueue.isEmpty()) {
                                                                                          stringBuilder.append(" ");
                                                                                      }
                                                                                  }
                                                                                  return ArgumentParseResult.success(
                                                                                          stringBuilder.toString());
                                                                              })).build())
                                                               .handler(commandContext -> commandContext.get("string")
                                                                                                            .ifPresent(
                                                                                                             System.out::println))
                                                               .build())
                           .command(jLineCommandManager.commandBuilder("test", SimpleCommandMeta.empty())
                                                               .argument(StaticArgument.required("one"))
                                                               .handler(commandContext -> System.out.println("Test (1)"))
                                                               .build())
                           .command(jLineCommandManager.commandBuilder("test", SimpleCommandMeta.empty())
                                                               .argument(StaticArgument.required("two"))
                                                               .handler(commandContext -> System.out.println("Test (2)"))
                                                               .build());
        System.out.println("Ready...");
        while (!shouldStop[0]) {
            final String line = lineReader.readLine();
            if (line == null || line.isEmpty() || !line.startsWith("/")) {
                System.out.println("Empty line");
                continue;
            }
            try {
                final List<String> suggestions = jLineCommandManager.suggest(new JLineCommandSender(), line.substring(1));
                for (final String suggestion : suggestions) {
                    System.out.printf("> %s\n", suggestion);
                }
                // jLineCommandManager.executeCommand(new JLineCommandSender(), line.substring(1)).join();
                // System.out.println("Successfully executed " + line);
            } catch (RuntimeException runtimeException) {
                if (runtimeException.getCause() instanceof NoSuchCommandException) {
                    System.out.println("No such command");
                } else if (runtimeException.getCause() instanceof InvalidSyntaxException) {
                    System.out.println(runtimeException.getCause().getMessage());
                } else {
                    System.out.printf("Something went wrong: %s\n", runtimeException.getCause().getMessage());
                    runtimeException.printStackTrace();
                }
            }
            if (shouldStop[0]) {
                System.out.println("Stopping.");
            }
        }
    }

    @Override
    public final void complete(@Nonnull final LineReader lineReader,
                         @Nonnull final ParsedLine parsedLine,
                         @Nonnull final List<Candidate> list) {
        final String line = parsedLine.line();
        if (line == null || line.isEmpty() || !line.startsWith("/")) {
            System.out.println("Cannot suggest: empty line");
            return;
        }
        System.out.printf("Trying to complete '%s'\n", line);
    }

    @Nonnull
    @Override
    public final SimpleCommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    @Override
    public final boolean hasPermission(@Nonnull final JLineCommandSender sender, @Nonnull final String permission) {
       return true;
    }

}
