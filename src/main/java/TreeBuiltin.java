import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TreeBuiltin {

    public static void execute(Path directory, int maxDepth) {

        System.out.println(directory.getFileName() + "/");

        printTree(directory, "", 0, maxDepth);
    }

    private static void printTree(
            Path directory,
            String prefix,
            int depth,
            int maxDepth) {

        if (depth >= maxDepth) {
            return;
        }

        try {
            List<Path> entries = Files.list(directory)
                    .filter(path -> !isIgnored(path))
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString()
                                    .toLowerCase()))
                    .collect(Collectors.toList());

            for (int i = 0; i < entries.size(); i++) {

                Path entry = entries.get(i);

                boolean last = i == entries.size() - 1;

                String branch = last ? "└── " : "├── ";

                System.out.println(
                        prefix
                                + branch
                                + entry.getFileName()
                                + (Files.isDirectory(entry) ? "/" : "")
                );

                if (Files.isDirectory(entry)) {

                    String childPrefix =
                            prefix + (last ? "    " : "│   ");

                    printTree(
                            entry,
                            childPrefix,
                            depth + 1,
                            maxDepth
                    );
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "tree: unable to read directory: "
                            + e.getMessage()
            );
        }
    }

    private static boolean isIgnored(Path path) {

        String name = path.getFileName().toString();

        return name.equals(".git")
                || name.equals("target")
                || name.equals("node_modules")
                || name.equals("build")
                || name.equals("dist")
                || name.equals(".idea");
    }
}