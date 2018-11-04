package org.blendee.plugin.views.element;

import org.blendee.sql.Column;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

public class UseColumnElement extends AbstractColumnElement {

	private static final ChangeTreeModeAction changeTreeModeAction = new ChangeTreeModeAction();

	private final AnchorElement parent;

	static UseColumnElement[] getElements(
		AnchorElement parent,
		Column[] columns) {
		UseColumnElement[] elements = new UseColumnElement[columns.length];
		for (int i = 0; i < columns.length; i++) {
			elements[i] = new UseColumnElement(parent, columns[i]);
		}

		return elements;
	}

	private UseColumnElement(AnchorElement parent, Column column) {
		super(parent.getId(), parent.getRepository(), column);
		this.parent = parent;
	}

	@Override
	public int getCategory() {
		return 0;
	}

	@Override
	public String getName() {
		return createPathBase(column);
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public void doubleClick() {}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		changeTreeModeAction.element = this;
		manager.add(changeTreeModeAction);
	}

	private static class ChangeTreeModeAction extends Action {

		UseColumnElement element;

		private ChangeTreeModeAction() {
			String text = "ツリー表示";
			setText(text);
			setToolTipText(text);
		}

		@Override
		public void run() {
			element.parent.changeMode();
			element.parent.selectColumn(element.column);
		}
	}

	@Override
	String getType() {
		return "検索に使用するカラム";
	}
}
