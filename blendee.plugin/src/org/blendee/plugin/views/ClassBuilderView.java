package org.blendee.plugin.views;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.BlendeePlugin.JavaProjectException;
import org.blendee.plugin.Constants;
import org.blendee.plugin.views.element.Element;
import org.blendee.plugin.views.element.GeneratorRootElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.DrillDownAdapter;

public class ClassBuilderView extends AbstractView {

	private TreeViewer viewer;

	private DrillDownAdapter drillDownAdapter;

	private Action collapseAllAction;

	private Action refreshAction;

	private class MyTreeContentProvider implements ITreeContentProvider {

		private GeneratorRootElement root;

		private MyTreeContentProvider() {
			reset();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((Element) parentElement).getChildren();
		}

		@Override
		public Object getParent(Object element) {
			return ((Element) element).getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((Element) element).hasChildren();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement.equals(getViewSite())) {
				return getChildren(root);
			}
			return getChildren(inputElement);
		}

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {}

		private void reset() {
			if (BlendeePlugin.getDefault().getProject() == null) {
				root = new GeneratorRootElement();
				return;
			}

			showViewName();

			collapseAllAction.setEnabled(true);

			root = new GeneratorRootElement(
				BlendeePlugin.getDefault().getSchemaNames());
		}
	}

	private static class MyLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			return ((Element) obj).getName();
		}

		@Override
		public Image getImage(Object obj) {
			return ((Element) obj).getIcon();
		}
	}

	private static class MyViewComparator extends ViewerComparator {

		@Override
		public int category(Object element) {
			return ((Element) element).getCategory();
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			return category(e1) - category(e2);
		}
	}

	public ClassBuilderView() {
		BlendeePlugin.getDefault().setClassBuilderView(this);
	}

	public TreeViewer getTreeViewer() {
		return viewer;
	}

	@Override
	public void createPartControl(Composite parent) {
		makeActions();
		viewer = new TreeViewer(
			parent,
			SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setLabelProvider(new MyLabelProvider());
		viewer.setComparator(new MyViewComparator());

		if (BlendeePlugin.getDefault().getProject() != null) {
			showViewName();
		}
		viewer.setContentProvider(new MyTreeContentProvider());
		viewer.setInput(getViewSite());

		hookSelection(viewer, this);
		hookContextMenu(viewer, this);
		hookDoubleClickAction(viewer);
		contributeToActionBars();

		getSite().setSelectionProvider(viewer);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void reset() {
		getMyTreeContentProvider().reset();
		viewer.refresh();
	}

	private MyTreeContentProvider getMyTreeContentProvider() {
		return (MyTreeContentProvider) viewer.getContentProvider();
	}

	@Override
	void fillContextMenu(IMenuManager manager) {
		final Element element = getSelectedElement();
		if (element == null) return;
		element.addActionToContextMenu(manager);
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	void fillLocalToolBar(IToolBarManager manager) {
		manager.add(collapseAllAction);
		manager.add(new Separator());
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		collapseAllAction = new Action() {

			@Override
			public void run() {
				viewer.collapseAll();
				viewer.refresh();
			}
		};
		String collapseAllActionText = "すべて縮小表示";
		collapseAllAction.setText(collapseAllActionText);
		collapseAllAction.setToolTipText(collapseAllActionText);
		collapseAllAction.setImageDescriptor(Constants.COLLAPSE_ALL_ICON);
		collapseAllAction.setEnabled(false);

		refreshAction = new Action() {

			@Override
			public void run() {
				if (!MessageDialog.openConfirm(
					viewer.getControl().getShell(),
					Constants.TITLE,
					"全ての情報の再読込みを行います")) return;
				BlendeePlugin plugin = BlendeePlugin.getDefault();
				try {
					plugin.setProjectAndRefresh(plugin.getProject());
				} catch (JavaProjectException e) {
					throw new RuntimeException(e);
				}
			}
		};

		String refreshActionText = "再読込み";
		refreshAction.setText(refreshActionText);
		refreshAction.setToolTipText(refreshActionText);
		refreshAction.setImageDescriptor(Constants.REFESH_ICON);
		refreshAction.setEnabled(true);
	}

	private Element getSelectedElement() {
		return (Element) ((IStructuredSelection) viewer.getSelection())
			.getFirstElement();
	}
}
