import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.ArgumentCompleter;
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
        Set<String> commands = getCommands();
        commands.add("echo");
        commands.add("cd");
        commands.add("pwd");
        commands.add("type");
        commands.add("exit");

        StringsCompleter commandCompleter =
                new StringsCompleter(
                        commands
                );

        // =========================================================
        // FILENAME COMPLETION  (NEW)
        //
        // Word 0 (the command itself) keeps using the existing
        // commandCompleter above, completely unchanged.
        //
        // Word 1+ (arguments, e.g. "cat re<TAB>") is routed to
        // FilenameCompleter, which scans the shell's live
        // current directory.
        // =========================================================

        Completer filenameCompleter =
                new FileNameCompleter(
                        () -> currentDirectory
                );

        ArgumentCompleter completer =
                new ArgumentCompleter(
                        commandCompleter,
                        filenameCompleter
                );

        /*
         * Don't require the command word to be an exact,
         * already-validated match before allowing argument
         * (filename) completion to kick in.
         */
        completer.setStrict(false);

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