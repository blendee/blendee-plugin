package org.blendee.plugin.views.element;

import java.util.Arrays;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.plugin.views.QueryEditorView;
import org.blendee.selector.CommandColumnRepository;
import org.blendee.sql.Column;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class ColumnElement extends AbstractColumnElement {

	static final Image icon = Constants.COLUMN_ICON.createImage();

	static final Image unmarkIcon = Constants.UNMARK_COLUMN_ICON.createImage();

	private static final ColumnAction useAction = new ColumnAction(
		"値を取得する",
		Constants.COLUMN_ICON,
		true);

	private static final ColumnAction unuseAction = new ColumnAction(
		"値を取得しない",
		Constants.COLUMN_DISABLE_ICON,
		false);

	private static final Image disabledIcon = Constants.COLUMN_DISABLE_ICON
		.createImage();

	private static final String useId = "USE";

	private static final PropertyDescriptor[] descripters = {
		getPropertyDescriptor(),
		new PropertyDescriptor(useId, "値の取得") };

	private final RelationshipElement relationship;

	private Element parent;

	ColumnElement(
		RelationshipElement relationship,
		CommandColumnRepository repository,
		String id,
		Column column) {
		super(id, repository, column);
		this.relationship = relationship;
	}

	@Override
	public int getCategory() {
		return 2;
	}

	@Override
	public String getName() {
		return column.getName();
	}

	@Override
	public Image getIcon() {
		if (!repository.containsColumn(id, column)) return disabledIcon;
		return super.getIcon();
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public void doubleClick() {
		doubleClick(this);
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		addActionToContextMenu(manager, this);
	}

	public Column getColumn() {
		return column;
	}

	void doubleClick(Element element) {
		if (repository.containsColumn(id, column)) {
			remove();
		} else {
			add();
		}
		operateView(element);
	}

	void addActionToContextMenu(IMenuManager manager, Element element) {
		useAction.column = this;
		unuseAction.column = this;
		useAction.selected = element;
		unuseAction.selected = element;
		if (repository.containsColumn(id, column)) {
			useAction.setEnabled(false);
			unuseAction.setEnabled(true);
		} else {
			useAction.setEnabled(true);
			unuseAction.setEnabled(false);
		}

		manager.add(useAction);
		manager.add(unuseAction);
	}

	@Override
	String getType() {
		return "カラム";
	}

	void setParent(Element parent) {
		this.parent = parent;
	}

	@Override
	IPropertyDescriptor[] getMyPropertyDescriptors() {
		return descripters;
	}

	@Override
	Object getMyPropertyValue(Object id) {
		if (useId.equals(id))
			return repository.containsColumn(this.id, column) ? "する" : "しない";

		return super.getMyPropertyValue(id);
	}

	private void add() {
		Arrays.stream(repository.getUsingClassNames(id)).forEach(c -> {
			try {
				repository.addColumn(id, column, c);
			} catch (Exception e) {
				repository.addColumn(id, column);
			}
		});
	}

	private void remove() {
		repository.removeColumn(id, column);
	}

	private void operateView(Element element) {
		QueryEditorView view = BlendeePlugin.getDefault()
			.getQueryEditorView();
		view.optimizeRepositoryActions(repository);
		TreeViewer viewer = view.getTreeViewer();
		viewer.refresh(relationship);
		viewer.setSelection(new StructuredSelection(element));
	}

	private static class ColumnAction extends Action {

		private final boolean use;

		private ColumnElement column;

		private Element selected;

		private ColumnAction(String text, ImageDescriptor icon, boolean use) {
			this.use = use;
			setText(text);
			setToolTipText(text);
			setImageDescriptor(icon);
		}

		@Override
		public void run() {
			if (use) {
				column.add();
			} else {
				column.remove();
			}

			column.operateView(selected);
		}
	}
}
