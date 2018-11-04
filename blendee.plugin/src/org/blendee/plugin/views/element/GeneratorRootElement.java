package org.blendee.plugin.views.element;

import java.util.LinkedList;
import java.util.List;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.util.Blendee;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;

public class GeneratorRootElement implements Element {

	private final SchemaElement[] children;

	public GeneratorRootElement(final String[] schemas) {
		final List<SchemaElement> list = new LinkedList<SchemaElement>();
		try {
			Blendee.execute(t -> {
				for (String schema : schemas) {
					list.add(new SchemaElement(schema));
				}
			});
		} catch (Throwable t) {
			t = BlendeePlugin.strip(t);
			t.printStackTrace();
			MessageDialog.openError(null, Constants.TITLE, t.getMessage());
		}

		children = list.toArray(new SchemaElement[list.size()]);
	}

	public GeneratorRootElement() {
		children = new SchemaElement[] {};
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {}

	@Override
	public void doubleClick() {}

	@Override
	public int getCategory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element[] getChildren() {
		return children;
	}

	@Override
	public Image getIcon() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasChildren() {
		return false;
	}
}
