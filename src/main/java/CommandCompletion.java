import org.jline.reader.LineReader;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CommandCompletion {

    private final LineReader reader;
    private final Terminal terminal;

    /*
     * false = first TAB
     * true  = second TAB
     */
    private boolean tabPressed = false;

    public CommandCompletion(
            LineReader reader,
            Terminal terminal) {

        this.reader = reader;
        this.terminal = terminal;
    }

    // =============================================================
    // CREATE TAB WIDGET
    // =============================================================

    public Widget createTabWidget() {

        return () -> {

            String buffer =
                    reader.getBuffer().toString();

            String prefix =
                    getCommandPrefix(buffer);

            List<String> matches =
                    findMatchingCommands(prefix);

            // =====================================================
            // NO MATCHES
            // =====================================================

            if (matches.isEmpty()) {

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

                String completed =
                        replaceCommandPrefix(
                                buffer,
                                prefix,
                                match
                        );

                /*
                 * If the command itself is the entire input,
                 * add a trailing space.
                 */
                if (buffer.length() == prefix.length()) {
                    completed += " ";
                }

                reader.getBuffer().clear();

                reader.getBuffer().write(completed);

                tabPressed = false;

                return true;
            }

            // =====================================================
            // MULTIPLE MATCHES
            // =====================================================

            String commonPrefix =
                    longestCommonPrefix(matches);

            // =====================================================
            // LCP CAN EXTEND INPUT
            // =====================================================

            if (commonPrefix.length() > prefix.length()) {

                String completed =
                        replaceCommandPrefix(
                                buffer,
                                prefix,
                                commonPrefix
                        );

                reader.getBuffer().clear();

                reader.getBuffer().write(completed);

                tabPressed = false;

                return true;
            }

            // =====================================================
            // NO FURTHER LCP PROGRESS
            // =====================================================

            if (!tabPressed) {

                /*
                 * First TAB:
                 * just ring the bell.
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
                    String.join("  ", matches)
            );

            terminal.writer().flush();

            tabPressed = false;

            /*
             * Restore prompt and input.
             */
            reader.callWidget(
                    LineReader.REDRAW_LINE
            );

            reader.callWidget(
                    LineReader.REDISPLAY
            );

            return true;
        };
    }

    // =============================================================
    // RESET TAB STATE
    // =============================================================

    public void resetTabState() {
        tabPressed = false;
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
        // BUILTINS
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