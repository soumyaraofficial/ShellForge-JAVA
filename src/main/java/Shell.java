import java.nio.file.Path;
import java.util.Scanner;

public class Shell {

    private final Scanner scanner = new Scanner(System.in);
    private final CommandExecutor executor = new CommandExecutor();



    private Path currentDirectory = Path.of("").toAbsolutePath();

    public void run() throws Exception {

        while (true) {

            System.out.print("$ ");

            String command = scanner.nextLine();

            if (command.isBlank()) {
                continue;
            }
            if(command.equalsIgnoreCase("EXIT"))break;

        currentDirectory =  executor.execute(command, currentDirectory);
        }
    }
}