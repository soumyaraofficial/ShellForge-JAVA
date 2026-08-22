import java.nio.file.Path;

public class CommandExecutor {

    public Path execute(String input, Path currentDirectory) throws Exception {

        String[] parts = input.split(" ");
        String command = parts[0];

        if (command.equals("exit")) {
            System.exit(0);
        }

        if (command.equals("echo")) {
            System.out.println(input.substring(5));
            return currentDirectory;
        }

        if (command.equals("pwd")) {
            System.out.println(currentDirectory);
            return currentDirectory;
        }

        if (command.equals("type")) {
            System.out.println(parts[1] + type(parts[1]));
            return currentDirectory;
        }

        if (command.equals("cd")) {
            return changeDirectory(parts[1], currentDirectory);
        }

        String commandPath = getCommandPath(command);

        if (commandPath != null) {

            Process process = new ProcessBuilder(parts)
                    .directory(currentDirectory.toFile())
                    .inheritIO()
                    .start();

            process.waitFor();

            return currentDirectory;
        }

        System.out.println(command + ": command not found");

        return currentDirectory;
    }

    private Path changeDirectory(String directory, Path currentDirectory) {

        Path newDirectory = currentDirectory
                .resolve(directory)
                .normalize();

        if (!newDirectory.toFile().isDirectory()) {
            System.out.println("cd: " + directory + ": No such file or directory");
            return currentDirectory;
        }

        return newDirectory.toAbsolutePath();
    }

    private String type(String command) {

        if (command.equals("exit")
                || command.equals("echo")
                || command.equals("pwd")
                || command.equals("type")
                || command.equals("cd")) {

            return " is a shell builtin";
        }

        String path = getCommandPath(command);

        if (path != null) {
            return " is " + path;
        }

        return ": not found";
    }

    private String getCommandPath(String command) {

        String path = System.getenv("PATH");

        for (String directory : path.split(java.io.File.pathSeparator)) {

            java.io.File file = new java.io.File(directory, command);

            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}