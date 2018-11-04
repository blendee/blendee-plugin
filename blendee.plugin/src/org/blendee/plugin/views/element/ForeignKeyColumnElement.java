package org.blendee.plugin.views.element;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

public class ForeignKeyColumnElement extends PropertySourceElement {

	private final ColumnElement base;

	private final String name;

	private ForeignKeyElement parent;

	ForeignKeyColumnElement(ColumnElement base, String referencesColumn) {
		this.base = base;
		name = base.getName() + " -> " + referencesColumn;
	}

	void setParent(ForeignKeyElement parent) {
		this.parent = parent;
	}

	@Override
	String getType() {
		return base.getType();
	}

	@Override
	public int getCategory() {
		return base.getCategory();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return base.getPath();
	}

	@Override
	public Image getIcon() {
		return base.getIcon();
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public Element[] getChildren() {
		return base.getChildren();
	}

	@Override
	public boolean hasChildren() {
		return base.hasChildren();
	}

	@Override
	public void doubleClick() {
		base.doubleClick(this);
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		base.addActionToContextMenu(manager, this);
	}

	@Override
	IPropertyDescriptor[] getMyPropertyDescriptors() {
		return base.getMyPropertyDescriptors();
	}

	@Override
	Object getMyPropertyValue(Object id) {
		return base.getMyPropertyValue(id);
	}
}
