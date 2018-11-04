package org.blendee.plugin.views.element;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

public class PrimaryKeyElement extends PropertySourceElement {

	private static final Image icon = Constants.PRIMARY_KEY_ICON.createImage();

	private final RelationshipElement parent;

	private final String name;

	private final ColumnElement[] columns;

	PrimaryKeyElement(
		RelationshipElement parent,
		String name,
		ColumnElement[] columns) {
		this.parent = parent;
		this.name = name;
		this.columns = columns;
		for (ColumnElement column : columns)
			column.setParent(this);
	}

	@Override
	public int getCategory() {
		return 1;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return parent.getPath() + PATH_SEPARETOR + name;
	}

	@Override
	public Image getIcon() {
		return icon;
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public Element[] getChildren() {
		return columns.clone();
	}

	@Override
	public boolean hasChildren() {
		return columns.length > 0;
	}

	@Override
	public void doubleClick() {
		TreeViewer viewer = BlendeePlugin.getDefault()
			.getQueryEditorView()
			.getTreeViewer();
		viewer.setExpandedState(this, !viewer.getExpandedState(this));
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {}

	@Override
	String getType() {
		return "主キー";
	}
}
