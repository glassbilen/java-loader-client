package me.glassbilen.client.packets;

import me.glassbilen.client.packets.types.ClosePacket;
import me.glassbilen.client.packets.types.HeartbeatPacket;
import me.glassbilen.client.packets.types.InfoPacket;
import me.glassbilen.client.packets.types.LogConfirmedPacket;
import me.glassbilen.client.packets.types.ProgramDataPacket;
import me.glassbilen.client.packets.types.RequestedClassPacket;
import me.glassbilen.client.packets.types.RequestedLibraryPacket;
import me.glassbilen.client.packets.types.RequestedResourcePacket;
import me.glassbilen.network.CPacketManager;

public class PacketManager extends CPacketManager {
	public PacketManager() {
		addPacket(new ClosePacket());
		addPacket(new HeartbeatPacket());
		addPacket(new InfoPacket());
		addPacket(new LogConfirmedPacket());
		addPacket(new ProgramDataPacket());
		addPacket(new RequestedClassPacket());
		addPacket(new RequestedLibraryPacket());
		addPacket(new RequestedResourcePacket());
	}
}
