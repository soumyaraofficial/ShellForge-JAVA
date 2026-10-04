import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebServer extends WebSocketServer {

    private static PtyProcess shellProcess;
    private static OutputStream shellInput;
    private static WebServer serverInstance;

    public WebServer(int port) {
        super(new InetSocketAddress(port));
    }

    public static void main(String[] args) throws Exception {

        // Start the existing ShellForge shell inside a PTY
        startShell();

        // Start WebSocket server
        serverInstance = new WebServer(8080);
        serverInstance.start();

        System.out.println(
                "ShellForge WebSocket server running on ws://localhost:8080"
        );
    }

    private static void startShell() throws Exception {

        String javaCommand =
                System.getProperty("java.home")
                        + "/bin/java";

        String jarPath =
                "target/shellforge-java.jar";

        /*
         * Start the existing ShellForge shell.
         *
         * We are NOT changing Shell.java
         * or CommandExecutor.java.
         */
        PtyProcessBuilder builder =
                new PtyProcessBuilder(
                        new String[]{
                                javaCommand,
                                "--enable-native-access=ALL-UNNAMED",
                                "-jar",
                                jarPath
                        }
                );

        /*
         * Keep the existing environment.
         *
         * Add terminal information for JLine.
         */
        Map<String, String> environment =
                new HashMap<>(System.getenv());

        environment.put(
                "TERM",
                "xterm-256color"
        );

        environment.put(
                "COLORTERM",
                "truecolor"
        );

        builder.setEnvironment(environment);

        /*
         * Run ShellForge from the project directory.
         */
        builder.setDirectory(
                System.getProperty("user.dir")
        );

        /*
         * Start the shell inside the PTY.
         */
        shellProcess = builder.start();

        /*
         * Give the PTY an initial terminal size.
         *
         * This is important for JLine because
         * it uses the terminal dimensions when
         * positioning and redrawing the command line.
         */
        shellProcess.setWinSize(
                new WinSize(120, 40)
        );

        /*
         * Input going INTO the Java shell.
         */
        shellInput =
                shellProcess.getOutputStream();

        /*
         * Output coming FROM the Java shell.
         */
        InputStream shellOutput =
                shellProcess.getInputStream();

        /*
         * Continuously read shell output and
         * forward it to the browser.
         */
        Thread readerThread = new Thread(() -> {

            byte[] buffer = new byte[4096];

            try {

                int bytesRead;

                while ((bytesRead =
                        shellOutput.read(buffer)) != -1) {

                    String output =
                            new String(
                                    buffer,
                                    0,
                                    bytesRead,
                                    StandardCharsets.UTF_8
                            );

                    /*
                     * Send RAW terminal output.
                     *
                     * Do NOT remove ANSI sequences.
                     * xterm.js interprets them.
                     */
                    if (serverInstance != null) {

                        for (WebSocket connection :
                                serverInstance.getConnections()) {

                            if (connection.isOpen()) {

                                connection.send(output);
                            }
                        }
                    }
                }

            } catch (IOException e) {

                System.out.println(
                        "Shell process disconnected."
                );
            }

        });

        readerThread.setDaemon(true);
        readerThread.start();
    }

    /*
     * Browser connected.
     */
    @Override
    public void onOpen(
            WebSocket connection,
            ClientHandshake handshake
    ) {

        System.out.println(
                "Browser connected: "
                        + connection.getRemoteSocketAddress()
        );
    }

    /*
     * Browser disconnected.
     */
    @Override
    public void onClose(
            WebSocket connection,
            int code,
            String reason,
            boolean remote
    ) {

        System.out.println(
                "Browser disconnected."
        );
    }

    /*
     * Keyboard input from xterm.js.
     *
     * Pass the input directly to the PTY.
     */
    @Override
    public void onMessage(
            WebSocket connection,
            String message
    ) {

        try {

            shellInput.write(
                    message.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            shellInput.flush();

        } catch (IOException e) {

            if (connection.isOpen()) {

                connection.send(
                        "\r\nShell input error: "
                                + e.getMessage()
                                + "\r\n"
                );
            }
        }
    }

    /*
     * WebSocket error.
     */
    @Override
    public void onError(
            WebSocket connection,
            Exception exception
    ) {

        exception.printStackTrace();
    }

    /*
     * WebSocket server started.
     */
    @Override
    public void onStart() {

        System.out.println(
                "WebSocket server started."
        );
    }
}