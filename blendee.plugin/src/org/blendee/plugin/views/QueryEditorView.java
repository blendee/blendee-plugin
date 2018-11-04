package org.blendee.plugin.views;

import org.blendee.internal.U;
import org.blendee.jdbc.TablePath;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.BlendeePlugin.JavaProjectException;
import org.blendee.plugin.Constants;
import org.blendee.plugin.TextDialog;
import org.blendee.plugin.views.element.EditorRootElement;
import org.blendee.plugin.views.element.Element;
import org.blendee.selector.ColumnRepositoryFactory;
import org.blendee.selector.CommandColumnRepository;
import org.blendee.util.Blendee;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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

public class QueryEditorView extends AbstractView {

	private static final IInputValidator tablePathValidator = new IInputValidator() {

		@Override
		public String isValid(String path) {
			if (path.length() == 0) return "未入力です";

			boolean[] exists = { false };
			try {
				Blendee.execute(t -> {
					exists[0] = TablePath.parse(path).exists();
				});
			} catch (Throwable t) {
				throw new IllegalStateException(t);
			}

			if (exists[0]) return null;

			return path + " は存在しません";
		}
	};

	private TreeViewer viewer;

	private DrillDownAdapter drillDownAdapter;

	private Action collapseAllAction;

	private RootModeAction rootModeAction;

	private Action refreshAction;

	private Action saveAction;

	private Action deleteAllEditAction;

	private Action undoAction;

	private Action redoAction;

	public QueryEditorView() {
		BlendeePlugin.getDefault().setQueryEditorView(this);
	}

	private class MyTreeContentProvider implements ITreeContentProvider {

		private EditorRootElement root;

		private CommandColumnRepository repository;

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
				root = new EditorRootElement();
				return;
			}

			showViewName();

			collapseAllAction.setEnabled(true);

			ColumnRepositoryFactory factory = BlendeePlugin.getDefault()
				.getColumnRepositoryFactory();

			repository = new CommandColumnRepository(
				factory.createColumnRepository());

			boolean anchorMode = false;
			if (root != null) anchorMode = root.getAnchorMode();

			root = new EditorRootElement(repository);
			root.setAnchorMode(anchorMode);

			if (anchorMode) {
				rootModeAction.toPackageTreeMode();
			} else {
				rootModeAction.toAnchorMode();
			}
			rootModeAction.setEnabled(true);

