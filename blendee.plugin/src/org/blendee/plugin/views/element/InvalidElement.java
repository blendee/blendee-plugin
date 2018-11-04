package org.blendee.plugin.views.element;

import org.blendee.plugin.Constants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Image;

public class InvalidElement extends JavaElement {

	static final Image icon = Constants.ERROR_ICON.createImage();

	InvalidElement(EditorRootElement root, String path) {
		super(root, path);
	}

	@Override
	public int getCategory() {
		return 0;
	}

	@Override
	public Image getIcon() {
		return icon;
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {}

	@Override
	String getType() {
		return "不正";
	}

	@Override
	boolean exists() {
		return false;
	}
}
