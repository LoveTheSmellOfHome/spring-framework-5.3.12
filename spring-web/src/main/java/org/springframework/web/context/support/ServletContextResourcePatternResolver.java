/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * ServletContext-aware subclass of {@link PathMatchingResourcePatternResolver},
 * able to find matching resources below the web application root directory
 * via {@link ServletContext#getResourcePaths}. Falls back to the superclass'
 * file system checking for other resources.
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 */
// PathMatchingResourcePatternResolver 的 ServletContext-aware 子类，
// 能够通过 ServletContext.getResourcePaths 在 web 应用根目录下找到匹配的资源。回退到超类的文件系统检查其他资源。
public class ServletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(ServletContextResourcePatternResolver.class);


	/**
	 * Create a new ServletContextResourcePatternResolver.
	 * @param servletContext the ServletContext to load resources with
	 * @see ServletContextResourceLoader#ServletContextResourceLoader(javax.servlet.ServletContext)
	 */
	// 创建一个新的 ServletContextResourcePatternResolver。
	// 形参：
	// 		servletContext – 用于加载资源的 ServletContext
	public ServletContextResourcePatternResolver(ServletContext servletContext) {
		super(new ServletContextResourceLoader(servletContext));
	}

	/**
	 * Create a new ServletContextResourcePatternResolver.
	 * @param resourceLoader the ResourceLoader to load root directories and
	 * actual resources with
	 */
	// 创建一个新的 ServletContextResourcePatternResolver。
	// 形参：resourceLoader – 用于加载根目录和实际资源的 ResourceLoader
	public ServletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * Overridden version which checks for ServletContextResource
	 * and uses {@code ServletContext.getResourcePaths} to find
	 * matching resources below the web application root directory.
	 * In case of other resources, delegates to the superclass version.
	 * @see #doRetrieveMatchingServletContextResources
	 * @see ServletContextResource
	 * @see javax.servlet.ServletContext#getResourcePaths
	 */
	// 检查 ServletContextResource 并使用 ServletContext.getResourcePaths 在 Web 应用程序根目录下查找匹配资源的覆盖版本。
	// 如果是其他资源，则委托给超类版本。
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		if (rootDirResource instanceof ServletContextResource) {
			ServletContextResource scResource = (ServletContextResource) rootDirResource;
			ServletContext sc = scResource.getServletContext();
			String fullPattern = scResource.getPath() + subPattern;
			Set<Resource> result = new LinkedHashSet<>(8);
			doRetrieveMatchingServletContextResources(sc, fullPattern, scResource.getPath(), result);
			return result;
		}
		else {
			return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
		}
	}

	/**
	 * Recursively retrieve ServletContextResources that match the given pattern,
	 * adding them to the given result set.
	 * @param servletContext the ServletContext to work on
	 * @param fullPattern the pattern to match against,
	 * with preprended root directory path
	 * @param dir the current directory
	 * @param result the Set of matching Resources to add to
	 * @throws IOException if directory contents could not be retrieved
	 * @see ServletContextResource
	 * @see javax.servlet.ServletContext#getResourcePaths
	 */
	// 递归检索与给定模式匹配的 ServletContextResources，将它们添加到给定的结果集中。
	// 形参：
	// 			servletContext – 要处理的 ServletContext
	//			fullPattern – 要匹配的模式，带有预先设定的根目录路径
	//			dir – 当前目录
	//			result - 要添加到的匹配资源集
	//			IOException – 如果无法检索目录内容
	protected void doRetrieveMatchingServletContextResources(
			ServletContext servletContext, String fullPattern, String dir, Set<Resource> result)
			throws IOException {

		Set<String> candidates = servletContext.getResourcePaths(dir);
		if (candidates != null) {
			boolean dirDepthNotFixed = fullPattern.contains("**");
			int jarFileSep = fullPattern.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			String jarFilePath = null;
			String pathInJarFile = null;
			if (jarFileSep > 0 && jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length() < fullPattern.length()) {
				jarFilePath = fullPattern.substring(0, jarFileSep);
				pathInJarFile = fullPattern.substring(jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length());
			}
			for (String currPath : candidates) {
				if (!currPath.startsWith(dir)) {
					// Returned resource path does not start with relative directory:
					// assuming absolute path returned -> strip absolute path.
					// 返回的资源路径不以相对目录开头：假设返回绝对路径 -> 去除绝对路径。
					int dirIndex = currPath.indexOf(dir);
					if (dirIndex != -1) {
						currPath = currPath.substring(dirIndex);
					}
				}
				if (currPath.endsWith("/") && (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") <=
						StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					// Search subdirectories recursively: ServletContext.getResourcePaths
					// only returns entries for one directory level.
					// 递归搜索子目录：ServletContext.getResourcePaths 只返回一个目录级别的条目。
					doRetrieveMatchingServletContextResources(servletContext, fullPattern, currPath, result);
				}
				if (jarFilePath != null && getPathMatcher().match(jarFilePath, currPath)) {
					// Base pattern matches a jar file - search for matching entries within.
					// 基本模式匹配一​​个 jar 文件 - 在其中搜索匹配的条目
					String absoluteJarPath = servletContext.getRealPath(currPath);
					if (absoluteJarPath != null) {
						doRetrieveMatchingJarEntries(absoluteJarPath, pathInJarFile, result);
					}
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new ServletContextResource(servletContext, currPath));
				}
			}
		}
	}

	/**
	 * Extract entries from the given jar by pattern.
	 * @param jarFilePath the path to the jar file
	 * @param entryPattern the pattern for jar entries to match
	 * @param result the Set of matching Resources to add to
	 */
	// 按模式从给定的 jar 中提取条目。
	// 形参：
	//		jarFilePath – jar 文件的路径
	//		entryPattern – jar 条目匹配的模式
	//		result - 要添加到的匹配资源集
	private void doRetrieveMatchingJarEntries(String jarFilePath, String entryPattern, Set<Resource> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching jar file [" + jarFilePath + "] for entries matching [" + entryPattern + "]");
		}
		try (JarFile jarFile = new JarFile(jarFilePath)) {
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (getPathMatcher().match(entryPattern, entryPath)) {
					result.add(new UrlResource(
							ResourceUtils.URL_PROTOCOL_JAR,
							ResourceUtils.FILE_URL_PREFIX + jarFilePath + ResourceUtils.JAR_URL_SEPARATOR + entryPath));
				}
			}
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching resources in jar file [" + jarFilePath +
						"] because the jar cannot be opened through the file system", ex);
			}
		}
	}

}
