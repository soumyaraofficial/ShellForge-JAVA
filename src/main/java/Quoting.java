import java.util.ArrayList;
import java.util.List;

public class Quoting {

    public List<String> parser(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inQuote = false;

        for (char c : command.toCharArray()) {

            if (c == '\'') {
                inQuote = !inQuote;
            } else if (Character.isWhitespace(c) && !inQuote) {

                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }

            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }
        return args;
    }

    
    public void executeEcho(List<String> commandSplit) {
        StringBuilder string = new StringBuilder();
    
        for (int i = 1; i < commandSplit.size(); i++) {
            if (i > 1) {
                string.append(" ");
            }
    
            string.append(commandSplit.get(i));
        }
    
        System.out.println(string);
    }

}
