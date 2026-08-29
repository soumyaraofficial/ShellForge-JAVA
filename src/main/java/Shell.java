import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
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
        Set<String> commands = getCommands();
        commands.add("echo");
        commands.add("cd");
        commands.add("pwd");
        commands.add("type");
        commands.add("exit");

        // =========================================================
        // FILENAME COMPLETION  (NEW)
        //
        // A single completer that dispatches on word position:
        //
        //  - word 0 (the command itself) is completed against
        //    `commands`, using the exact same flat Candidate
        //    construction StringsCompleter used before, so JLine's
        //    default ambiguous-completion behavior (bell on first
        //    TAB, list on second TAB, common-prefix expansion)
        //    is unchanged from the previous behavior.
        //
        //  - word 1+ (arguments, e.g. "cat re<TAB>") is completed
        //    against filenames in the shell's live current
        //    directory.
        // =========================================================

        Completer completer =
                new FileNameCompleter(
                        () -> currentDirectory,
                        () -> commands
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

    public static Set<String> getCommands() {

        Set<String> commands = new TreeSet<>();

        String paths = System.getenv("PATH");

        if (paths == null) {
            return commands;
        }

        for (String dir : paths.split(File.pathSeparator)) {

            File directory = new File(dir);

            File[] files = directory.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {

                if (file.isFile() && file.canExecute()) {
                    commands.add(file.getName());
                }
            }
        }

        return commands;
    }
}