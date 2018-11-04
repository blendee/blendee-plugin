package org.blendee.plugin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.blendee.internal.U;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

class JavaProjectClassLoader extends ClassLoader {

	private final ClassLoader[] loaders;

	JavaProjectClassLoader(
		ClassLoader parent,
		IJavaProject project)
		throws JavaModelException {
		super(parent);
		List<ClassLoader> allList = new LinkedList<>();
		List<ClassLoader> exportedList = new LinkedList<>();
		ClassLoader defaultOutputClassLoader = createClassLoader(
			getAbsolutePath(project, project.getOutputLocation()));
		IClasspathEntry[] entries = project.getResolvedClasspath(true);
		for (IClasspathEntry entry : entries) {
			ClassLoader loader = switchClassLoader(
				entry,
				project,
				defaultOutputClassLoader);
			allList.add(loader);

			if (entry.isExported()) exportedList.add(loader);
		}

		loaders = allList.toArray(new ClassLoader[allList.size()]);
	}

	@Override
	protected Class<?> findClass(String className)
		throws ClassNotFoundException {
		for (ClassLoader loader : loaders) {
			URL url = loader.getResource(className.replace('.', '/') + ".class");
			if (url == null) continue;
			return defineClass(url);
		}

		throw new ClassNotFoundException(className);
	}

	@Override
	public URL getResource(String name) {
		Enumeration<URL> resources;
		try {
			resources = getResources(name);
		} catch (IOException e) {
			return null;
		}

		if (!resources.hasMoreElements()) return null;
		return resources.nextElement();
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		List<URL> results = new LinkedList<>();
		for (ClassLoader loader : loaders) {
			Enumeration<URL> resources = loader.getResources(name);
			while (resources.hasMoreElements()) {
				results.add(resources.nextElement());
			}
		}

		final Iterator<URL> iterator = results.iterator();
		return new Enumeration<URL>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public URL nextElement() {
				return iterator.next();
			}
		};
	}

	public Class<?> defineClass(URL url) {
		try (BufferedInputStream buffer = new BufferedInputStream(
			url.openStream())) {
			byte[] bytes = U.readBytes(buffer);
			return defineClass(null, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private ClassLoader switchClassLoader(
		IClasspathEntry entry,
		IJavaProject project,
		ClassLoader defaultOutputClassLoader)
		throws JavaModelException {
		switch (entry.getEntryKind()) {
		case IClasspathEntry.CPE_LIBRARY:
			return createClassLoader(getAbsolutePath(project, entry.getPath()));
		case IClasspathEntry.CPE_PROJECT:
			return new JavaProjectClassLoader(
				getClass().getClassLoader(),
				createProject(entry));
		case IClasspathEntry.CPE_SOURCE:
			IPath path = entry.getOutputLocation();
			if (path == null) {
				return defaultOutputClassLoader;
			}

			return createClassLoader(getAbsolutePath(project, path));
		default:
			throw new RuntimeException(String.valueOf(entry.getEntryKind()));
		}
	}

	private static IPath getAbsolutePath(IJavaProject project, IPath path)
		throws JavaModelException {
		IPath projectLocation = project.getProject().getLocation();
		//プロジェクト外のライブラリの場合
		if (path.toFile().isAbsolute()) return path;

		return projectLocation.removeLastSegments(1).append(path);
	}

	private ClassLoader createClassLoader(IPath path) {
		try {
			return new URLClassLoader(
				new URL[] { path.makeAbsolute().toFile().toURI().toURL() });
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private static IJavaProject createProject(IClasspathEntry entry) {
		try {
			return (IJavaProject) ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProject(entry.getPath().toString().substring(1))
				.getNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
}
