/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.pifaceintegrator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.badvision.pifaceintegrator.piface.LocalConnection;
import org.badvision.pifaceintegrator.piface.MockConnection;
import org.badvision.pifaceintegrator.piface.PifaceConnection;
import org.badvision.pifaceintegrator.piface.RestClient;
import org.badvision.pifaceintegrator.piface.RestServer;
import org.badvision.pifaceintegrator.scratch.Constants;

/**
 *
 * @author blurry
 */
public class Launcher {

    static Logger log = Logger.getLogger(Launcher.class.getName());

    public static void main(String... args) throws IOException {
        OptionParser parser = new OptionParser("m:p:r:h?");
        parser.accepts("m", "mesh host:port, implies --mesh").withRequiredArg().ofType(String.class);
        parser.accepts("r", "remote piface server host:port, implies --client").withRequiredArg().ofType(String.class);
        parser.accepts("p", "server port, implies --server").withRequiredArg().ofType(Integer.class);
        parser.acceptsAll(Arrays.asList(new String[]{"?", "h"}), "Show help (this message)");
        parser.accepts("gui", "Operate in GUI mode (default)");
        parser.accepts("text", "Operate in Text mode");
        parser.accepts("server", "Start server, assumes port 1701 unless otherwise specified with -p");
        parser.accepts("client", "Connect to remote server, assumes localhost:1701 unless otherwise specified with -r");
        parser.accepts("local", "Connect to piface on this device (default)");
        parser.accepts("mock", "Connect to mock (fake) piface used for testing");
        parser.accepts("nomesh", "Do not connect to mesh");
        parser.accepts("mesh", "Connect to mesh (default), assumes localhost:42001 unless otherwise specified with -m");
        OptionSet options = parser.parse(args);
        if (offerHelp(options)) {
            System.out.println("PiFace Integrator");
            System.out.println("Usage: Specify a UI (text, gui) and a connection mode (local, client, mock.)\n"
                    + "  Optionally, a server mode can be enabled for allowing remote connectivity.");
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        UserInterface ui = options.has("text") ? setupTextMode(options) : setupGuiMode(options);

        PifaceController pifaceController = new PifaceController(buildPifaceConnection(options));
        pifaceController.connectToUI(ui);
        buildMeshController(options).ifPresent(mesh -> {
            mesh.connectToUI(ui);
            try {
                mesh.connectToPiface(pifaceController);
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        });

        buildServerConnection(options, pifaceController).ifPresent(server
                -> ui.addShutdownHook(server::shutdown));

        ui.mainLoop();
    }

    private static boolean offerHelp(OptionSet options) {
        return (options.has("h") || options.has("?"));
    }

    private static UserInterface setupTextMode(OptionSet options) {
        return new TextApp();
    }

    private static UserInterface setupGuiMode(OptionSet options) {
        new Thread(() -> Application.launch(GuiApp.class, new String[0])).start();
        while (GuiApp.ui == null) {
            Thread.yield();
        }
        return GuiApp.ui;
    }

    private static PifaceConnection buildPifaceConnection(OptionSet options) throws IOException {
        // First determine if this is local, client or mock
        PifaceConnection piface = buildClientPifaceConnection(options).orElse(
                buildMockPifaceConnection(options).orElse(null));
        if (piface == null) {
            return buildLocalPifaceConnection(options);
        }
        return piface;
    }

    private static PifaceConnection buildLocalPifaceConnection(OptionSet options) throws IOException {
        return new LocalConnection();
    }

    private static Optional<PifaceConnection> buildClientPifaceConnection(OptionSet options) {
        if (options.has("client") || options.hasArgument("r")) {
            String host = "localhost";
            int port = RestServer.DEFAULT_PORT;
            if (options.hasArgument("r")) {
                String[] parts = String.valueOf(options.valueOf("r")).split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }
            System.out.println("using client connection to "+host+", port "+port);
            return Optional.of(new RestClient(host, port));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<PifaceConnection> buildMockPifaceConnection(OptionSet options) {
        if (options.has("mock")) {
            System.out.println("using mock");
           return Optional.of(new MockConnection());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<RestServer> buildServerConnection(OptionSet options, PifaceConnection piface) throws IOException {
        if (options.has("server") || options.hasArgument("p")) {
            int port = RestServer.DEFAULT_PORT;
            if (options.hasArgument("p")) {
                port = (Integer) options.valueOf("p");
            }
            System.out.println("hosting on port "+port);
            return Optional.of(new RestServer(port, piface));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<MeshController> buildMeshController(OptionSet options) throws IOException {
        if (!options.has("nomesh")) {
            String host = Constants.SCRATCH_DEFAULT_HOST;
            int port = Constants.SCRATCH_DEFAULT_PORT;
            if (options.hasArgument("m")) {
                String[] parts = String.valueOf(options.valueOf("m")).split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }
            System.out.println("using scratch mesh connection to "+host+", port "+port);
            return Optional.of(new MeshController(host, port));
        } else {
            return Optional.empty();
        }
    }

}
