package me.glassbilen.client.packets.types;

import me.glassbilen.client.Main;
import me.glassbilen.client.classloader.AppHandlerThread;
import me.glassbilen.client.classloader.MemoryClassLoader;
import me.glassbilen.common.security.SecurityLevel;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class ProgramDataPacket implements Packet {

	@Override
	public String getPacketName() {
		return "ProgramData";
	}

	@Override
	public int getArgsWanted() {
		return 3;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		if (Main.PROGRAM.isReady()) {
			return;
		}

		SecurityLevel level = SecurityLevel.valueOf(args[0]);
		boolean exitAfterExecution = Boolean.parseBoolean(args[1]);

		if (level == null) {
			System.exit(0);
			return;
		}

		Main.PROGRAM.setLevel(level);
		Main.PROGRAM.setExitAfterFinish(exitAfterExecution);

		String mainClass = args[2];

		Main.loader = new MemoryClassLoader();
		Main.appThread = new AppHandlerThread(mainClass);
		Main.appThread.start();
	}
}