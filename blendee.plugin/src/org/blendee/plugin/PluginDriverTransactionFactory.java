package org.blendee.plugin;

import java.sql.Connection;

import org.blendee.util.DriverManagerTransactionFactory;

public class PluginDriverTransactionFactory extends DriverManagerTransactionFactory {

	private static JavaProjectClassLoader loader;

	private static Class<?> proxyClass;

	public PluginDriverTransactionFactory() throws Exception {
		if (proxyClass != null) return;

		var url = PluginDriverTransactionFactory.class
			.getResource("ProxyDriverManager.class");
		proxyClass = loader.defineClass(url);
	}

	static void setClassLoader(JavaProjectClassLoader loader) {
		PluginDriverTransactionFactory.loader = loader;
	}

	@Override
	protected ClassLoader getClassLoader() {
		return loader;
	}

	@Override
	protected Connection getJDBCConnection() {
		try {
			return (Connection) proxyClass
				.getMethod(
					"getConnection",
					String.class,
					String.class,
					String.class)
				.invoke(null, url(), user(), password());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