			optimizeRepositoryActions(repository);
		}

		private void save() {
			repository.commit();
			optimizeRepositoryActions(repository);
			refreshInternal();
		}

		private void deleteAllEdit() {
			repository.rollback();
			optimizeRepositoryActions(repository);
			refreshInternal();
		}

		private void undo() {
			repository.undo();
			optimizeRepositoryActions(repository);
			refreshInternal();
		}

		private void redo() {
			repository.redo();
			optimizeRepositoryActions(repository);
			refreshInternal();
		}

		private void refreshInternal() {
			root.refresh();
			viewer.refresh();
		}

		private void reverseAnchorMode() {
			root.setAnchorMode(!root.getAnchorMode());
			refreshInternal();
		}

		private boolean getAnchorMode() {
			return root.getAnchorMode();
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
			int sub = category(e1) - category(e2);
			return sub != 0 ? sub : e1.toString().compareTo(e2.toString());
		}
	}

	public boolean hasEdit() {
		MyTreeContentProvider provider = getMyTreeContentProvider();

		if (provider == null) return false;

		CommandColumnRepository repository = provider.repository;
		if (repository == null) return false;
		return repository.canCommit()
			|| repository.canUndo()
			|| repository.canRedo();
	}

	public TreeViewer getTreeViewer() {
		return viewer;
	}

	public InputDialog createTableDialog(
		String defaultTablePath,
		String addition) {
		String message = "新しいテーブル名を入力してください";

		return new InputDialog(
			null,
			Constants.TITLE,
			addition == null ? message : message + U.LINE_SEPARATOR + addition,
			defaultTablePath,
			tablePathValidator);
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

	public void optimizeRepositoryActions(CommandColumnRepository repository) {
		boolean deleteAllEditEnabled;
		if (repository.canCommit()) {
			saveAction.setEnabled(true);
			deleteAllEditEnabled = true;
		} else {
			saveAction.setEnabled(false);
			deleteAllEditEnabled = false;
		}

		if (repository.canUndo()) {
			undoAction.setEnabled(true);
			deleteAllEditEnabled = true;
		} else {
			undoAction.setEnabled(false);
		}

		if (repository.canRedo()) {
			redoAction.setEnabled(true);
			deleteAllEditEnabled = true;
		} else {
			redoAction.setEnabled(false);
		}

		deleteAllEditAction.setEnabled(deleteAllEditEnabled);
	}

	public void refreshTree() {
		getMyTreeContentProvider().refreshInternal();
	}

	public void reset() {
		MyTreeContentProvider provider = getMyTreeContentProvider();

		if (provider == null) return;

		provider.reset();
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
		manager.add(new Separator());

		final String viewNameActionText = "名前を表示";
		Action viewNameAction = new Action() {

			@Override
			public void run() {
				new TextDialog(viewNameActionText, element.getName()).open();
			}
		};
		viewNameAction.setText(viewNameActionText);
		viewNameAction.setToolTipText(viewNameActionText);
		manager.add(viewNameAction);

		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	void fillLocalToolBar(IToolBarManager manager) {
		manager.add(collapseAllAction);
		manager.add(rootModeAction);
		manager.add(new Separator());
		manager.add(refreshAction);
		manager.add(saveAction);
		manager.add(deleteAllEditAction);
		manager.add(new Separator());
		manager.add(undoAction);
		manager.add(redoAction);
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

		rootModeAction = new RootModeAction();
		rootModeAction.toAnchorMode();
		rootModeAction.setEnabled(false);

		refreshAction = new Action() {

			@Override
			public void run() {
				if (!MessageDialog.openConfirm(
					viewer.getControl().getShell(),
					Constants.TITLE,
					"全ての情報の再読込みを行います（保管していない変更は捨てられます）")) return;
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

		saveAction = new Action() {

			@Override
			public void run() {
				if (!MessageDialog.openConfirm(
					viewer.getControl().getShell(),
					Constants.TITLE,
					"変更を保管します")) return;
				getMyTreeContentProvider().save();
			}
		};

		String saveActionText = "保管";
		saveAction.setText(saveActionText);
		saveAction.setToolTipText(saveActionText);
		saveAction.setImageDescriptor(Constants.SAVE_ICON);
		saveAction.setEnabled(false);

		deleteAllEditAction = new Action() {

			@Override
			public void run() {
				if (!MessageDialog.openConfirm(
					viewer.getControl().getShell(),
					Constants.TITLE,
					"すべての変更と変更履歴を除去します")) return;
				getMyTreeContentProvider().deleteAllEdit();
				viewer.setSelection(viewer.getSelection());
			}
		};

		String deleteAllEditActionText = "すべての変更と変更履歴を除去";
		deleteAllEditAction.setText(deleteAllEditActionText);
		deleteAllEditAction.setToolTipText(deleteAllEditActionText);
		deleteAllEditAction.setImageDescriptor(Constants.DERETE_ALL_EDIT_ICON);
		deleteAllEditAction.setEnabled(false);

		undoAction = new Action() {

			@Override
			public void run() {
				getMyTreeContentProvider().undo();
				viewer.setSelection(viewer.getSelection());
			}
		};

		String undoActionText = "元に戻す";
		undoAction.setText(undoActionText);
		undoAction.setToolTipText(undoActionText);
		undoAction.setImageDescriptor(Constants.UNDO_ICON);
		undoAction.setEnabled(false);

		redoAction = new Action() {

			@Override
			public void run() {
				getMyTreeContentProvider().redo();
				viewer.setSelection(viewer.getSelection());
			}
		};

		String redoActionText = "やり直し";
		redoAction.setText(redoActionText);
		redoAction.setToolTipText(redoActionText);
		redoAction.setImageDescriptor(Constants.REDO_ICON);
		redoAction.setEnabled(false);
	}

	private Element getSelectedElement() {
		return (Element) ((IStructuredSelection) viewer.getSelection())
			.getFirstElement();
	}

	private class RootModeAction extends Action {

		@Override
		public void run() {
			MyTreeContentProvider provider = getMyTreeContentProvider();
			provider.reverseAnchorMode();
			if (provider.getAnchorMode()) {
				toPackageTreeMode();
			} else {
				toAnchorMode();
			}
		}

		private void toPackageTreeMode() {
			String text = "ツリー表示へ";
			setText(text);
			setToolTipText(text);
			setImageDescriptor(Constants.CLASS_ICON);
		}

		private void toAnchorMode() {
			String text = "一覧表示へ";
			setText(text);
			setToolTipText(text);
			setImageDescriptor(Constants.ANCHOR_ICON);
		}
	}
}
