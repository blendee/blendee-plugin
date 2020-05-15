package org.blendee.plugin;

import org.blendee.codegen.CodeFormatter;
import org.blendee.codegen.TableFacadeGenerator;
import org.blendee.jdbc.Metadata;

public class PluginTableFacadeGenerator extends TableFacadeGenerator {

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
}
