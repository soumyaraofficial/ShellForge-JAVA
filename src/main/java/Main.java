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
            } else if (command.substring(0, 4).equals("type")){
               
                String type = command.substring(5, command.length());
                switch (type) {
                    case "echo":
                        System.out.println(type +" is a shell builtin");
                        break;
                    case "exit":
                        System.out.println(type +" is a shell builtin");
                        break;
                    case "type":
                        System.out.println(type +" is a shell builtin");
                        break;
                    default:
                        System.out.println(type +" : not found");
                        break;
                }
               // System.out.println(type +" is a shell builtin");

            } else {
                System.out.println(command + ": command not found");
            }
        }
    }
}
