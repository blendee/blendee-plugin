package org.blendee.plugin.views.element;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blendee.plugin.BlendeePlugin;
import org.eclipse.jface.viewers.TreeViewer;

abstract class JavaElement extends PropertySourceElement {

	private static final Pattern pattern = Pattern.compile("([^\\.]+)$");

	private final String path;

	private final String name;

	private Element parent;

	private final Set<Element> children = new HashSet<>();

	JavaElement(EditorRootElement root, String path) {
		this.path = path;
		name = path.length() > 0 ? getElementName(path) : "";
		parent = root.processParentElement(path, this);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public Element[] getChildren() {
		return children.toArray(new Element[children.size()]);
	}

	public boolean hasChildren(JavaElement child) {
		return children.contains(child);
	}

	@Override
	public boolean hasChildren() {
		return children.size() > 0;
	}

	@Override
	public void doubleClick() {
		TreeViewer viewer = BlendeePlugin.getDefault()
			.getQueryEditorView()
			.getTreeViewer();
		viewer.setExpandedState(this, !viewer.getExpandedState(this));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JavaElement)) return false;

		return ((JavaElement) o).path.equals(path);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public String toString() {
		return path;
	}

	abstract boolean exists();

	void addChild(Element child) {
		children.add(child);
	}

	static String getElementName(String path) {
		Matcher matcher = pattern.matcher(path);
		matcher.find();
		return matcher.group();
	}
}
