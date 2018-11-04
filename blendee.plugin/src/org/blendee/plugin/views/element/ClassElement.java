package org.blendee.plugin.views.element;

import org.blendee.plugin.Constants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Image;

public class ClassElement extends JavaElement {

	static final Image icon = Constants.CLASS_ICON.createImage();

	ClassElement(EditorRootElement root, String path) {
		super(root, path);
	}

	@Override
	public int getCategory() {
		return 2;
	}

	@Override
	public Image getIcon() {
		return icon;
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {}

	@Override
	String getType() {
		return "クラス";
	}

	@Override
	boolean exists() {
		return true;
	}
}
