package org.blendee.plugin.views.element;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.blendee.jdbc.CrossReference;
import org.blendee.jdbc.MetadataUtilities;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.selector.CommandColumnRepository;
import org.blendee.sql.Column;
import org.blendee.sql.Relationship;
import org.blendee.util.Blendee;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

public class RelationshipElement extends PropertySourceElement {

	private static final Image icon = Constants.RELATIONSHIP_ICON.createImage();

	private final CommandColumnRepository repository;

	private final Relationship relationship;

	private final String name;

	private final String id;

	private Map<Relationship, RelationshipElement> relationshipMap;

	private Map<Column, ColumnElement> columnMap;

	private Map<Column, ForeignKeyColumnElement> fkColumnMap;

	private Element[] children;

	private Element parent;

	private Element parentForPath;

	RelationshipElement(
		CommandColumnRepository repository,
		String id,
		Relationship relationship) {
		this.repository = repository;
		this.relationship = relationship;
		name = createName(relationship);
		this.id = id;
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
		return parentForPath.getPath() + PATH_SEPARETOR + name;
	}

	@Override
	public Image getIcon() {
		return icon;
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public Element[] getChildren() {
		if (children != null) return children.clone();

		try {
			Blendee.execute(t -> {
				prepareChildren();
			});
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return children.clone();
	}

	@Override
	public String toString() {
		return relationship.toString();
	}

	@Override
	public boolean hasChildren() {
		if (children == null) prepareChildren();
		return children.length > 0;
	}

	@Override
	public void doubleClick() {
		TreeViewer viewer = BlendeePlugin.getDefault()
			.getQueryEditorView()
			.getTreeViewer();
		viewer.setExpandedState(this, !viewer.getExpandedState(this));
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {}

	void setParent(Element parent) {
		this.parent = parent;
	}

	void setParentForPath(Element parentForPath) {
		this.parentForPath = parentForPath;
	}

	Relationship getRelationship() {
		return relationship;
	}

	RelationshipElement findRelationship(Relationship relationship) {
		return relationshipMap.get(relationship);
	}

	Element findColumn(Column column) {
		ForeignKeyColumnElement element = fkColumnMap.get(column);
		if (element != null) return element;

		return columnMap.get(column);
	}

	static String createName(Relationship relationship) {
		return relationship.getTablePath()
			+ "("
			+ relationship.getId()
			+ ")";
	}

	void prepareChildren() {
		relationshipMap = new HashMap<>();
		columnMap = new HashMap<>();
		fkColumnMap = new HashMap<>();

		List<ColumnElement> remain = new LinkedList<>();
		Column[] myColumns = relationship.getColumns();
		for (int i = 0; i < myColumns.length; i++) {
			Column column = myColumns[i];
			ColumnElement element = new ColumnElement(
				this,
				repository,
				id,
				column);
			element.setParent(this);
			columnMap.put(column, element);
			remain.add(element);
		}

		List<Element> elements = new LinkedList<>();

		Column[] pkColumns = relationship.getPrimaryKeyColumns();
		if (pkColumns.length > 0) {
			ColumnElement[] pkColumnElements = new ColumnElement[pkColumns.length];
			for (int i = 0; i < pkColumns.length; i++) {
				pkColumnElements[i] = columnMap.get(pkColumns[i]);
				remain.remove(pkColumnElements[i]);
			}

			try {
				Blendee.execute(t -> {
					elements.add(
						new PrimaryKeyElement(
							this,
							MetadataUtilities.getPrimaryKeyName(
								relationship.getTablePath()),
							pkColumnElements));
				});
			} catch (Throwable t) {
				throw new IllegalStateException(t);
			}
		}

		try {
			Blendee.execute(t -> {
				Relationship[] relations = relationship.getRelationships();
				for (Relationship element : relations) {
					RelationshipElement relationshipElement = new RelationshipElement(repository, id, element);
					relationshipMap.put(element, relationshipElement);
					elements.add(
						createForeignKeyElement(
							repository,
							relationship,
							element,
							relationshipElement,
							columnMap,
							remain));
				}
			});
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}

		elements.addAll(remain);
		children = elements.toArray(new Element[elements.size()]);
	}

	private ForeignKeyElement createForeignKeyElement(
		CommandColumnRepository repository,
		Relationship parent,
		Relationship child,
		RelationshipElement element,
		Map<Column, ColumnElement> myColumns,
		List<ColumnElement> remain) {
		CrossReference reference = child.getCrossReference();
		String[] fks = reference.getForeignKeyColumnNames();
		String[] pks = reference.getPrimaryKeyColumnNames();
		ForeignKeyColumnElement[] columns = new ForeignKeyColumnElement[fks.length];
		try {
			Blendee.execute(t -> {
				for (int i = 0; i < fks.length; i++) {

					Column key = parent.getColumn(fks[i]);

					ColumnElement base = myColumns.get(key);

					ForeignKeyColumnElement fkColumnElement = new ForeignKeyColumnElement(base, pks[i]);

					columns[i] = fkColumnElement;

					fkColumnMap.put(key, fkColumnElement);
					remain.remove(base);
				}
			});
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}

		return new ForeignKeyElement(
			this,
			reference.getForeignKeyName(),
			element,
			columns);
	}

	@Override
	String getType() {
		return "テーブル";
	}
}
