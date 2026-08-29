import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class FileNameCompleter implements Completer {

    private final Supplier<Path> currentDirectorySupplier;

    public FileNameCompleter(
            Supplier<Path> currentDirectorySupplier) {

        this.currentDirectorySupplier =
                currentDirectorySupplier;
    }

    @Override
    public void complete(
            LineReader reader,
            ParsedLine line,
            List<Candidate> candidates) {

        String input = line.line();

        /*
         * Filename completion only applies after
         * the command name.
         *
         * Example:
         *
         * cat re
         *     ^^
         *
         * The last whitespace separates the command
         * from the filename being completed.
         */
        int lastWhitespace = findLastWhitespace(input);

        if (lastWhitespace == -1) {
            return;
        }

        String prefix =
                input.substring(lastWhitespace + 1);

        /*
         * Nothing to complete.
         *
         * We don't want TAB after "cat " to dump
         * every file in the directory.
         */
        if (prefix.isEmpty()) {
            return;
        }

        Path currentDirectory =
                currentDirectorySupplier.get();

        if (currentDirectory == null) {
            return;
        }

        try (DirectoryStream<Path> entries =
                     Files.newDirectoryStream(currentDirectory)) {

            for (Path entry : entries) {

                /*
                 * This stage requires filename completion,
                 * so only regular files are considered.
                 */
                if (!Files.isRegularFile(entry)) {
                    continue;
                }

                String fileName =
                        entry.getFileName().toString();

                if (!fileName.startsWith(prefix)) {
                    continue;
                }

                /*
                 * Candidate.value replaces the current
                 * word being completed.
                 *
                 * Example:
                 *
                 * input:      cat re
                 * candidate:  readme.txt
                 *
                 * becomes:
                 *
                 * cat readme.txt
                 *
                 * The trailing space is intentional.
                 */
                candidates.add(
                        new Candidate(fileName + " ")
                );
            }

        } catch (IOException ignored) {
            /*
             * If the directory cannot be read, there is
             * simply nothing to complete.
             */
        }
    }

    private int findLastWhitespace(String input) {

        for (int i = input.length() - 1; i >= 0; i--) {

            if (Character.isWhitespace(input.charAt(i))) {
                return i;
            }
        }

        return -1;
    }
}