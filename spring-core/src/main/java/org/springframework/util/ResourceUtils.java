/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;

/**
 * Utility methods for resolving resource locations to files in the
 * file system. Mainly for internal use within the framework.
 *
 * <p>Consider using Spring's Resource abstraction in the core package
 * for handling all kinds of file resources in a uniform manner.
 * {@link org.springframework.core.io.ResourceLoader}'s {@code getResource()}
 * method can resolve any location to a {@link org.springframework.core.io.Resource}
 * object, which in turn allows one to obtain a {@code java.io.File} in the
 * file system through its {@code getFile()} method.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.core.io.FileSystemResource
 * @see org.springframework.core.io.UrlResource
 * @see org.springframework.core.io.ResourceLoader
 */
// 用于将资源位置解析为文件系统中的文件的实用方法。 主要供框架内部使用。
//
// 考虑在核心包中使用 Spring 的 Resource 抽象，以统一的方式处理各种文件资源。 org.springframework.core.io.ResourceLoader
// 的 getResource() 方法可以将任何位置解析为 org.springframework.core.io.Resource 对象，
// 这反过来又允许人们通过以下方式获取文件系统中的 java.io.File 它的 getFile() 方法。
public abstract class ResourceUtils {

	/** Pseudo URL prefix for loading from the class path: "classpath:". */
	// 用于从类路径加载的伪 URL 前缀："classpath:"
	public static final String CLASSPATH_URL_PREFIX = "classpath:"; // 匹配当前 classpath: 下的资源

	/** URL prefix for loading from the file system: "file:". */
	// 从文件系统加载的 URL 前缀："file:"
	public static final String FILE_URL_PREFIX = "file:";

	/** URL prefix for loading from a jar file: "jar:". */
	// 从 jar 文件加载的 URL 前缀："jar:"
	public static final String JAR_URL_PREFIX = "jar:";

	/** URL prefix for loading from a war file on Tomcat: "war:". */
	// 从 Tomcat 上的 war 文件加载的 URL 前缀："war:"
	public static final String WAR_URL_PREFIX = "war:";

	/** URL protocol for a file in the file system: "file". */
	// 文件系统中文件的 URL 协议："file"
	public static final String URL_PROTOCOL_FILE = "file";

	/** URL protocol for an entry from a jar file: "jar". */
	// 来自 jar 文件的条目的 URL 协议："jar"
	public static final String URL_PROTOCOL_JAR = "jar";

	/** URL protocol for an entry from a war file: "war". */
	// 来自 war 文件的条目的 URL 协议："war"
	public static final String URL_PROTOCOL_WAR = "war";

	/** URL protocol for an entry from a zip file: "zip". */
	// 来自 zip 文件的条目的 URL 协议："zip"
	public static final String URL_PROTOCOL_ZIP = "zip";

	/** URL protocol for an entry from a WebSphere jar file: "wsjar". */
	// WebSphere jar 文件中条目的 URL 协议："wsjar"
	public static final String URL_PROTOCOL_WSJAR = "wsjar";

	/** URL protocol for an entry from a JBoss jar file: "vfszip". */
	// JBoss jar 文件中条目的 URL 协议："vfszip"
	public static final String URL_PROTOCOL_VFSZIP = "vfszip";

	/** URL protocol for a JBoss file system resource: "vfsfile". */
	// JBoss 文件系统资源的 URL 协议："vfsfile"
	public static final String URL_PROTOCOL_VFSFILE = "vfsfile";

	/** URL protocol for a general JBoss VFS resource: "vfs". */
	// 通用 JBoss VFS 资源的 URL 协议："vfs"
	public static final String URL_PROTOCOL_VFS = "vfs";

	/** File extension for a regular jar file: ".jar". */
	// 常规 jar 文件的文件扩展名：".jar"
	public static final String JAR_FILE_EXTENSION = ".jar";

	/** Separator between JAR URL and file path within the JAR: "!/". */
	// JAR URL 和 JAR 中的文件路径之间的分隔符："!/"
	public static final String JAR_URL_SEPARATOR = "!/";

