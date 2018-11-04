package org.blendee.plugin;

import java.io.File;
import java.io.IOException;

import org.blendee.develop.CodeFormatter;
import org.blendee.develop.TableFacadeGenerator;
import org.blendee.jdbc.Metadata;

public class PluginTableFacadeGenerator extends TableFacadeGenerator {

	private static ClassLoader loader;

	static void setClassLoader(ClassLoader loader) {
		PluginTableFacadeGenerator.loader = loader;
	}

	public PluginTableFacadeGenerator(
		Metadata metadata,
		String rootPackageName,
		Class<?> tableFacadeSuperclass,
		Class<?> rowSuperclass,
		CodeFormatter codeFormatter,
		boolean useNumberClass,
		boolean useNullGuard) {
		super(metadata, rootPackageName, tableFacadeSuperclass, rowSuperclass, codeFormatter, useNumberClass, useNullGuard);
	}

	@Override
	public void writeDatabaseInfo(File home) throws IOException {
		writeDatabaseInfo(home, loader);
	}
}
