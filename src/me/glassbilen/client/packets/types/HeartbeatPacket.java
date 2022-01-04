package me.glassbilen.client.packets.types;

import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class HeartbeatPacket implements Packet {

	@Override
	public String getPacketName() {
		return "Heartbeat";
	}

	@Override
	public int getArgsWanted() {
		return 0;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		manager.updateLastHeartbeat();
	}
}
