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

    public FileNameCompleter(Supplier<Path> currentDirectorySupplier) {
        this.currentDirectorySupplier = currentDirectorySupplier;
    }

    @Override
    public void complete(
            LineReader reader,
            ParsedLine line,
            List<Candidate> candidates) {

        String input = line.line();

        int lastWhitespace = findLastWhitespace(input);

        if (lastWhitespace == -1) {
            return;
        }

        String prefix = input.substring(lastWhitespace + 1);

        /*
         * Remove surrounding quotes from the prefix.
         *
         * Example:
         *
         * du 'apple-5
         *
         * becomes:
         *
         * apple-5
         */
        String searchPrefix = removeQuotes(prefix);

        if (searchPrefix.isEmpty()) {
            return;
        }

        Path currentDirectory = currentDirectorySupplier.get();

        if (currentDirectory == null) {
            return;
        }

        try (DirectoryStream<Path> entries =
                     Files.newDirectoryStream(currentDirectory)) {

            for (Path entry : entries) {

                if (!Files.isRegularFile(entry)) {
                    continue;
                }

                String fileName =
                        entry.getFileName().toString();

                if (!fileName.startsWith(searchPrefix)) {
                    continue;
                }

                /*
                 * Complete only the filename.
                 *
                 * JLine will add the trailing space because
                 * complete() is true.
                 *
                 * Do NOT put " " inside the candidate value.
                 */
                candidates.add(
                        new Candidate(
                                fileName,
                                fileName,
                                null,
                                null,
                                null,
                                null,
                                true
                        )
                );
            }

        } catch (IOException ignored) {
            // No completion candidates if the directory
            // cannot be read.
        }
    }

    private String removeQuotes(String value) {

        if (value.length() >= 2) {

            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);

            if ((first == '\'' && last == '\'')
                    || (first == '"' && last == '"')) {

                return value.substring(
                        1,
                        value.length() - 1
                );
            }
        }

        if (value.startsWith("'")
                || value.startsWith("\"")) {

            return value.substring(1);
        }

        return value;
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