package me.glassbilen.client.packets.types;

import me.glassbilen.client.Main;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class LogConfirmedPacket implements Packet {

	@Override
	public String getPacketName() {
		return "LogConfirmed";
	}

	@Override
	public int getArgsWanted() {
		return 0;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		Main.networkManager.close();
	}
}