	/** Special separator between WAR URL and jar part on Tomcat. */
	// Tomcat 上 WAR URL 和 jar 部分之间的特殊分隔符："*/"
	public static final String WAR_URL_SEPARATOR = "*/";


	/**
	 * Return whether the given resource location is a URL:
	 * either a special "classpath" pseudo URL or a standard URL.
	 * @param resourceLocation the location String to check
	 * @return whether the location qualifies as a URL
	 * @see #CLASSPATH_URL_PREFIX
	 * @see java.net.URL
	 */
	// 返回给定的资源位置是否为 URL：特殊的“类路径”伪 URL 或标准 URL。
	// 形参：resourceLocation – 要检查的位置字符串
	// 返回值：该位置是否符合 URL
	public static boolean isUrl(@Nullable String resourceLocation) {
		if (resourceLocation == null) {
			return false;
		}
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			return true;
		}
		try {
			new URL(resourceLocation);
			return true;
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * Resolve the given resource location to a {@code java.net.URL}.
	 * <p>Does not check whether the URL actually exists; simply returns
	 * the URL that the given location would correspond to.
	 * @param resourceLocation the resource location to resolve: either a
	 * "classpath:" pseudo URL, a "file:" URL, or a plain file path
	 * @return a corresponding URL object
	 * @throws FileNotFoundException if the resource cannot be resolved to a URL
	 */
	// 将给定的资源位置解析为java.net.URL 。
	// 不检查 URL 是否实际存在； 简单地返回给定位置对应的 URL。
	// 形参：resourceLocation – 要解析的资源位置：“classpath:”伪 URL、“file:” URL 或纯文件路径
	// 返回值：一个对应的 URL 对象
	public static URL getURL(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				String description = "class path resource [" + path + "]";
				throw new FileNotFoundException(description +
						" cannot be resolved to URL because it does not exist");
			}
			return url;
		}
		try {
			// try URL
			// 尝试网址
			return new URL(resourceLocation);
		}
		catch (MalformedURLException ex) {
			// no URL -> treat as file path
			// 无 URL -> 视为文件路径
			try {
				return new File(resourceLocation).toURI().toURL();
			}
			catch (MalformedURLException ex2) {
				throw new FileNotFoundException("Resource location [" + resourceLocation +
						"] is neither a URL not a well-formed file path");
			}
		}
	}

	/**
	 * Resolve the given resource location to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * <p>Does not check whether the file actually exists; simply returns
	 * the File that the given location would correspond to.
	 * @param resourceLocation the resource location to resolve: either a
	 * "classpath:" pseudo URL, a "file:" URL, or a plain file path
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the resource cannot be resolved to
	 * a file in the file system
	 */
	// 将给定的资源位置解析为java.io.File ，即文件系统中的文件。
	// 不检查文件是否实际存在； 简单地返回给定位置对应的文件。
	// 形参：resourceLocation – 要解析的资源位置：“classpath:”伪 URL、“file:” URL 或纯文件路径
	// 返回值：一个对应的 File 对象
	public static File getFile(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			String description = "class path resource [" + path + "]";
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				throw new FileNotFoundException(description +
						" cannot be resolved to absolute file path because it does not exist");
			}
			return getFile(url, description);
		}
		try {
			// try URL
			return getFile(new URL(resourceLocation));
		}
		catch (MalformedURLException ex) {
			// no URL -> treat as file path
			return new File(resourceLocation);
		}
	}

	/**
	 * Resolve the given resource URL to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * @param resourceUrl the resource URL to resolve
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the URL cannot be resolved to
	 * a file in the file system
	 */
	// 将给定的资源 URL 解析为java.io.File ，即文件系统中的文件。
	// 形参：resourceUrl – 要解析的资源 URL
	// 返回值：一个对应的 File 对象
	public static File getFile(URL resourceUrl) throws FileNotFoundException {
		return getFile(resourceUrl, "URL");
	}

	/**
	 * Resolve the given resource URL to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * @param resourceUrl the resource URL to resolve
	 * @param description a description of the original resource that
	 * the URL was created for (for example, a class path location)
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the URL cannot be resolved to
	 * a file in the file system
	 */
	// 将给定的资源 URL 解析为java.io.File ，即文件系统中的文件。
	// 形参：
	// resourceUrl – 要解析的资源 URL
	// description – 为其创建 URL 的原始资源的描述（例如，类路径位置）
	// 返回值：一个对应的 File 对象
	public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
		Assert.notNull(resourceUrl, "Resource URL must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
					"because it does not reside in the file system: " + resourceUrl);
		}
		try {
			return new File(toURI(resourceUrl).getSchemeSpecificPart());
		}
		catch (URISyntaxException ex) {
			// Fallback for URLs that are not valid URIs (should hardly ever happen).
			// 对不是有效 URI 的 URL 的回退（应该几乎永远不会发生）
			return new File(resourceUrl.getFile());
		}
	}

	/**
	 * Resolve the given resource URI to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * @param resourceUri the resource URI to resolve
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the URL cannot be resolved to
	 * a file in the file system
	 * @since 2.5
	 */
	// 将给定的资源 URI 解析为java.io.File ，即文件系统中的文件。
	// 形参：resourceUri – 要解析的资源 URI
	// 返回值：一个对应的 File 对象
	public static File getFile(URI resourceUri) throws FileNotFoundException {
		return getFile(resourceUri, "URI");
	}

	/**
	 * Resolve the given resource URI to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * @param resourceUri the resource URI to resolve
	 * @param description a description of the original resource that
	 * the URI was created for (for example, a class path location)
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the URL cannot be resolved to
	 * a file in the file system
	 * @since 2.5
	 */
	// 将给定的资源 URI 解析为java.io.File ，即文件系统中的文件。
	// 形参：
	// resourceUri – 要解析的资源 URI
	// description – 为其创建 URI 的原始资源的描述（例如，类路径位置）
	// 返回值：一个对应的 File 对象
	public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
		Assert.notNull(resourceUri, "Resource URI must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
					"because it does not reside in the file system: " + resourceUri);
		}
		return new File(resourceUri.getSchemeSpecificPart());
	}

	/**
	 * Determine whether the given URL points to a resource in the file system,
	 * i.e. has protocol "file", "vfsfile" or "vfs".
	 * @param url the URL to check
	 * @return whether the URL has been identified as a file system URL
	 */
	// 确定给定的 URL 是否指向文件系统中的资源，即具有协议“file”、“vfsfile”或“vfs”。
	// 形参：url – 要检查的 URL
	// 返回值：URL 是否已被识别为文件系统 URL
	public static boolean isFileURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol) ||
				URL_PROTOCOL_VFS.equals(protocol));
	}

	/**
	 * Determine whether the given URL points to a resource in a jar file.
	 * i.e. has protocol "jar", "war, ""zip", "vfszip" or "wsjar".
	 * @param url the URL to check
	 * @return whether the URL has been identified as a JAR URL
	 */
	// 确定给定的 URL 是否指向 jar 文件中的资源。 即具有协议“jar”、“war”、“zip”、“vfszip”或“wsjar”。
	// 形参：url – 要检查的 URL
	// 返回值：URL 是否已被识别为 JAR URL
	public static boolean isJarURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_JAR.equals(protocol) || URL_PROTOCOL_WAR.equals(protocol) ||
				URL_PROTOCOL_ZIP.equals(protocol) || URL_PROTOCOL_VFSZIP.equals(protocol) ||
				URL_PROTOCOL_WSJAR.equals(protocol));
	}

	/**
	 * Determine whether the given URL points to a jar file itself,
	 * that is, has protocol "file" and ends with the ".jar" extension.
	 * @param url the URL to check
	 * @return whether the URL has been identified as a JAR file URL
	 * @since 4.1
	 */
	// 确定给定的 URL 是否指向 jar 文件本身，即具有协议“文件”并以“.jar”扩展名结尾。
	// 形参：url – 要检查的 URL
	// 返回值：该 URL 是否已被识别为 JAR 文件 URL
	public static boolean isJarFileURL(URL url) {
		return (URL_PROTOCOL_FILE.equals(url.getProtocol()) &&
				url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION));
	}

	/**
	 * Extract the URL for the actual jar file from the given URL
	 * (which may point to a resource in a jar file or to a jar file itself).
	 * @param jarUrl the original URL
	 * @return the URL for the actual jar file
	 * @throws MalformedURLException if no valid jar file URL could be extracted
	 */
	// 从给定的 URL（可能指向 jar 文件中的资源或指向 jar 文件本身）中提取实际 jar 文件的 URL。
	// 形参：jarUrl – 原始 URL
	// 返回值：实际 jar 文件的 URL
	public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();
		int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
		if (separatorIndex != -1) {
			String jarFile = urlFile.substring(0, separatorIndex);
			try {
				return new URL(jarFile);
			}
			catch (MalformedURLException ex) {
				// Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
				// This usually indicates that the jar file resides in the file system.
				// 原始 jar URL 中可能没有协议，例如 "jar:C:/mypath/myjar.jar"。这通常表示 jar 文件驻留在文件系统中。
				if (!jarFile.startsWith("/")) {
					jarFile = "/" + jarFile;
				}
				return new URL(FILE_URL_PREFIX + jarFile);
			}
		}
		else {
			return jarUrl;
		}
	}

	/**
	 * Extract the URL for the outermost archive from the given jar/war URL
	 * (which may point to a resource in a jar file or to a jar file itself).
	 * <p>In the case of a jar file nested within a war file, this will return
	 * a URL to the war file since that is the one resolvable in the file system.
	 * @param jarUrl the original URL
	 * @return the URL for the actual jar file
	 * @throws MalformedURLException if no valid jar file URL could be extracted
	 * @since 4.1.8
	 * @see #extractJarFileURL(URL)
	 */
	// 从给定的 jar/war URL（可能指向 jar 文件中的资源或指向 jar 文件本身）中提取最外层存档的 URL。
	// 对于嵌套在 war 文件中的 jar 文件，这将返回一个指向该 war 文件的 URL，因为它是文件系统中可解析的。
	// 形参：jarUrl – 原始 URL
	// 返回值：实际 jar 文件的 URL
	// MalformedURLException – 如果无法提取有效的 jar 文件 URL
	public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();

		int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
		if (endIndex != -1) {
			// Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
			String warFile = urlFile.substring(0, endIndex);
			if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
				return new URL(warFile);
			}
			int startIndex = warFile.indexOf(WAR_URL_PREFIX);
			if (startIndex != -1) {
				return new URL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
			}
		}

		// Regular "jar:file:...myjar.jar!/myentry.txt"
		return extractJarFileURL(jarUrl);
	}

	/**
	 * Create a URI instance for the given URL,
	 * replacing spaces with "%20" URI encoding first.
	 * @param url the URL to convert into a URI instance
	 * @return the URI instance
	 * @throws URISyntaxException if the URL wasn't a valid URI
	 * @see java.net.URL#toURI()
	 */
	// 为给定的 URL 创建一个 URI 实例，首先用“%20”URI 编码替换空格。
	// 形参：url – 要转换为 URI 实例的 URL
	// 返回值：URI 实例
	// URISyntaxException – 如果 URL 不是有效的 URI
	public static URI toURI(URL url) throws URISyntaxException {
		return toURI(url.toString());
	}

	/**
	 * Create a URI instance for the given location String,
	 * replacing spaces with "%20" URI encoding first.
	 * @param location the location String to convert into a URI instance
	 * @return the URI instance
	 * @throws URISyntaxException if the location wasn't a valid URI
	 */
	// 为给定的位置字符串创建一个 URI 实例，首先用“%20”URI 编码替换空格。
	// 形参：location – 要转换为 URI 实例的位置字符串
	// 返回值：URI 实例
	// URISyntaxException – 如果该位置不是有效的 URI
	public static URI toURI(String location) throws URISyntaxException {
		return new URI(StringUtils.replace(location, " ", "%20"));
	}

	/**
	 * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the
	 * given connection, preferring {@code false} but leaving the
	 * flag at {@code true} for JNLP based resources.
	 * @param con the URLConnection to set the flag on
	 */
	// 在给定的连接上设置"useCaches"标志，对于基于 JNLP 的资源， "useCaches" false但将该标志保留为true 。
	// 形参：con – 要在其上设置标志的 URLConnection
	public static void useCachesIfNecessary(URLConnection con) {
		con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
	}

}
