import java.nio.file.Path;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

    private final CommandExecutor executor =
            new CommandExecutor();

    private Path currentDirectory =
            Path.of("").toAbsolutePath();

    public void run() throws Exception {

        Terminal terminal =
                TerminalBuilder.builder()
                        .system(true)
                        .build();

        // =========================================================
        // JLINE PARSER
        // =========================================================

        DefaultParser parser =
                new DefaultParser();

        /*
         * IMPORTANT:
         *
         * Do NOT let JLine consume backslashes.
         *
         * Our Quoting.parseCommand() handles shell
         * escaping itself.
         */
        parser.setEscapeChars(null);

        // =========================================================
        // COMMAND COMPLETION
        // =========================================================

        StringsCompleter completer =
                new StringsCompleter(
                        "echo",
                        "cd",
                        "pwd",
                        "type",
                        "exit"
                );

        // =========================================================
        // LINE READER
        // =========================================================

        LineReader reader =
                LineReaderBuilder.builder()
                        .terminal(terminal)
                        .parser(parser)
                        .completer(completer)
                        .build();

        // =========================================================
        // SHELL LOOP
        // =========================================================

        while (true) {

            String command =
                    reader.readLine("$ ");

            if (command == null) {
                break;
            }

            if (command.isBlank()) {
                continue;
            }

            if (command.equalsIgnoreCase("exit")) {
                break;
            }

            currentDirectory =
                    executor.execute(
                            command,
                            currentDirectory
                    );
        }

        terminal.close();
    }
}