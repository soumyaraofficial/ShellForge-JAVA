import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.stream.Stream;

public final class InspectBuiltin {

    private InspectBuiltin() {}

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            ".idea"
    );

    public static void execute(Path path) {

        if (!Files.exists(path)) {
            System.out.println(
                    "inspect: file or directory not found: " + path
            );
            return;
        }

        try {

            if (Files.isDirectory(path)) {
                inspectDirectory(path);
            } else if (Files.isRegularFile(path)) {
                inspectFile(path);
            } else {
                System.out.println(
                        "inspect: unsupported file type: " + path
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "inspect: unable to inspect: " + path
            );
        }
    }

    private static void inspectFile(Path path) throws IOException {

        long size = Files.size(path);
        long lines = 0;

        try (Stream<String> stream = Files.lines(path)) {
            lines = stream.count();
        } catch (Exception ignored) {
            // Some binary files cannot be read as text
        }

        FileTime modified = Files.getLastModifiedTime(path);

        String fileName = path.getFileName().toString();

        String type = detectFileType(fileName);

        System.out.println();
        System.out.println("Inspect");
        System.out.println("────────────────────────────");

        System.out.println("Path       : " + path);
        System.out.println("Type       : " + type);
        System.out.println("Size       : " + formatSize(size));
        System.out.println("Lines      : " + lines);
        System.out.println("Modified   : " + modified);

        System.out.println("────────────────────────────");
        System.out.println();
    }

    private static void inspectDirectory(Path path) throws IOException {

        int files = 0;
        int directories = 0;

        long totalSize = 0;

        int javaFiles = 0;
        int jsFiles = 0;
        int jsxFiles = 0;
        int tsFiles = 0;
        int tsxFiles = 0;
        int cssFiles = 0;
        int htmlFiles = 0;
        int jsonFiles = 0;
        int otherFiles = 0;

        try (Stream<Path> paths = Files.walk(path)) {

            for (Path current : paths.toList()) {

                if (current.equals(path)) {
                    continue;
                }

                /*
                 * Ignore dependency/build directories.
                 */
                if (shouldSkipDirectory(path, current)) {
                    continue;
                }

                if (Files.isDirectory(current)) {

                    directories++;

                } else if (Files.isRegularFile(current)) {

                    files++;

                    try {
                        totalSize += Files.size(current);
                    } catch (IOException ignored) {
                    }

                    String fileName =
                            current.getFileName()
                                    .toString()
                                    .toLowerCase();

                    if (fileName.endsWith(".java")) {
                        javaFiles++;
                    } else if (fileName.endsWith(".js")) {
                        jsFiles++;
                    } else if (fileName.endsWith(".jsx")) {
                        jsxFiles++;
                    } else if (fileName.endsWith(".ts")) {
                        tsFiles++;
                    } else if (fileName.endsWith(".tsx")) {
                        tsxFiles++;
                    } else if (fileName.endsWith(".css")) {
                        cssFiles++;
                    } else if (fileName.endsWith(".html")) {
                        htmlFiles++;
                    } else if (fileName.endsWith(".json")) {
                        jsonFiles++;
                    } else {
                        otherFiles++;
                    }
                }
            }
        }

        FileTime modified = Files.getLastModifiedTime(path);

        System.out.println();
        System.out.println("Inspect");
        System.out.println("────────────────────────────");

        System.out.println("Path       : " + path);
        System.out.println("Type       : Directory");

        System.out.println();

        System.out.println("Contents");
        System.out.println("────────────────────────────");

        System.out.println("Files      : " + files);
        System.out.println("Directories: " + directories);
        System.out.println("Total Size : " + formatSize(totalSize));

        System.out.println();

        System.out.println("File Types");
        System.out.println("────────────────────────────");

        if (javaFiles > 0) {
            System.out.println("Java       : " + javaFiles);
        }

        if (jsFiles > 0) {
            System.out.println("JavaScript : " + jsFiles);
        }

        if (jsxFiles > 0) {
            System.out.println("JSX        : " + jsxFiles);
        }

        if (tsFiles > 0) {
            System.out.println("TypeScript : " + tsFiles);
        }

        if (tsxFiles > 0) {
            System.out.println("TSX        : " + tsxFiles);
        }

        if (cssFiles > 0) {
            System.out.println("CSS        : " + cssFiles);
        }

        if (htmlFiles > 0) {
            System.out.println("HTML       : " + htmlFiles);
        }

        if (jsonFiles > 0) {
            System.out.println("JSON       : " + jsonFiles);
        }

        if (otherFiles > 0) {
            System.out.println("Other      : " + otherFiles);
        }

        System.out.println();

        System.out.println("Modified   : " + modified);

        System.out.println("────────────────────────────");
        System.out.println();
    }

    private static boolean shouldSkipDirectory(
            Path root,
            Path path
    ) {

        Path relativePath = root.relativize(path);

        for (Path part : relativePath) {

            String name = part.toString();

            if (IGNORED_DIRECTORIES.contains(name)) {
                return true;
            }
        }

        return false;
    }

    private static String detectFileType(String fileName) {

        String lower = fileName.toLowerCase();

        if (lower.endsWith(".java")) {
            return "Java Source File";
        }

        if (lower.endsWith(".js")) {
            return "JavaScript File";
        }

        if (lower.endsWith(".jsx")) {
            return "React JSX File";
        }

        if (lower.endsWith(".ts")) {
            return "TypeScript File";
        }

        if (lower.endsWith(".tsx")) {
            return "React TSX File";
        }

        if (lower.endsWith(".css")) {
            return "CSS File";
        }

        if (lower.endsWith(".html")) {
            return "HTML File";
        }

        if (lower.endsWith(".json")) {
            return "JSON File";
        }

        if (lower.endsWith(".xml")) {
            return "XML File";
        }

        if (lower.endsWith(".md")) {
            return "Markdown File";
        }

        if (lower.endsWith(".yml")
                || lower.endsWith(".yaml")) {
            return "YAML File";
        }

        if (lower.endsWith(".sql")) {
            return "SQL File";
        }

        return "File";
    }

    private static String formatSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    "%.2f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }
}