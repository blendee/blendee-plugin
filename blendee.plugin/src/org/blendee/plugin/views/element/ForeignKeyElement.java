package org.blendee.plugin.views.element;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

public class ForeignKeyElement extends PropertySourceElement
	implements Element {

	private static final Image icon = Constants.FOREIGN_KEY_ICON.createImage();

	private final RelationshipElement parent;

	private final String name;

	private final RelationshipElement relationship;

	private final Element[] children;

	ForeignKeyElement(
		RelationshipElement parent,
		String name,
		RelationshipElement relationship,
		ForeignKeyColumnElement[] columns) {
		this.parent = parent;
		this.name = name;
		this.relationship = relationship;
		relationship.setParent(this);
		relationship.setParentForPath(parent);
		children = new Element[columns.length + 1];
		children[0] = relationship;
		for (int i = 0; i < columns.length; i++) {
			columns[i].setParent(this);
			children[i + 1] = columns[i];
		}
	}

	@Override
	public int getCategory() {
		return 0;
	}

	@Override
	public String getName() {
		return name + " - " + relationship.getName();
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
		return children.clone();
	}

	@Override
	public boolean hasChildren() {
		return children.length > 0;
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

	public RelationshipElement getRelationship() {
		return relationship;
	}

	@Override
	String getType() {
		return "外部キー";
	}
}
