/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;

/**
 * Extension of {@link org.springframework.beans.factory.parsing.ReaderContext},
 * specific to use with an {@link XmlBeanDefinitionReader}. Provides access to the
 * {@link NamespaceHandlerResolver} configured in the {@link XmlBeanDefinitionReader}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
// {@link org.springframework.beans.factory.parsing.ReaderContext} 的扩展，专门用于 {@link XmlBeanDefinitionReader}。
// 提供对 {@link XmlBeanDefinitionReader} 中配置的 {@link NamespaceHandlerResolver} 的访问。
public class XmlReaderContext extends ReaderContext {

	private final XmlBeanDefinitionReader reader;

	private final NamespaceHandlerResolver namespaceHandlerResolver;


	/**
	 * Construct a new {@code XmlReaderContext}.
	 * @param resource the XML bean definition resource
	 * @param problemReporter the problem reporter in use
	 * @param eventListener the event listener in use
	 * @param sourceExtractor the source extractor in use
	 * @param reader the XML bean definition reader in use
	 * @param namespaceHandlerResolver the XML namespace resolver
	 */
	public XmlReaderContext(
			Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor,
			XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver) {

		super(resource, problemReporter, eventListener, sourceExtractor);
		this.reader = reader;
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}


	/**
	 * Return the XML bean definition reader in use.
	 */
	// 返回正在使用的 XML bean 定义读取器
	public final XmlBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * Return the bean definition registry to use.
	 * @see XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry)
	 */
	// 返回要使用的 bean 定义注册表
	public final BeanDefinitionRegistry getRegistry() {
		return this.reader.getRegistry();
	}

	/**
	 * Return the resource loader to use, if any.
	 * <p>This will be non-null in regular scenarios,
	 * also allowing access to the resource class loader.
	 * @see XmlBeanDefinitionReader#setResourceLoader
	 * @see ResourceLoader#getClassLoader()
	 */
	// 返回要使用的资源加载器（如果有）。
	// <p>这在常规场景中为非空，也允许访问资源类加载器。
	@Nullable
	public final ResourceLoader getResourceLoader() {
		return this.reader.getResourceLoader();
	}

	/**
	 * Return the bean class loader to use, if any.
	 * <p>Note that this will be null in regular scenarios,
	 * as an indication to lazily resolve bean classes.
	 * @see XmlBeanDefinitionReader#setBeanClassLoader
	 */
	// 返回要使用的 bean 类加载器（如果有）。
	// <p>请注意，这在常规场景中将为 null，作为延迟解析 bean 类的指示。
	@Nullable
	public final ClassLoader getBeanClassLoader() {
		return this.reader.getBeanClassLoader();
	}

	/**
	 * Return the environment to use.
	 * @see XmlBeanDefinitionReader#setEnvironment
	 */
	// 返回环境使用
	public final Environment getEnvironment() {
		return this.reader.getEnvironment();
	}

	/**
	 * Return the namespace resolver.
	 * @see XmlBeanDefinitionReader#setNamespaceHandlerResolver
	 */
	// 返回命名空间解析器。
	public final NamespaceHandlerResolver getNamespaceHandlerResolver() {
		return this.namespaceHandlerResolver;
	}


	// Convenience methods to delegate to

	/**
	 * Call the bean name generator for the given bean definition.
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 */
	// 为给定的 bean 定义调用 bean 名称生成器
	public String generateBeanName(BeanDefinition beanDefinition) {
		return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
	}

	/**
	 * Call the bean name generator for the given bean definition
	 * and register the bean definition under the generated name.
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 * @see BeanDefinitionRegistry#registerBeanDefinition
	 */
	// 为给定的 bean 定义调用 bean 名称生成器，并在生成的名称下注册 bean 定义
	public String registerWithGeneratedName(BeanDefinition beanDefinition) {
		String generatedName = generateBeanName(beanDefinition);
		getRegistry().registerBeanDefinition(generatedName, beanDefinition);
		return generatedName;
	}

	/**
	 * Read an XML document from the given String.
	 * @see #getReader()
	 */
	// 从给定的字符串读取 XML 文档
	public Document readDocumentFromString(String documentContent) {
		InputSource is = new InputSource(new StringReader(documentContent));
		try {
			return this.reader.doLoadDocument(is, getResource());
		}
		catch (Exception ex) {
			throw new BeanDefinitionStoreException("Failed to read XML document", ex);
		}
	}

}
