package org.blendee.plugin;

import org.blendee.jdbc.Metadata;
import org.blendee.jdbc.Metadatas;
import org.blendee.jdbc.impl.JDBCMetadata;
import org.blendee.support.TableFacade;
import org.blendee.util.AnnotationMetadataFactory;

public class PluginAnnotationMetadataFactory extends AnnotationMetadataFactory {

	private static final String tableClassName = TableFacade.class.getName();

	private static ClassLoader loader;

	static void setClassLoader(ClassLoader loader) {
		PluginAnnotationMetadataFactory.loader = loader;
	}

	@Override
	public Metadata createMetadata() {
		return new Metadatas(new JDBCMetadata(), super.createMetadata());
	}

	@Override
	protected ClassLoader getClassLoader() {
		return loader;
	}

	@Override
	protected boolean matches(Class<?> clazz) {
		return hasTarget(clazz.getInterfaces()) && !clazz.isInterface();
	}

	@Override
	protected Metadata getDepends() {
		return new JDBCMetadata();
	}

	private static boolean hasTarget(Class<?>[] interfaces) {
		for (Class<?> clazz : interfaces) {
			if (clazz.getName().equals(tableClassName)) return true;
		}

		return false;
	}
}
