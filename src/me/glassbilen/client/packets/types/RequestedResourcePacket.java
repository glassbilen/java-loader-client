package me.glassbilen.client.packets.types;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import me.glassbilen.client.Main;
import me.glassbilen.client.logging.LogUtils;
import me.glassbilen.client.resources.VirtualResourceStream;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class RequestedResourcePacket implements Packet {

	@Override
	public String getPacketName() {
		return "RequestedResource";
	}

	@Override
	public int getArgsWanted() {
		return 2;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		String resource = args[0];
		String content = args[1];

		try {
			URL url = new URL("", "", -1, "", new VirtualResourceStream(Base64.getDecoder().decode(content)));
			Main.loader.cacheResource(resource, url);
		} catch (IOException e) {
			LogUtils.log(e);
			System.exit(0);
		}
	}
}
