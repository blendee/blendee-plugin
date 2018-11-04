package org.blendee.plugin.views.element;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Image;

public interface Element {

	public static final Element[] EMPTY_ARRAY = {};

	String PATH_SEPARETOR = "/";

	int getCategory();

	String getName();

	String getPath();

	Image getIcon();

	Element getParent();

	Element[] getChildren();

	boolean hasChildren();

	void doubleClick();

	void addActionToContextMenu(IMenuManager manager);
}
