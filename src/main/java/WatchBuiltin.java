import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class WatchBuiltin {

    private WatchBuiltin() {
    }

    public static void execute(Path file) {

        if (!Files.exists(file)) {
            System.out.println(
                    "watch: file not found: " + file
            );
            return;
        }

        if (!Files.isRegularFile(file)) {
            System.out.println(
                    "watch: not a file: " + file
            );
            return;
        }

        System.out.println();
        System.out.println(
                "Watching: " + file
        );
        System.out.println(
                "────────────────────────────"
        );
        System.out.println(
                "Press Ctrl+C to stop."
        );
        System.out.println();

        try {

            long position = Files.size(file);

            while (true) {

                long currentSize = Files.size(file);

                if (currentSize > position) {

                    try (var input =
                                 Files.newBufferedReader(file)) {

                        input.skip(position);

                        String line;

                        while ((line = input.readLine()) != null) {

                            System.out.println(line);
                        }
                    }

                    position = currentSize;
                }

                Thread.sleep(500);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println();
            System.out.println(
                    "Stopped watching."
            );

        } catch (IOException e) {

            System.out.println(
                    "watch: error reading file: "
                            + e.getMessage()
            );
        }
    }
}