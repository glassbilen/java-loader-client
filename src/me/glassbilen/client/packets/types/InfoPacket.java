package me.glassbilen.client.packets.types;

import javax.swing.JOptionPane;

import me.glassbilen.client.Main;
import me.glassbilen.network.CNetworkManager;
import me.glassbilen.network.Packet;

public class InfoPacket implements Packet {

	@Override
	public String getPacketName() {
		return "Info";
	}

	@Override
	public int getArgsWanted() {
		return 2;
	}

	@Override
	public void onReceive(CNetworkManager manager, String[] args) {
		Main.showMessage(args[0], args[1], JOptionPane.INFORMATION_MESSAGE);
	}
}
