import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = sc.nextLine();
            if (command.equals("exit")) {
                break;
            } else if (command.substring(0, 4).equals("echo")) {

                System.out.println(command.substring(5, command.length()));
                
            } else if (command.substring(0, 4).equals("type")) {
                String search = command.substring(5, command.length());
                System.out.println(search + type(search));

            } else {
                System.out.println(command + ": command not found");
            }
        }
    }

    public static String type(String command){
      
        String[] builin = {"exit","echo", "type"};
        String path = System.getenv("PATH");
        String[] pathDir = path.split(":");
        for(String s : builin){
            if(s.equals(command))return " is a shell builtin";
        }

        for(String dir : pathDir){
           File file = new File(dir, command);
           if(file.exists() && file.canExecute()){
            return " is " + dir;
           }
        }
        return ": not found";
    }
}
