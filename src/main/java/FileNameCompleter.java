import org.jline.reader.LineReader;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public class FileNameCompleter {

    private final LineReader reader;
    private final Terminal terminal;
    private final Supplier<Path> currentDirectorySupplier;
    private final Supplier<Set<String>> commandNamesSupplier;

    /*
     * false = first TAB (of this "stuck" attempt)
     * true  = second TAB
     */
    private boolean tabPressed = false;

    public FileNameCompleter(
            LineReader reader,
            Terminal terminal,
            Supplier<Path> currentDirectorySupplier,
            Supplier<Set<String>> commandNamesSupplier) {

        this.reader = reader;
        this.terminal = terminal;
        this.currentDirectorySupplier = currentDirectorySupplier;
        this.commandNamesSupplier = commandNamesSupplier;
    }

    // =============================================================
    // CREATE TAB WIDGET
    //
    // Bound directly to the TAB key (see Shell.java). Implements:
    //
    //  - word 0 (command name)    -> match against PATH/builtins
    //  - word 1+ (argument)       -> match against filenames in
    //                                 the shell's current directory
    //
    // Behavior for both:
    //  - 0 matches            -> ring the bell
    //  - 1 match               -> complete it, trailing space
    //  - 2+ matches, LCP grows -> complete to the longest common
    //                             prefix, no trailing space
    //  - 2+ matches, LCP stuck -> bell on first TAB, list of all
    //                             matches (alphabetical, two-space
    //                             separated) on the second TAB
    // =============================================================

    public Widget createTabWidget() {

        return () -> {

            String buffer =
                    reader.getBuffer().toString();

            boolean completingFirstWord =
                    isCompletingFirstWord(buffer);

            String prefix =
                    getCurrentWordPrefix(buffer);

            List<String> matches =
                    completingFirstWord
                            ? findMatchingCommands(prefix)
                            : findMatchingFiles(prefix);

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
                        replacePrefix(
                                buffer,
                                match
                        ) + " ";

                setBuffer(completed);

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
                        replacePrefix(
                                buffer,
                                commonPrefix
                        );

                setBuffer(completed);

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

            List<String> sorted =
                    new ArrayList<>(
                            new TreeSet<>(matches)
                    );

            terminal.writer().println();

            terminal.writer().println(
                    String.join("  ", sorted)
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
    // WORD-BOUNDARY HELPERS
    // =============================================================

    /*
     * True while the token currently being typed is the very
     * first word on the line (the command name itself), even if
     * preceded by leading spaces.
     */
    private boolean isCompletingFirstWord(String buffer) {

        String trimmedLeading =
                buffer.stripLeading();

        return !trimmedLeading.contains(" ");
    }

    /*
     * The text of the word currently being completed: everything
     * after the last space in the buffer (or the whole buffer if
     * there is no space).
     */
    private String getCurrentWordPrefix(String buffer) {

        int lastSpace =
                buffer.lastIndexOf(' ');

        return buffer.substring(lastSpace + 1);
    }

    /*
     * Rebuilds the buffer with the word currently being completed
     * replaced by `replacement`, preserving everything before it
     * (including any leading spaces or earlier words).
     */
    private String replacePrefix(
            String buffer,
            String replacement) {

        int lastSpace =
                buffer.lastIndexOf(' ');

        String before =
                buffer.substring(0, lastSpace + 1);

        return before + replacement;
    }

    private void setBuffer(String completed) {

        reader.getBuffer().clear();

        reader.getBuffer().write(completed);
    }

    // =============================================================
    // FIND MATCHING COMMANDS (word 0)
    // =============================================================

    private List<String> findMatchingCommands(
            String prefix) {

        List<String> matches =
                new ArrayList<>();

        if (prefix == null) {
            return matches;
        }

        Set<String> commandNames =
                commandNamesSupplier != null
                        ? commandNamesSupplier.get()
                        : null;

        if (commandNames == null) {
            return matches;
        }

        for (String command :
                new TreeSet<>(commandNames)) {

            if (command.startsWith(prefix)) {
                matches.add(command);
            }
        }

        return matches;
    }

    // =============================================================
    // FIND MATCHING FILES (word 1+)
    // =============================================================

    private List<String> findMatchingFiles(
            String prefix) {

        List<String> matches =
                new ArrayList<>();

        if (prefix == null) {
            prefix = "";
        }

        Path directory =
                resolveDirectory();

        File dir =
                directory.toFile();

        File[] files =
                dir.listFiles();

        if (files == null) {
            return matches;
        }

        Set<String> sortedNames =
                new TreeSet<>();

        for (File file : files) {

            String name =
                    file.getName();

            if (name.startsWith(prefix)) {
                sortedNames.add(name);
            }
        }

        matches.addAll(sortedNames);

        return matches;
    }

    private Path resolveDirectory() {

        Path directory = null;

        if (currentDirectorySupplier != null) {
            directory = currentDirectorySupplier.get();
        }

        if (directory == null) {

            directory = Path.of(
                    System.getProperty("user.dir")
            );
        }

        return directory;
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
}