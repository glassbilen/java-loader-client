package me.glassbilen.client.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import me.glassbilen.client.Main;
import me.glassbilen.client.logging.LogUtils;
import sun.net.www.ParseUtil;

// Class contains custom implementation of a lot of functions, some are modified versions of their original, thus the weird name format and some variables / code.
public class MemoryClassLoader extends ClassLoader {
	private Map<String, byte[]> classes;
	private Map<String, URL> resources;
	private Map<String, String> libraries;
	
	// Variables directly copied from public JDK source.
	private String[] usr_paths;
	private String[] sys_paths;
	private Constructor<Package> packageClassNonMani;
	private Constructor<Package> packageClassMani;
	private Method getPackage;
	private Method getSystemPackage0;
	private Map<String, URL> urls;
	private Map<String, Manifest> mans;
	private final Map<String, Package> packages;

	private final ClassLoader parent = "".getClass().getClassLoader();

	public MemoryClassLoader() {
		super(null);

		try {
			packageClassNonMani = Package.class.getDeclaredConstructor(String.class, String.class, String.class,
					String.class, String.class, String.class, String.class, URL.class, ClassLoader.class);
			packageClassNonMani.setAccessible(true);

			packageClassMani = Package.class
					.getDeclaredConstructor(new Class[] { String.class, Manifest.class, URL.class, ClassLoader.class });
			packageClassMani.setAccessible(true);

			getPackage = ClassLoader.class.getDeclaredMethod("getPackage", new Class[] { String.class });
			getPackage.setAccessible(true);

			getSystemPackage0 = Package.class.getDeclaredMethod("getSystemPackage0", new Class[] { String.class });
			getSystemPackage0.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			LogUtils.log(e);
			Main.showMessage("Failed to launch program.",
					"We failed to launch program, most likely caused by a unsupported java version. If this persists, contact an developer.");
		}

		urls = new HashMap<>(10);
		mans = new HashMap<>(10);
		packages = new HashMap<>();
		classes = new HashMap<>();
		resources = new HashMap<>();
		libraries = new HashMap<>();
	}

	public void cacheClass(String name, byte[] clazz) {
		classes.put(name, clazz);
	}

	public void cacheResource(String name, URL resource) {
		resources.put(name, resource);
	}

	public void cacheLibrary(String library, String path) {
		libraries.put(library, path);
	}

	@Override
	public Class<?> loadClass(String name) {
		Class<?> c = findLoadedClass(name);

		if (c != null) {
			return c;
		}

		try {
			return super.loadClass(name);
		} catch (ClassNotFoundException e) {
			return findClass(name);
		}
	}

	@Override
	public Class<?> findClass(String name) {
		byte[] arr = null;

		if (!classes.containsKey(name)) {
			try {
				Main.networkManager.sendLine("RequestClass", name);
			} catch (IOException e) {
				if (Main.DEBUG_MODE) {
					e.printStackTrace();
				}
			}

			int max = 60;

			for (int i = max; i > 0; i--) {
				if (classes.containsKey(name)) {
					arr = classes.get(name);
					classes.remove(name);
					break;
				}

				try {
					Thread.sleep(getDelay(i, max));
				} catch (InterruptedException e) {
					if (Main.DEBUG_MODE) {
						e.printStackTrace();
					}
				}
			}
		} else {
			arr = classes.get(name);
			classes.remove(name);
		}

		if (arr == null) {
			Main.showMessage("Failed to find class!",
					"A internal error occurred, unstable internet connection? Try again or contact an developer.");
			System.exit(0);
		}

		return defineClass(name, arr, 0, arr.length, null);
	}

	@Override
	public URL getResource(String name) {
		URL resource = null;

		try {
			resource = super.getResource(name);
		} catch (NullPointerException e) {
		}

		if (resource == null) {
			if (!resources.containsKey(name)) {
				try {
					Main.networkManager.sendLine("RequestResource", name);
				} catch (IOException e) {
					if (Main.DEBUG_MODE) {
						e.printStackTrace();
					}
				}

				int max = 60;

				for (int i = max; i > 0; i--) {
					if (resources.containsKey(name)) {
						resource = resources.get(name);
						break;
					}

					try {
						Thread.sleep(getDelay(i, max));
					} catch (InterruptedException e) {
						if (Main.DEBUG_MODE) {
							e.printStackTrace();
						}
					}
				}
			} else {
				resource = resources.get(name);
			}
		}

		if (resource == null) {
			Main.showMessage("Failed to find resource!",
					"A internal error occurred, unstable internet connection? Try again or contact an developer.");
			System.exit(0);
		}

		return resource;
	}

