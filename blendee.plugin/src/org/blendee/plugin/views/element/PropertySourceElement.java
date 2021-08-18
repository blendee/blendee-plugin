package org.blendee.plugin.views.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.blendee.plugin.BlendeePlugin;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

abstract class PropertySourceElement implements Element, IPropertySource {

	private static final String projectId = "PROJECT";

	private static final String nameId = "NAME";

	private static final String pathId = "PATH";

	private static final String typeId = "TYPE";

	private static final PropertyDescriptor projectPropertyDescriptor = new PropertyDescriptor(
		projectId,
		"Project");

	private static final PropertyDescriptor namePropertyDescriptor = new PropertyDescriptor(
		nameId,
		"Name");

	private static final PropertyDescriptor pathPropertyDescriptor = new PropertyDescriptor(
		pathId,
		"Path");

	private static final PropertyDescriptor typePropertyDescriptor = new PropertyDescriptor(
		typeId,
		"Type");

	private final Map<String, String> values = new HashMap<>();

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		values.put(
			projectId,
			BlendeePlugin.getDefault().getProject().getElementName());
		values.put(nameId, getName());
		values.put(pathId, getPath());
		values.put(typeId, getType());
		var list = new ArrayList<IPropertyDescriptor>();
		list.add(projectPropertyDescriptor);
		list.add(namePropertyDescriptor);
		list.add(pathPropertyDescriptor);
		list.add(typePropertyDescriptor);
		list.addAll(Arrays.asList(getMyPropertyDescriptors()));
		return list.toArray(new IPropertyDescriptor[list.size()]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		var value = values.get(id);
		if (value == null) return getMyPropertyValue(id);
		return value;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	}

	abstract String getType();

	IPropertyDescriptor[] getMyPropertyDescriptors() {
		return new IPropertyDescriptor[0];
	}

	Object getMyPropertyValue(Object id) {
		throw new UnsupportedOperationException();
	}
}
