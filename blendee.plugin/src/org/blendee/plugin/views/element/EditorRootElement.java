package org.blendee.plugin.views.element;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.blendee.plugin.BlendeePlugin;
import org.blendee.selector.CommandColumnRepository;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Image;

public class EditorRootElement implements Element {

	private final CommandColumnRepository repository;

	private final Map<String, AnchorElement> anchors = new TreeMap<>();

	private final Map<String, JavaElement> javaMap = new HashMap<>();

	private final Set<Element> children = new HashSet<>();

	private boolean anchorMode = false;

	public EditorRootElement(CommandColumnRepository repository) {
		this.repository = repository;
		Arrays.stream(repository.getIds()).forEach(this::buildTree);
	}

	public EditorRootElement() {
		repository = null;
	}

	@Override
	public int getCategory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Image getIcon() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element[] getChildren() {
		if (!anchorMode) return children.toArray(new Element[children.size()]);
		Collection<? extends Element> values = anchors.values();
		return values.toArray(new Element[values.size()]);
	}

	@Override
	public boolean hasChildren() {
		if (!anchorMode) return children.size() > 0;
		return anchors.size() > 0;
	}

	@Override
	public void doubleClick() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		throw new UnsupportedOperationException();
	}

	public void setAnchorMode(boolean anchorMode) {
		this.anchorMode = anchorMode;
	}

	public boolean getAnchorMode() {
		return anchorMode;
	}

	public void refresh() {
		javaMap.clear();
		children.clear();
		anchors.clear();

		Arrays.stream(repository.getIds()).forEach(this::buildTree);

		List<String> removeTargets = new LinkedList<>();
		for (Entry<String, JavaElement> entry : javaMap.entrySet()) {
			JavaElement element = entry.getValue();
			if (!element.hasChildren()) removeTargets.add(entry.getKey());
		}

		for (String key : removeTargets) {
			javaMap.remove(key);
		}
	}

	Element processParentElement(String childPath, Element child) {
		String parentPath = childPath.replaceFirst("\\.?[^\\.]+$", "");
		if (parentPath.length() == 0) {
			children.add(child);
			return null;
		}

		JavaElement parent = getJavaElement(parentPath);

		processParentElement(parentPath, parent);

		if (child instanceof JavaElement && !parent.hasChildren((JavaElement) child))
			parent.addChild(child);

		return parent;
	}

	private JavaElement getJavaElement(String path) {
		JavaElement element = javaMap.get(path);
		if (element == null) {
			try {
				IJavaProject project = BlendeePlugin.getDefault()
					.getProject();
				if (project.findType(path) != null) {
					element = new ClassElement(this, path);
				} else if (project.findElement(
					new Path(path.replace('.', '/'))) != null) {
					element = new PackageElement(this, path);
				} else {
					element = new InvalidElement(this, path);
				}
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}

			javaMap.put(path, element);
		}

		return element;
	}

	private void buildTree(String id) {
		String[] names = repository.getUsingClassNames(id);

		if (names.length == 0) {
			buildAnchor(names, "", id);
			return;
		}

		Arrays.stream(names).forEach(name -> buildAnchor(names, name, id));
	}

	private void buildAnchor(String[] names, String name, String id) {
		AnchorElement element = new AnchorElement(this, id, name, names, repository);
		element.setParent(getJavaElement(name));
		anchors.put(id, element);
	}
}
