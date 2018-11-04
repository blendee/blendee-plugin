package org.blendee.plugin.views;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.views.element.Element;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

public abstract class AbstractView extends ViewPart {

	String createProjectNamePart() {
		IJavaProject project = BlendeePlugin.getDefault().getProject();
		if (project != null) return "[" + project.getElementName() + "]";
		return " ";
	}

	void showViewName() {
		String projectPart = createProjectNamePart();
		setContentDescription(projectPart);
		getViewSite()
			.getActionBars()
			.getStatusLineManager()
			.setMessage(projectPart);
	}

	void hookSelection(TreeViewer viewer, final AbstractView self) {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Element element = (Element) ((IStructuredSelection) event.getSelection()).getFirstElement();
				String message;
				String projectPart = createProjectNamePart();
				if (element != null) {
					message = projectPart + " " + element.getPath();
				} else {
					message = projectPart;
				}

				self.setContentDescription(message);
				getViewSite()
					.getActionBars()
					.getStatusLineManager()
					.setMessage(message);
			}
		});
	}

	void hookContextMenu(TreeViewer viewer, final AbstractView self) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				self.fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	void hookDoubleClickAction(TreeViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				Element element = (Element) ((IStructuredSelection) event.getSelection()).getFirstElement();
				element.doubleClick();
			}
		});
	}

	void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	abstract void fillContextMenu(IMenuManager manager);

	abstract void fillLocalToolBar(IToolBarManager manager);
}
