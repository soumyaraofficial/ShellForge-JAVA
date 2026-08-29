import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class FileNameCompleter implements Completer {

    private final Supplier<Path> currentDirectorySupplier;

    public FileNameCompleter(
            Supplier<Path> currentDirectorySupplier) {

        this.currentDirectorySupplier =
                currentDirectorySupplier;
    }

    // =============================================================
    // COMPLETE
    // =============================================================

    @Override
    public void complete(
            LineReader reader,
            ParsedLine line,
            List<Candidate> candidates) {

        if (line == null) {
            return;
        }

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
             *
             * When multiple candidates match, JLine falls back
             * to its normal multi-match handling (common-prefix
             * completion / listing), same as the existing
             * command completer already does for word 0.
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
