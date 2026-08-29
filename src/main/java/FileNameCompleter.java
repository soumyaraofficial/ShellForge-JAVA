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
    //  - word 1+ (argument)       -> if the command has a
    //                                 registered `complete -C`
    //                                 completer, run it; otherwise
    //                                 match against filenames in
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
                    resolveMatches(
                            buffer,
                            completingFirstWord,
                            prefix
                    );

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

                /*
                 * Directory matches already carry a trailing '/'
                 * (added in findMatchingFiles) and get no space,
                 * so the user can immediately TAB into the next
                 * path segment. Everything else (files, commands,
                 * external completer candidates) gets a trailing
                 * space as before.
                 */
                String separator =
                        match.endsWith("/")
                                ? ""
                                : " ";

                String completed =
                        replacePrefix(
                                buffer,
                                match
                        ) + separator;

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
    // RESOLVE MATCHES
    //
    // word 0            -> commands (unchanged)
    // word 1+, command
    //   has a registered
    //   -C completer     -> run the external completer
    // word 1+, otherwise -> filenames (unchanged)
    // =============================================================

    private List<String> resolveMatches(
            String buffer,
            boolean completingFirstWord,
            String prefix) {

        if (completingFirstWord) {
            return findMatchingCommands(prefix);
        }

        String commandName =
                getCommandName(buffer);

        String completerScript =
                CompletionRegistry.get(commandName);

        if (completerScript != null) {

            int cursor =
                    reader.getBuffer().cursor();

            return ExternalCompleter.getCandidates(
                    completerScript,
                    buffer,
                    cursor
            );
        }

        return findMatchingFiles(prefix);
    }

    private String getCommandName(String buffer) {

        String trimmed = buffer.stripLeading();

        int spaceIndex = trimmed.indexOf(' ');

        return spaceIndex == -1
                ? trimmed
                : trimmed.substring(0, spaceIndex);
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
    //
    // Supports nested paths: if the current word contains a '/',
    // everything up to and including the last '/' is treated as
    // the directory to search (resolved relative to the shell's
    // current directory), and everything after it is the name
    // prefix to match. Matches are returned with the directory
    // portion re-attached, so the caller can drop the full result
    // straight back into the buffer as a complete replacement.
    //
    // With no '/', behavior is unchanged from before: search the
    // current directory directly.
    // =============================================================

    private List<String> findMatchingFiles(
            String prefix) {

        List<String> matches =
                new ArrayList<>();

        if (prefix == null) {
            prefix = "";
        }

        int lastSlash =
                prefix.lastIndexOf('/');

        String directoryPart =
                lastSlash >= 0
                        ? prefix.substring(0, lastSlash + 1)
                        : "";

        String namePrefix =
                lastSlash >= 0
                        ? prefix.substring(lastSlash + 1)
                        : prefix;

        Path baseDirectory =
                resolveDirectory();

        Path searchDirectory =
                directoryPart.isEmpty()
                        ? baseDirectory
                        : baseDirectory
                                .resolve(directoryPart)
                                .normalize();

        File dir =
                searchDirectory.toFile();

        File[] files =
                dir.listFiles();

        /*
         * Directory doesn't exist, isn't a directory, or isn't
         * readable - no matches, handled gracefully.
         */
        if (files == null) {
            return matches;
        }

        Set<String> sortedNames =
                new TreeSet<>();

        for (File file : files) {

            String name =
                    file.getName();

            if (!name.startsWith(namePrefix)) {
                continue;
            }

            String entry =
                    directoryPart + name;

            /*
             * Directories get a trailing '/' baked into the
             * match itself (and, per the single-match branch
             * above, no additional trailing space) so the user
             * can immediately TAB again into the next path
             * segment - e.g. "pig/" -> "pig/dog/".
             */
            if (file.isDirectory()) {
                entry = entry + "/";
            }

            sortedNames.add(entry);
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