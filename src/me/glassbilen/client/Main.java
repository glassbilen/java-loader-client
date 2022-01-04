package me.glassbilen.client;

import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import me.glassbilen.client.classloader.MemoryClassLoader;
import me.glassbilen.client.logging.LogUtils;
import me.glassbilen.client.packets.PacketManager;
import me.glassbilen.common.security.SecuredApp;
import me.glassbilen.io.text.InputHandler;
import me.glassbilen.io.text.OutputHandler;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.exceptions.MaliciousPacketException;
import me.glassbilen.network.exceptions.NoPacketHandlerException;

public class Main {
	private static String SERVER_IP = "127.0.0.1";
	private static int SERVER_PORT = 20692;
	public static boolean DEBUG_MODE = true;
	// abcdefghijklmnopqrstuvw, placeholder string so it can be more easily
	// bytepatched.
	public static final SecuredApp PROGRAM = new SecuredApp("abcdefghijklmnopqrstuvw"); // 23 characters
	public static MemoryClassLoader loader;
	public static Thread appThread;
	public static String[] args;

	private Socket socket;
	private PacketManager packetManager;
	public static CNetworkManager networkManager;

	public static OutputHandler output;
	public static InputHandler input;

	public Main() {
		output = new OutputHandler(true);
		input = new InputHandler(output);

		if (DEBUG_MODE) {
			if (!input.isDebug()) {
				DEBUG_MODE = false;
			}
		}

		packetManager = new PacketManager();

		try {
			socket = new Socket(SERVER_IP, SERVER_PORT);
		} catch (IOException e) {
			showMessage("Failed to initiate!", "Failed to connect to the authentication server.");
			return;
		}

		if (DEBUG_MODE) {
			showMessage("Debug mode!", "Debug mode has been detected.");
		}

		networkManager = new CNetworkManager(socket, packetManager) {
			@Override
			public void onProcess(String line) {
				if (!networkManager.isRunning()) {
					return;
				}

				try {
					networkManager.handleDefaultPacket(line);
				} catch (MaliciousPacketException e) {
					LogUtils.log(e);
					showMessage("Program packet failed.",
							"A crucial packet failed, please contact the author of the program or try restarting the application.");
				} catch (NoPacketHandlerException e) {
					LogUtils.log(e);
				}
			}

			@Override
			public void onAuthenticate() {
				try {
					sendLine("RequestProgram", PROGRAM.getHash());
				} catch (IOException e) {
					if (Main.input.isDebug()) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onClose() {
			}
		};

		try {
			networkManager.init(true);
		} catch (IOException e) {
			showMessage("Failed to initiate!", "Failed to start connection to the authentication server.");
			return;
		}

		networkManager.start();
	}

	public static void showMessage(String title, String message, int type) {
		if (output != null && input != null && input.isDebug()) {
			output.println(title + " - " + message);
		} else {
			JFrame frame = new JFrame();
			frame.setAlwaysOnTop(true);
			frame.setLocationRelativeTo(null);
			JOptionPane.showMessageDialog(frame, message, title, type);
			frame.dispose();
		}
	}

	public static void showMessage(String title, String message) {
		showMessage(title, message, JOptionPane.PLAIN_MESSAGE);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			if (Main.input.isDebug()) {
				e.printStackTrace();
			}
		}

		Main.args = args;

		new Main();
	}
}