	@Override
	protected Package getPackage(String name) {
		Package pkg;

		synchronized (packages) {
			pkg = packages.get(name);
		}

		if (pkg == null) {
			if (parent != null && getPackage != null) {
				try {
					pkg = (Package) getPackage.invoke(parent, name);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					LogUtils.log(e);
					Main.showMessage("Failed to run program code.",
							"Failed to run code chunk, most likely caused by a unsupported java version. If this persists, contact an developer.");
				}
			} else {
				pkg = getSystemPackage(name);
			}

			if (pkg != null) {
				synchronized (packages) {
					Package pkg2 = packages.get(name);

					if (pkg2 == null) {
						packages.put(name, pkg);
					} else {
						pkg = pkg2;
					}
				}
			}
		}

		if (pkg == null) {
			String fn = name.replace('.', '/').concat("/");
			
			String newName = name.replace('/', '.');

			Manifest man = loadManifest(fn);
			URL url = urls.get(fn);

			if (url == null) {
				File file = new File(fn);
				
				try {
					url = ParseUtil.fileToEncodedURL(file);
				} catch (MalformedURLException e) {
				}

				if (url != null) {
					urls.put(fn, url);

					if (file.isFile()) {
						mans.put(fn, loadManifest(fn));
					}
				}
			}

			if (man != null) {
				try {
					pkg = (Package) packageClassMani.newInstance(newName, man, url, null);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					LogUtils.log(e);
					Main.showMessage("Failed to run program code.",
							"Failed to run code chunk (CODE: MANI), most likely caused by a unsupported java version. If this persists, contact an developer.");
				}
			} else {
				try {
					pkg = (Package) packageClassNonMani.newInstance(newName, null, null, null, null, null, null, null,
							null);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					LogUtils.log(e);
					Main.showMessage("Failed to run program code.",
							"Failed to run code chunk (CODE: NONMANI), most likely caused by a unsupported java version. If this persists, contact an developer.");
				}
			}
		}

		return pkg;
	}

	private Package getSystemPackage(String name) {
		synchronized (packages) {
			Package pkg = packages.get(name);

			if (pkg == null) {
				name = name.replace('.', '/').concat("/");
				String fn = null;

				try {
					fn = (String) getSystemPackage0.invoke(null, name);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					LogUtils.log(e);
					Main.showMessage("Failed to run program code.",
							"Failed to run code chunk (CODE: SYSPACK0), most likely caused by a unsupported java version. If this persists, contact an developer.");
				}

				if (fn != null) {
					pkg = defineSystemPackage(name, fn);
				}
			}
			return pkg;
		}
	}

	private Package defineSystemPackage(final String iname, final String fn) {
		return AccessController.doPrivileged(new PrivilegedAction<Package>() {
			public Package run() {
				String name = iname;
				URL url = urls.get(fn);

				if (url == null) {
					File file = new File(fn);
					try {
						url = ParseUtil.fileToEncodedURL(file);
					} catch (MalformedURLException e) {
					}

					if (url != null) {
						urls.put(fn, url);

						if (file.isFile()) {
							mans.put(fn, loadManifest(fn));
						}
					}
				}

				name = name.substring(0, name.length() - 1).replace('/', '.');
				Package pkg = null;
				Manifest man = mans.get(fn);

				if (man != null) {
					try {
						pkg = (Package) packageClassMani.newInstance(name, man, url, null);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						LogUtils.log(e);
						Main.showMessage("Failed to run program code.",
								"Failed to run code chunk (CODE: MANI), most likely caused by a unsupported java version. If this persists, contact an developer.");
					}
				} else {
					try {
						pkg = (Package) packageClassNonMani.newInstance(name, null, null, null, null, null, null, null,
								null);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						LogUtils.log(e);
						Main.showMessage("Failed to run program code.",
								"Failed to run code chunk (CODE: NONMANI), most likely caused by a unsupported java version. If this persists, contact an developer.");
					}
				}

				packages.put(name, pkg);
				return pkg;
			}
		});
	}

