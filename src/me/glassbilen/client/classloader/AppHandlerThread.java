package me.glassbilen.client.classloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.glassbilen.client.Main;
import me.glassbilen.client.logging.LogUtils;

public class AppHandlerThread extends Thread {
	private String mainClass;

	public AppHandlerThread(String mainClass) {
		this.mainClass = mainClass;
	}

	@Override
	public void run() {
		try {
			Class<?> loadedClazz = Main.loader.loadClass(mainClass);
			Method main = loadedClazz.getMethod("main", new Class<?>[] { new String[0].getClass() });
			Object[] argsArray = { Main.args };

			main.invoke(null, argsArray);

			if (Main.PROGRAM.shouldExitAfterFinish()) {
				Main.networkManager.close();
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			LogUtils.log(e);
			Main.showMessage("Failed to start program.", "Failed to load program, contact the author if this problem persists.");
		} catch (UnsupportedClassVersionError e) {
			LogUtils.log(e);
			Main.showMessage("App is not updated.", "You need to either update your java version or ask the developer to compile its libraries or program with a older java version.");
		}
	}
}
