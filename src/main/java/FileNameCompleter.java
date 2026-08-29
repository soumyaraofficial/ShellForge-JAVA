import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class FileNameCompleter implements Completer {

    private final Supplier<Path> currentDirectorySupplier;
    private final Supplier<Set<String>> commandNamesSupplier;

    public FileNameCompleter(
            Supplier<Path> currentDirectorySupplier,
            Supplier<Set<String>> commandNamesSupplier) {

        this.currentDirectorySupplier =
                currentDirectorySupplier;

        this.commandNamesSupplier =
                commandNamesSupplier;
    }

    // =============================================================
    // COMPLETE
    //
    // Word 0 (the command itself) is completed exactly the same
    // way StringsCompleter used to do it: a flat list of Candidate
    // objects with complete=true, so JLine's default ambiguous-
    // completion handling (bell on first TAB, list on second TAB,
    // common-prefix expansion) behaves identically to before.
    //
    // Word 1+ (arguments) is completed against filenames in the
    // shell's current directory.
    // =============================================================

    @Override
    public void complete(
            LineReader reader,
            ParsedLine line,
            List<Candidate> candidates) {

        if (line == null) {
            return;
        }

        if (line.wordIndex() == 0) {

            completeCommandNames(
                    line,
                    candidates
            );

        } else {

            completeFileNames(
                    line,
                    candidates
            );
        }
    }

    // =============================================================
    // COMMAND NAME COMPLETION (word 0)
    // =============================================================

    private void completeCommandNames(
            ParsedLine line,
            List<Candidate> candidates) {

        String prefix = line.word();

        if (prefix == null) {
            prefix = "";
        }

        Set<String> commandNames =
                commandNamesSupplier != null
                        ? commandNamesSupplier.get()
                        : null;

        if (commandNames == null) {
            return;
        }

        for (String name : commandNames) {

            if (name.startsWith(prefix)) {

                candidates.add(
                        new Candidate(
                                name,
                                name,
                                null,
                                null,
                                null,
                                null,
                                true
                        )
                );
            }
        }
    }

    // =============================================================
    // FILENAME COMPLETION (word 1+)
    // =============================================================

    private void completeFileNames(
            ParsedLine line,
            List<Candidate> candidates) {

        String prefix = line.word();

        if (prefix == null) {
            prefix = "";
        }

        Path directory = resolveDirectory();

        File dir = directory.toFile();

        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            String name = file.getName();

            if (!name.startsWith(prefix)) {
                continue;
            }

            /*
             * complete = true tells JLine's default completion
             * behavior to:
             *
             *  - auto-insert the missing characters when this
             *    is the only match, and
             *  - append a trailing space after it,
             *
             * exactly matching:
             *
             *   cat re<TAB>  -> cat readme.txt
             */
            candidates.add(
                    new Candidate(
                            name,
                            name,
                            null,
                            null,
                            null,
                            null,
                            true
                    )
            );
        }
    }

    // =============================================================
    // RESOLVE CURRENT DIRECTORY
    // =============================================================

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
}