	@Override
	protected Package definePackage(String name, String specTitle, String specVersion, String specVendor,
			String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
		synchronized (packages) {
			Package pkg = getPackage(name);

			if (pkg != null) {
				throw new IllegalArgumentException(name);
			}

			try {
				pkg = (Package) packageClassNonMani.newInstance(name, specTitle, specVersion, specVendor, implTitle,
						implVersion, implVendor, sealBase, this);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				LogUtils.log(e);
				Main.showMessage("Failed to run program code.",
						"Failed to run code chunk (CODE: DEFNONMANI), most likely caused by a unsupported java version. If this persists, contact an developer.");
			}

			packages.put(name, pkg);
			return pkg;
		}
	}

	@Override
	protected String findLibrary(String name) {
		if (sys_paths == null) {
			usr_paths = initializePath("java.library.path");
			sys_paths = initializePath("sun.boot.library.path");
		}

		String realName = System.mapLibraryName(name);
		String library = null;

		if ((library = getLibrary(realName)) != null) {
			return library;
		}

		for (int i = 0; i < sys_paths.length; i++) {
			File libfile = new File(sys_paths[i], realName);

			if (libfile.exists()) {
				return libfile.getAbsolutePath();
			}

			libfile = mapAlternativeName(libfile);

			if (libfile != null && libfile.exists()) {
				return libfile.getAbsolutePath();
			}
		}

		for (int i = 0; i < usr_paths.length; i++) {
			File libfile = new File(usr_paths[i], realName);

			if (libfile.exists()) {
				return libfile.getAbsolutePath();
			}

			libfile = mapAlternativeName(libfile);

			if (libfile != null && libfile.exists()) {
				return libfile.getAbsolutePath();
			}
		}

		if ((library = getLibrary(realName)) == null) {
			try {
				Main.networkManager.sendLine("RequestLibrary", realName);
			} catch (IOException e) {
				if (Main.DEBUG_MODE) {
					e.printStackTrace();
				}
			}

			int max = 60;

			for (int i = max; i > 0; i--) {
				if ((library = getLibrary(realName)) != null) {
					break;
				}

				try {
					Thread.sleep(getDelay(i, max));
				} catch (InterruptedException e) {
					if (Main.DEBUG_MODE) {
						e.printStackTrace();
					}
				}
			}
		}

		return library;
	}

	private File mapAlternativeName(File lib) {
		String name = lib.toString();
		int index = name.lastIndexOf('.');

		if (index < 0) {
			return null;
		}

		return new File(name.substring(0, index) + ".jnilib");
	}

	private String[] initializePath(String propname) {
		String ldpath = System.getProperty(propname, "");
		String ps = File.pathSeparator;
		int ldlen = ldpath.length();
		int i, j, n;

		i = ldpath.indexOf(ps);
		n = 0;

		while (i >= 0) {
			n++;
			i = ldpath.indexOf(ps, i + 1);
		}

		String[] paths = new String[n + 1];

		n = i = 0;
		j = ldpath.indexOf(ps);

		while (j >= 0) {
			if (j - i > 0) {
				paths[n++] = ldpath.substring(i, j);
			} else if (j - i == 0) {
				paths[n++] = ".";
			}
			i = j + 1;
			j = ldpath.indexOf(ps, i);
		}

		paths[n] = ldpath.substring(i, ldlen);

		return paths;
	}

	private String getLibrary(String name) {
		for (Entry<String, String> entry : libraries.entrySet()) {
			String key = entry.getKey();

			if (key.equals(name)) {
				return entry.getValue();
			}
		}

		return null;
	}

	private static Manifest loadManifest(String fn) {
		try (FileInputStream fis = new FileInputStream(fn); JarInputStream jis = new JarInputStream(fis, false)) {
			return jis.getManifest();
		} catch (IOException e) {
			return null;
		}
	}

	private long getDelay(int i, int max) {
		int count = max - i;
		long delay = 0;

		if (count > 50) {
			delay = 200;
		} else if (count > 40) {
			delay = 150;
		} else if (count > 30) {
			delay = 125;
		} else if (count > 20) {
			delay = 75;
		} else if (count > 10) {
			delay = 50;
		} else {
			delay = 25;
		}

		return delay;
	}
}
