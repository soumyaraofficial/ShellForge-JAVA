import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
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
        // COMMAND LIST (used by TAB completion for word 0)
        // =========================================================
        Set<String> commands = getCommands();
        commands.addAll(Builtins.names());

        // =========================================================
        // LINE READER
        //
        // No .completer(...) is registered here: JLine's built-in
        // ambiguous-completion handling doesn't implement the
        // required "bell on first TAB, list on second TAB" flow in
        // this environment, so completion is instead driven
        // entirely by a custom widget bound directly to the TAB
        // key below.
        // =========================================================

        LineReader reader =
                LineReaderBuilder.builder()
                        .terminal(terminal)
                        .parser(parser)
                        .build();

        // =========================================================
        // TAB COMPLETION  (commands for word 0, filenames for
        // word 1+, with longest-common-prefix completion and
        // bell-then-list behavior for ambiguous matches)
        // =========================================================

        FileNameCompleter tabCompletion =
                new FileNameCompleter(
                        reader,
                        terminal,
                        () -> currentDirectory,
                        () -> commands
                );

        reader.getWidgets().put(
                "shell-tab-complete",
                tabCompletion.createTabWidget()
        );

        reader.getKeyMaps()
                .get(LineReader.MAIN)
                .bind(
                        new Reference("shell-tab-complete"),
                        "\t"
                );

        // =========================================================
        // SHELL LOOP
        // =========================================================

        while (true) {

            // -----------------------------------------------------
            // AUTOMATIC BACKGROUND JOB REAPING
            //
            // Runs before every prompt is drawn - after foreground
            // commands, builtins, background launches, and empty
            // input (which loops straight back here via `continue`
            // below). Prints "Done" lines for anything that
            // finished since the last check; silent otherwise.
            // -----------------------------------------------------

            JobManager.reapBeforePrompt(System.out);

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