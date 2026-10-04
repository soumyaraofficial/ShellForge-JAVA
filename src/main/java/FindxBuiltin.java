import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class FindxBuiltin {

    private FindxBuiltin() {}

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            ".idea"
    );

    private static final Set<String> SEARCHABLE_EXTENSIONS = Set.of(
            "java",
            "js",
            "jsx",
            "ts",
            "tsx",
            "css",
            "html",
            "xml",
            "json",
            "md",
            "txt",
            "properties",
            "yml",
            "yaml",
            "sql",
            "sh"
    );

    private static final Set<String> IGNORED_FILES = Set.of(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "composer.lock",
            "Gemfile.lock"
    );

    public static void execute(Path startDirectory, String searchTerm) {

        int matches = 0;
        int filesMatched = 0;

        String search = searchTerm.toLowerCase();

        try (Stream<Path> paths = Files.walk(startDirectory)) {

            for (Path path : paths.toList()) {

                // Skip ignored directories
                if (shouldSkipDirectory(startDirectory, path)) {
                    continue;
                }

                // Only search regular files
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                // Skip lock/generated files
                if (shouldSkipFile(path)) {
                    continue;
                }

                // Only search supported source/config files
                if (!isSearchableFile(path)) {
                    continue;
                }

                try {

                    List<String> lines = Files.readAllLines(path);

                    boolean fileMatched = false;

                    for (int i = 0; i < lines.size(); i++) {

                        String line = lines.get(i);

                        if (line.toLowerCase().contains(search)) {

                            // Print filename only once
                            if (!fileMatched) {

                                if (filesMatched > 0) {
                                    System.out.println();
                                }

                                System.out.println(
                                        startDirectory
                                                .relativize(path)
                                                .toString()
                                );

                                fileMatched = true;
                                filesMatched++;
                            }

                            System.out.println(
                                    "  " + (i + 1) + ": " + line
                            );

                            matches++;
                        }
                    }

                } catch (IOException ignored) {
                    // Ignore files that cannot be read
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "findx: unable to search directory"
            );

            return;
        }

        System.out.println();
        System.out.println("────────────────────────────");

        System.out.println(
                matches
                        + " match"
                        + (matches == 1 ? "" : "es")
                        + " in "
                        + filesMatched
                        + " file"
                        + (filesMatched == 1 ? "" : "s")
        );
    }

    private static boolean shouldSkipDirectory(
            Path startDirectory,
            Path path
    ) {

        Path relativePath = startDirectory.relativize(path);

        for (Path part : relativePath) {

            String name = part.toString();

            if (IGNORED_DIRECTORIES.contains(name)) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldSkipFile(Path path) {

        String fileName = path.getFileName().toString();

        return IGNORED_FILES.contains(fileName);
    }

    private static boolean isSearchableFile(Path path) {

        String fileName = path.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');

        // Files without extensions are ignored
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension =
                fileName.substring(dotIndex + 1).toLowerCase();

        return SEARCHABLE_EXTENSIONS.contains(extension);
    }
}