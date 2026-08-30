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
        //
        // No .history(...) is set explicitly either: JLine builds
        // an in-memory History automatically, and its default
        // keymap already binds UP/DOWN to move through it and
        // ENTER to submit whatever line is currently on the buffer
        // - which is exactly the behavior the `history` builtin
        // below needs to read from. HISTORY_IGNORE_DUPS is turned
        // off so that typing the same command twice in a row (e.g.
        // "history" immediately followed by "history") still
        // records two separate entries, as the shell's `history`
        // output must reflect every invocation.
        // =========================================================

        LineReader reader =
                LineReaderBuilder.builder()
                        .terminal(terminal)
                        .parser(parser)
                        .option(LineReader.Option.HISTORY_IGNORE_DUPS, false)
                        .build();

        HistoryManager.setHistory(reader.getHistory());

        // =========================================================
        // LOAD $HISTFILE ON STARTUP
        //
        // If HISTFILE is unset or the file doesn't exist, this is
        // a silent no-op - startup must never crash and must never
        // create the file just because it was missing. Entries
        // loaded here are marked as already persisted, so a later
        // `history -a` or exit-time save won't re-write them.
        // =========================================================

        String startupHistFile = System.getenv("HISTFILE");

        if (startupHistFile != null && !startupHistFile.isBlank()) {
            HistoryManager.loadStartupHistory(startupHistFile);
        }

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

        // =========================================================
        // PERSIST HISTORY TO $HISTFILE ON EXIT
        //
        // Covers both exit paths above (the `exit` command and
        // EOF/Ctrl-D). Only entries not yet persisted (by startup
        // load or an earlier `history -a`) are appended, so this
        // can never duplicate what's already on disk.
        // =========================================================

        String exitHistFile = System.getenv("HISTFILE");

        if (exitHistFile != null && !exitHistFile.isBlank()) {
            HistoryManager.saveOnExit(exitHistFile);
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