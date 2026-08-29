import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
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

        DefaultParser parser =
                new DefaultParser();

        parser.setEscapeChars(null);

        // =========================================================
        // COMMAND COMPLETION
        // =========================================================

        Set<String> commands =
                getCommands();

        commands.add("echo");
        commands.add("cd");
        commands.add("pwd");
        commands.add("type");
        commands.add("exit");

        StringsCompleter commandCompleter =
                new StringsCompleter(commands);

        // =========================================================
        // FILENAME COMPLETION
        // =========================================================

        FileNameCompleter fileNameCompleter =
                new FileNameCompleter(
                        () -> currentDirectory
                );

        // =========================================================
        // COMPLETION
        // =========================================================

        AggregateCompleter completer =
                new AggregateCompleter(
                        commandCompleter,
                        fileNameCompleter
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

    // =============================================================
    // GET AVAILABLE COMMANDS
    // =============================================================

    public static Set<String> getCommands() {

        Set<String> commands =
                new TreeSet<>();

        String paths =
                System.getenv("PATH");

        if (paths == null) {
            return commands;
        }

        for (String dir :
                paths.split(File.pathSeparator)) {

            File directory =
                    new File(dir);

            File[] files =
                    directory.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {

                if (file.isFile()
                        && file.canExecute()) {

                    commands.add(
                            file.getName()
                    );
                }
            }
        }

        return commands;
    }
}