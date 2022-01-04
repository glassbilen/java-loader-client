package me.glassbilen.client.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import me.glassbilen.client.Main;
import me.glassbilen.io.text.TextColor;
import me.glassbilen.network.exceptions.MaliciousPacketException;
import me.glassbilen.network.exceptions.NoPacketHandlerException;

public class LogUtils {
	public LogUtils() {}

	public static void log(Throwable throwable) {
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		String toSend = writer.toString();

		if (throwable instanceof MaliciousPacketException) {
			MaliciousPacketException packet = (MaliciousPacketException) throwable;

			toSend = packet.getMessage() + "\n" + toSend;
		} else if (throwable instanceof NoPacketHandlerException) {
			NoPacketHandlerException packet = (NoPacketHandlerException) throwable;

			toSend = "Missing packet: " + packet.getMessage() + ".\n" + toSend;
		}

		try {
			Main.networkManager.sendLine("LogError", toSend);
		} catch (IOException e) {
			if (Main.DEBUG_MODE) {
				Main.output.println(TextColor.RED + toSend);
			}
		}
	}
}
