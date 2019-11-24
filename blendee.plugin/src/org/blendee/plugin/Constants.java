package org.blendee.plugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;

public class Constants {

	public static final String TITLE = "Blendee";

	public static final String SCHEMA_NAMES = "schemaNames";

	public static final String OUTPUT_PACKAGE_NAME = "outputPackageName";

	public static final String JDBC_DRIVER_CLASS = "jdbcDriverClass";

	public static final String JDBC_URL = "jdbcURL";

	public static final String JDBC_USER = "jdbcUser";

	public static final String JDBC_PASSWORD = "jdbcPassword";

	public static final String TRANSACTION_FACTORY_CLASS = "transactionFactoryClass";

	public static final String METADATA_FACTORY_CLASS = "metadataFactoryClass";

	public static final String TABLE_FACADE_PARENT_CLASS = "tableFacadeParentClass";

	public static final String ROW_PARENT_CLASS = "rowParentClass";

	public static final String CODE_FORMATTER_CLASS = "codeFormatterClass";

	public static final String USE_NUMBER_CLASS = "useNumberClass";

	public static final String NOT_USE_NULL_GUARD = "notUseNullGuard";

	public static final ImageDescriptor BLENDEE_ICON;

	public static final ImageDescriptor COLLAPSE_ALL_ICON;

	public static final ImageDescriptor REFESH_ICON;

	public static final ImageDescriptor SCHEMA_ICON;

	public static final ImageDescriptor TABLE_ICON;

	public static final ImageDescriptor UNBUILT_TABLE_ICON;

	private static final String ID = "org.blendee.plugin";

	static {
		BLENDEE_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/blendee.png").get();

		COLLAPSE_ALL_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/collapse_all.gif").get();

		REFESH_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/refresh.gif").get();

		SCHEMA_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/schema.gif").get();

		TABLE_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/table.png").get();

		UNBUILT_TABLE_ICON = ResourceLocator.imageDescriptorFromBundle(ID, "icons/unbuilt_table.png").get();
	}
}
