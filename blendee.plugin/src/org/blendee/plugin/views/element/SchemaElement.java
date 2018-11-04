package org.blendee.plugin.views.element;

import java.util.LinkedHashMap;
import java.util.Map;

import org.blendee.jdbc.BlendeeManager;
import org.blendee.jdbc.TablePath;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.plugin.views.ClassBuilderView;
import org.blendee.util.Blendee;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

public class SchemaElement extends PropertySourceElement {

	private static final Image icon = Constants.SCHEMA_ICON.createImage();

	private static final AllBuildAction allBuildAction = new AllBuildAction();

	private static final RebuildAction rebuildAction = new RebuildAction();

	private final String name;

	private final Map<TablePath, TableElement> children = new LinkedHashMap<>();

	SchemaElement(String name) {
		this.name = name;

		TablePath[] tables = BlendeeManager.get().getMetadata().getTables(name);

		for (TablePath table : tables) {
			children.put(table, new TableElement(this, table));
		}
	}

	@Override
	public int getCategory() {
		return 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return name;
	}

	@Override
	public Image getIcon() {
		return icon;
	}

	@Override
	public Element getParent() {
		return null;
	}

	@Override
	public TableElement[] getChildren() {
		return children.values().toArray(new TableElement[children.size()]);
	}

	@Override
	public boolean hasChildren() {
		return children.size() > 0;
	}

	@Override
	public void doubleClick() {
		TreeViewer viewer = BlendeePlugin.getDefault()
			.getClassBuilderView()
			.getTreeViewer();
		viewer.setExpandedState(this, !viewer.getExpandedState(this));
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		allBuildAction.elements = getChildren();
		rebuildAction.elements = getChildren();
		manager.add(allBuildAction);
		manager.add(rebuildAction);
	}

	@Override
	String getType() {
		return "スキーマ";
	}

	void refresh(TablePath table) {
		TableElement element = children.get(table);

		if (element == null) return;

		ClassBuilderView view = BlendeePlugin.getDefault().getClassBuilderView();
		TreeViewer viewer = view.getTreeViewer();
		viewer.refresh(element);
	}

	private static class AllBuildAction extends Action {

		private TableElement[] elements;

		private AllBuildAction() {
			String text = "すべてのテーブルに Blendee クラスを生成する";
			setText(text);
			setToolTipText(text);
			setImageDescriptor(Constants.SCHEMA_ICON);
		}

		@Override
		public void run() {
			try {
				BlendeePlugin.getDefault().refresh();
				Blendee.execute(t -> {
					for (TableElement element : elements) {
						element.build();
					}
				});
			} catch (Throwable t) {
				throw new IllegalStateException(t);
			} finally {
				BlendeePlugin.refreshOutputPackage();
			}
		}
	}

	private static class RebuildAction extends Action {

		private TableElement[] elements;

		private RebuildAction() {
			String text = "すべての Blendee クラスを再生成する";
			setText(text);
			setToolTipText(text);
			setImageDescriptor(Constants.SCHEMA_ICON);
		}

		@Override
		public void run() {
			try {
				BlendeePlugin.getDefault().refresh();
				Blendee.execute(t -> {
					for (TableElement element : elements) {
						if (element.isAvailable()) element.build();
					}
				});
			} catch (Throwable t) {
				throw new IllegalStateException(t);
			} finally {
				BlendeePlugin.refreshOutputPackage();
			}
		}
	}
}
