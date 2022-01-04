package me.glassbilen.client.packets.types;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import me.glassbilen.client.Main;
import me.glassbilen.client.logging.LogUtils;
import me.glassbilen.file.FileHandler;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class RequestedLibraryPacket implements Packet {

	@Override
	public String getPacketName() {
		return "RequestedLibrary";
	}

	@Override
	public int getArgsWanted() {
		return 2;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		String library = args[0];
		String content = args[1];

		File file = storeCustom(library, Base64.getDecoder().decode(content));

		if (file != null) {
			Main.loader.cacheLibrary(library, file.getAbsolutePath());
		}
	}

	private File getFile(String library) {
		try {
			return File.createTempFile(UUID.randomUUID().toString().replaceAll("-", ""), System.mapLibraryName(""));
		} catch (IOException e) {
			LogUtils.log(e);
			System.exit(0);
		}

		return null;
	}

	private File storeCustom(String library, byte[] content) {
		try {
			File file = getFile(library);

			if (file.exists()) {
				if (!file.delete()) {
					file.deleteOnExit();
					return file;
				}
			}

			FileHandler handler = new FileHandler(file);
			handler.init();
			handler.writeToFile(content);
			// file.deleteOnExit();
			return file;
		} catch (IOException e) {
			LogUtils.log(e);
			System.exit(0);
		}

		return null;
	}
}
