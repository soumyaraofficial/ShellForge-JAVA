import java.nio.file.Path;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

    private final CommandExecutor executor = new CommandExecutor();

    private Path currentDirectory = Path.of("").toAbsolutePath();

    public void run() throws Exception {

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        StringsCompleter completer = new StringsCompleter(
                "echo",
                "cd",
                "pwd",
                "exit");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();
        while (true) {

            String command = reader.readLine("$ ");

            if (command.isBlank()) {
                continue;
            }

            if (command.equalsIgnoreCase("exit")) {
                break;
            }

            currentDirectory = executor.execute(command, currentDirectory);
        }

        terminal.close();
    }
}
