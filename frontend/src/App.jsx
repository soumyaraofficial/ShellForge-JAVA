import { useEffect, useRef, useState } from "react";
import { Terminal } from "@xterm/xterm";
import { FitAddon } from "@xterm/addon-fit";


import "@xterm/xterm/css/xterm.css";
import "./App.css";

const WS_URL = "ws://localhost:8080";

const retroTheme = {
  background: "rgba(0,0,0,0)", // let the screen gradient show through
  foreground: "#ffb000",

  cursor: "#ffd060",
  cursorAccent: "#120a00",
  selectionBackground: "rgba(255, 176, 0, 0.35)",

  black: "#120a00",
  red: "#ff6a3d",
  green: "#ffb000",
  yellow: "#ffd060",
  blue: "#d98a00",
  magenta: "#ff9a52",
  cyan: "#ffc870",
  white: "#ffcf80",

  brightBlack: "#6b4a00",
  brightRed: "#ff8a5c",
  brightGreen: "#ffc933",
  brightYellow: "#ffe08a",
  brightBlue: "#f0a020",
  brightMagenta: "#ffb070",
  brightCyan: "#ffdca0",
  brightWhite: "#fff0cc",
};

const LINK_LABEL = {
  connecting: "LINKING",
  online: "ONLINE",
  offline: "OFFLINE",
};

function formatUptime(totalSeconds) {
  const h = String(Math.floor(totalSeconds / 3600)).padStart(2, "0");
  const m = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, "0");
  const s = String(totalSeconds % 60).padStart(2, "0");
  return `${h}:${m}:${s}`;
}

function App() {
  const terminalRef = useRef(null);
  const xtermRef = useRef(null);

  const [link, setLink] = useState("connecting"); // connecting | online | offline
  const [size, setSize] = useState({ cols: 0, rows: 0 });
  const [seconds, setSeconds] = useState(0);

  // Session timer: counts while the link is online.
  useEffect(() => {
    if (link !== "online") return;
    setSeconds(0);
    const id = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, [link]);

  useEffect(() => {
    let disposed = false;
    let cleanup = () => {};

    (async () => {
      // Load the font BEFORE xterm measures character cells,
      // otherwise cols/rows are computed wrong.
      await document.fonts.load('20px "VT323"');
      await document.fonts.ready;
      if (disposed) return;

      const terminal = new Terminal({
        cursorBlink: true,
        disableStdin: false,
        convertEol: true,

        fontFamily: '"VT323", "Courier New", monospace',
        fontSize: 20,
        lineHeight: 1.15,
        letterSpacing: 0,

        cursorStyle: "block",
        cursorWidth: 2,

        scrollback: 5000,
        allowTransparency: true,
        theme: retroTheme,
      });
      xtermRef.current = terminal;

      const fitAddon = new FitAddon();
      terminal.loadAddon(fitAddon);
      terminal.open(terminalRef.current);
      terminal.focus();

      const sizeDisposable = terminal.onResize(({ cols, rows }) =>
        setSize({ cols, rows })
      );

      requestAnimationFrame(() => {
        if (disposed) return;
        fitAddon.fit();
        setSize({ cols: terminal.cols, rows: terminal.rows });
      });

      // Debounced to one fit per animation frame.
      let raf = 0;
      const resizeObserver = new ResizeObserver(() => {
        cancelAnimationFrame(raf);
        raf = requestAnimationFrame(() => {
          if (!disposed) fitAddon.fit();
        });
      });
      resizeObserver.observe(terminalRef.current);

      const socket = new WebSocket(WS_URL);
      let gotOutput = false;
      let nudgeTimer = 0;

      socket.onopen = () => {
        setLink("online");

        terminal.write(
          `\x1b[38;5;214m●\x1b[0m \x1b[38;5;130mconnected to ${WS_URL}\x1b[0m\r\n\r\n`
        );

        // If the shell's first prompt was missed, nudge it to print one.
        nudgeTimer = setTimeout(() => {
          if (!gotOutput && socket.readyState === WebSocket.OPEN) {
            socket.send("\r");
          }
        }, 400);
      };

      socket.onmessage = (event) => {
        gotOutput = true;
        terminal.write(event.data);
      };

      socket.onerror = () => {
        setLink("offline");
        terminal.write(
          "\r\n\x1b[38;5;203m[ WebSocket connection error ]\x1b[0m\r\n"
        );
      };

      socket.onclose = () => {
        setLink("offline");
        terminal.write(
          "\r\n\x1b[38;5;203m[ ShellForge disconnected ]\x1b[0m\r\n"
        );
      };

      // Send keyboard input directly to Java PTY
      const inputDisposable = terminal.onData((data) => {
        if (socket.readyState === WebSocket.OPEN) {
          socket.send(data);
        }
      });

      cleanup = () => {
        clearTimeout(nudgeTimer);
        cancelAnimationFrame(raf);
        resizeObserver.disconnect();
        sizeDisposable.dispose();
        inputDisposable.dispose();
        socket.close();
        terminal.dispose();
        xtermRef.current = null;
      };
    })();

    return () => {
      disposed = true;
      cleanup();
    };
  }, []);

  return (
    <div className="app" data-link={link}>
      <div className="frame">
        {/* Masthead */}
        <header className="masthead">
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">
              &gt;_
            </span>
            <div>
              <h1 className="brand-name">
                SHELLFORGE
                <span className="brand-tag">// AI CORE //</span>
              </h1>
              <p className="brand-sub">CUSTOM SHELL // LOCAL AI RUNTIME</p>
            </div>
          </div>

          <dl className="readouts">
            <div className="readout">
              <dt>LINK</dt>
              <dd>
                <span className="led" />
                {LINK_LABEL[link]}
              </dd>
            </div>
            <div className="readout">
              <dt>PTY</dt>
              <dd>{size.cols ? `${size.cols}×${size.rows}` : "--×--"}</dd>
            </div>
            <div className="readout">
              <dt>UPTIME</dt>
              <dd>{formatUptime(seconds)}</dd>
            </div>
          </dl>
        </header>

        {/* Screen */}
        <main className="screen-wrap">
          <div className="sub-bar">
            <span>shellforge@local</span>
            <span>{WS_URL}</span>
          </div>

          <div
            className="terminal-container"
            onClick={() => xtermRef.current?.focus()}
          >
            <div ref={terminalRef} className="terminal" />
          </div>
        </main>

        {/* Status bar */}
        <footer className="statusbar">
          <span>JAVA // PTY // WEBSOCKET</span>
          <span>UTF-8 // XTERM.JS</span>
        </footer>
      </div>
    </div>
  );
}

export default App;
