package me.glassbilen.client.packets.types;

import javax.swing.JOptionPane;

import me.glassbilen.client.Main;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class ClosePacket implements Packet {

	@Override
	public String getPacketName() {
		return "Close";
	}

	@Override
	public int getArgsWanted() {
		return 2;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		Main.showMessage(args[0], args[1], JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}
}
