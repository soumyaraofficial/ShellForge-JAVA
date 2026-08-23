public class Quoting {

    public void executeEcho(String command) {
        StringBuilder args = new StringBuilder();
        StringBuilder current = new StringBuilder();

        boolean inQuote = false;

        for (char c : command.toCharArray()) {

            if (c == '\'') {
                inQuote = !inQuote;
            } else if (Character.isWhitespace(c) && !inQuote) {

                if (current.length() > 0) {
                    if (args.length() > 0) {
                        args.append(" ");
                    }

                    args.append(current.toString());
                    current.setLength(0);
                }

            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            if (args.length() > 0) {
                args.append(" ");
            }

            args.append(current.toString());
        }

        System.out.println(args);
    }


    
}
