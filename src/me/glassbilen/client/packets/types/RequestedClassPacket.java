package me.glassbilen.client.packets.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import me.glassbilen.client.Main;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class RequestedClassPacket implements Packet {

	@Override
	public String getPacketName() {
		return "RequestedClass";
	}

	@Override
	public int getArgsWanted() {
		return 2;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		String className = args[0];
		String content = args[1];

		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			try {
				stream.write(Base64.getDecoder().decode(content));
				content = "";
			} catch (IllegalArgumentException e) {
				if (Main.DEBUG_MODE) {
					e.printStackTrace();
				}

				System.exit(0);
			}

			Main.loader.cacheClass(className, stream.toByteArray());
		} catch (IOException e) {
			if (Main.DEBUG_MODE) {
				e.printStackTrace();
			}

			System.exit(0);
		}
	}
}
