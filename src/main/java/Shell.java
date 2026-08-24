import java.nio.file.Path;
import java.util.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
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

        StringsCompleter completer =
                new StringsCompleter(
                        "echo",
                        "cd",
                        "pwd",
                        "type",
                        "exit"
                );

        /*
         * IMPORTANT:
         *
         * JLine normally has its own parser for quotes
         * and escape characters.
         *
         * We don't want JLine parsing our shell command.
         * Our Quoting.parseCommand() is responsible for that.
         *
         * This parser simply returns the original line.
         */
        Parser rawParser = new Parser() {

            @Override
            public org.jline.reader.ParsedLine parse(
                    String line,
                    int cursor,
                    ParseContext context) {

                return new org.jline.reader.ParsedLine() {

                    @Override
                    public String word() {
                        return line;
                    }

                    @Override
                    public int wordCursor() {
                        return cursor;
                    }

                    @Override
                    public int wordIndex() {
                        return 0;
                    }

                    @Override
                    public List<String> words() {
                        return List.of(line);
                    }

                    @Override
                    public String line() {
                        return line;
                    }

                    @Override
                    public int cursor() {
                        return cursor;
                    }
                };
            }
        };

        LineReader reader =
                LineReaderBuilder.builder()
                        .terminal(terminal)
                        .parser(rawParser)
                        .completer(completer)
                        .build();

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