import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

    private final CommandExecutor executor =
            new CommandExecutor();

    private Path currentDirectory =
            Path.of("").toAbsolutePath();

    /*
     * Multiple completion state.
     *
     * false = first TAB
     * true  = second TAB
     */
    private boolean tabPressed = false;

    public void run() throws Exception {

        // =========================================================
        // TERMINAL
        // =========================================================

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
         * Do not let JLine consume backslashes.
         *
         * Our Quoting.parseCommand() handles shell escaping.
         */
        parser.setEscapeChars(null);

        // =========================================================
        // LINE READER
        // =========================================================

        LineReader reader =
                LineReaderBuilder.builder()
                        .terminal(terminal)
                        .parser(parser)
                        .build();

        // =========================================================
        // CUSTOM TAB WIDGET
        // =========================================================

        Widget tabWidget = () -> {

            /*
             * Get the current input buffer.
             */
            String buffer =
                    reader.getBuffer().toString();

            /*
             * Get the command prefix.
             *
             * Examples:
             *
             * "ech"      -> "ech"
             * "custom"   -> "custom"
             * "xyz_"     -> "xyz_"
             */
            String prefix =
                    getCommandPrefix(buffer);

            /*
             * Find both builtin and external commands.
             */
            List<String> matches =
                    findMatchingCommands(prefix);

            // =====================================================
            // NO MATCHES
            // =====================================================

            if (matches.isEmpty()) {

                /*
                 * No completion exists.
                 *
                 * Ring the bell.
                 */
                tabPressed = false;

                terminal.writer().print("\007");
                terminal.writer().flush();

                return true;
            }

            // =====================================================
            // ONE MATCH
            // =====================================================

            if (matches.size() == 1) {

                String match =
                        matches.get(0);

                /*
                 * Replace only the command prefix.
                 */
                String completed =
                        replaceCommandPrefix(
                                buffer,
                                prefix,
                                match
                        );

                /*
                 * CodeCrafters expects a space after
                 * a successful completion.
                 *
                 * Example:
                 *
                 * ech<TAB>
                 *
                 * becomes:
                 *
                 * echo␠
                 */
                if (buffer.length() == prefix.length()) {
                    completed += " ";
                }

                /*
                 * Update JLine's input buffer.
                 */
                reader.getBuffer().clear();

                reader.getBuffer().write(
                        completed
                );

                /*
                 * Reset multiple-TAB state.
                 */
                tabPressed = false;

                return true;
            }

            // =====================================================
            // MULTIPLE MATCHES
            // =====================================================

            /*
             * FIRST TAB
             */
            if (!tabPressed) {

                /*
                 * Only ring the bell.
                 *
                 * Do NOT print matches.
                 */
                terminal.writer().print("\007");
                terminal.writer().flush();

                tabPressed = true;

                return true;
            }

            /*
             * SECOND TAB
             *
             * Print all matches.
             */

            terminal.writer().println();

            terminal.writer().println(
                    String.join(
                            "  ",
                            matches
                    )
            );

            terminal.writer().flush();

            /*
             * Reset TAB state.
             */
            tabPressed = false;

            /*
             * Redraw the original prompt and
             * preserve the input buffer.
             */
            reader.callWidget(
                    LineReader.REDRAW_LINE
            );

            reader.callWidget(
                    LineReader.REDISPLAY
            );

            return true;
        };

        // =========================================================
        // REGISTER TAB WIDGET
        // =========================================================

        reader.getWidgets().put(
                "codecrafters-tab",
                tabWidget
        );

        /*
         * Override JLine's default TAB behavior.
         */
        reader.getKeyMaps()
                .get(LineReader.MAIN)
                .bind(
                        tabWidget,
                        "\t"
                );

        // =========================================================
        // SHELL LOOP
        // =========================================================

        try {

            while (true) {

                /*
                 * Every new command starts a fresh
                 * multiple-completion sequence.
                 */
                tabPressed = false;

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

                /*
                 * Execute the command.
                 *
                 * External commands and builtins are handled
                 * by CommandExecutor.
                 */
                currentDirectory =
                        executor.execute(
                                command,
                                currentDirectory
                        );
            }

        } finally {

            terminal.close();
        }
    }

    // =============================================================
    // GET COMMAND PREFIX
    // =============================================================

    private String getCommandPrefix(
            String buffer) {

        /*
         * Ignore leading spaces.
         */
        String trimmed =
                buffer.stripLeading();

        if (trimmed.isEmpty()) {
            return "";
        }

        /*
         * We only complete the first word.
         *
         * Example:
         *
         * "echo hello"
         *
         * gives:
         *
         * "echo"
         */
        int spaceIndex =
                trimmed.indexOf(' ');

        if (spaceIndex == -1) {
            return trimmed;
        }

        return trimmed.substring(
                0,
                spaceIndex
        );
    }

    // =============================================================
    // FIND MATCHING COMMANDS
    // =============================================================

    private List<String> findMatchingCommands(
            String prefix) {

        /*
         * TreeSet:
         *
         * 1. Alphabetically sorts commands
         * 2. Removes duplicates
         */
        Set<String> sortedMatches =
                new TreeSet<>();

        if (prefix == null ||
                prefix.isEmpty()) {

            return new ArrayList<>();
        }

        // =========================================================
        // BUILTIN COMMANDS
        // =========================================================

        sortedMatches.add("echo");
        sortedMatches.add("cd");
        sortedMatches.add("pwd");
        sortedMatches.add("type");
        sortedMatches.add("exit");

        // =========================================================
        // EXTERNAL COMMANDS FROM PATH
        // =========================================================

        String path =
                System.getenv("PATH");

        if (path != null &&
                !path.isEmpty()) {

            /*
             * PATH can contain multiple directories.
             *
             * Example:
             *
             * /tmp/fox:/tmp/dog:/tmp/rat
             */
            for (String directoryPath :
                    path.split(File.pathSeparator)) {

                if (directoryPath == null ||
                        directoryPath.isEmpty()) {

                    continue;
                }

                File directory =
                        new File(directoryPath);

                File[] files =
                        directory.listFiles();

                if (files == null) {
                    continue;
                }

                for (File file : files) {

                    /*
                     * Only regular files.
                     */
                    if (!file.isFile()) {
                        continue;
                    }

                    /*
                     * Only executable files.
                     */
                    if (!file.canExecute()) {
                        continue;
                    }

                    String name =
                            file.getName();

                    sortedMatches.add(name);
                }
            }
        }

        // =========================================================
        // FILTER BY PREFIX
        // =========================================================

        List<String> matches =
                new ArrayList<>();

        for (String command :
                sortedMatches) {

            if (command.startsWith(prefix)) {
                matches.add(command);
            }
        }

        return matches;
    }

    // =============================================================
    // REPLACE COMMAND PREFIX
    // =============================================================

    private String replaceCommandPrefix(
            String buffer,
            String prefix,
            String replacement) {

        /*
         * Count leading spaces.
         */
        int leadingSpaces =
                buffer.length()
                        - buffer.stripLeading().length();

        /*
         * Replace only the first command.
         *
         * Example:
         *
         * "ech"
         *
         * becomes:
         *
         * "echo"
         *
         * And:
         *
         * "ech hello"
         *
         * becomes:
         *
         * "echo hello"
         */
        return buffer.substring(
                0,
                leadingSpaces
        )
                + replacement
                + buffer.substring(
                        leadingSpaces
                                + prefix.length()
                );
    }

    // =============================================================
    // GET ALL EXTERNAL COMMANDS
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

            if (dir == null ||
                    dir.isEmpty()) {

                continue;
            }

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