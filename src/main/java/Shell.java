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
     * Used for WH6 multiple-match handling.
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
        // PARSER
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

            String buffer =
                    reader.getBuffer().toString();

            String prefix =
                    getCommandPrefix(buffer);

            /*
             * Find all builtin and external commands
             * matching the current prefix.
             */
            List<String> matches =
                    findMatchingCommands(prefix);

            // =====================================================
            // NO MATCHES
            // =====================================================

            if (matches.isEmpty()) {

                tabPressed = false;

                /*
                 * Ring the bell.
                 */
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

                String completed =
                        replaceCommandPrefix(
                                buffer,
                                prefix,
                                match
                        );

                /*
                 * Exactly one match means the command is
                 * completely resolved.
                 *
                 * Add the required trailing space.
                 */
                if (buffer.length() == prefix.length()) {
                    completed += " ";
                }

                reader.getBuffer().clear();

                reader.getBuffer().write(
                        completed
                );

                tabPressed = false;

                return true;
            }

            // =====================================================
            // MULTIPLE MATCHES
            // =====================================================

            /*
             * Find the longest common prefix of all matches.
             */
            String commonPrefix =
                    longestCommonPrefix(matches);

            // =====================================================
            // LCP IS LONGER THAN CURRENT INPUT
            // =====================================================

            if (commonPrefix.length() > prefix.length()) {

                /*
                 * We can make progress.
                 *
                 * Example:
                 *
                 * xyz_
                 *
                 * matches:
                 *
                 * xyz_foo
                 * xyz_foo_bar
                 * xyz_foo_bar_baz
                 *
                 * commonPrefix:
                 *
                 * xyz_foo
                 */
                String completed =
                        replaceCommandPrefix(
                                buffer,
                                prefix,
                                commonPrefix
                        );

                reader.getBuffer().clear();

                reader.getBuffer().write(
                        completed
                );

                /*
                 * This was a successful partial completion.
                 *
                 * Do not add a space because there are
                 * still multiple possible commands.
                 */
                tabPressed = false;

                return true;
            }

            // =====================================================
            // NO FURTHER LCP PROGRESS
            // =====================================================

            /*
             * The current input is already the longest common
             * prefix.
             *
             * Example:
             *
             * xyz_
             *
             * xyz_cow
             * xyz_owl
             * xyz_rat
             *
             * There is no additional character to complete.
             *
             * First TAB -> bell
             * Second TAB -> list matches
             */

            if (!tabPressed) {

                /*
                 * FIRST TAB
                 */
                terminal.writer().print("\007");
                terminal.writer().flush();

                tabPressed = true;

                return true;
            }

            // =====================================================
            // SECOND TAB
            // =====================================================

            terminal.writer().println();

            terminal.writer().println(
                    String.join(
                            "  ",
                            matches
                    )
            );

            terminal.writer().flush();

            tabPressed = false;

            /*
             * Restore the prompt and original input.
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
        // REGISTER CUSTOM TAB WIDGET
        // =========================================================

        reader.getWidgets().put(
                "codecrafters-tab",
                tabWidget
        );

        /*
         * Override JLine's normal TAB behavior.
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
                 * Every new command starts a new TAB sequence.
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
                 * Execute command normally.
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
         * Only complete the first word.
         *
         * Example:
         *
         * "xyz_ hello"
         *
         * -> "xyz_"
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
        // EXTERNAL COMMANDS
        // =========================================================

        String path =
                System.getenv("PATH");

        if (path != null &&
                !path.isEmpty()) {

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

                    if (!file.isFile()) {
                        continue;
                    }

                    if (!file.canExecute()) {
                        continue;
                    }

                    String name =
                            file.getName();

                    if (name.startsWith(prefix)) {
                        sortedMatches.add(name);
                    }
                }
            }
        }

        // =========================================================
        // FILTER
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
    // LONGEST COMMON PREFIX
    // =============================================================

    private String longestCommonPrefix(
            List<String> matches) {

        if (matches == null ||
                matches.isEmpty()) {

            return "";
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        String first =
                matches.get(0);

        int commonLength =
                first.length();

        for (int i = 1;
             i < matches.size();
             i++) {

            String current =
                    matches.get(i);

            int length =
                    Math.min(
                            commonLength,
                            current.length()
                    );

            int j = 0;

            while (j < length &&
                    first.charAt(j)
                            == current.charAt(j)) {

                j++;
            }

            commonLength = j;

            if (commonLength == 0) {
                return "";
            }
        }

        return first.substring(
                0,
                commonLength
        );
    }

    // =============================================================
    // REPLACE COMMAND PREFIX
    // =============================================================

    private String replaceCommandPrefix(
            String buffer,
            String prefix,
            String replacement) {

        /*
         * Preserve leading spaces.
         */
        int leadingSpaces =
                buffer.length()
                        - buffer.stripLeading().length();

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