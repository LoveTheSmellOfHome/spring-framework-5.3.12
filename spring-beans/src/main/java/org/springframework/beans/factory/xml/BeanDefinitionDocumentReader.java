/*
 * Copyright 2002-2015 the original author or authors.
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

import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * SPI for parsing an XML document that contains Spring bean definitions.
 * Used by {@link XmlBeanDefinitionReader} for actually parsing a DOM document.
 *
 * <p>Instantiated per document to parse: implementations can hold
 * state in instance variables during the execution of the
 * {@code registerBeanDefinitions} method &mdash; for example, global
 * settings that are defined for all bean definitions in the document.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 18.12.2003
 * @see XmlBeanDefinitionReader#setDocumentReaderClass
 */
// 用于解析包含 Spring bean 定义的 XML 文档的 SPI 扩展点。 {@link XmlBeanDefinitionReader} 用于实际解析 DOM 文档
//
// <p>根据要解析的文档实例化：实现可以在 {@code registerBeanDefinitions} 方法的执行期间在实例变量中保存状态——例如，
// 为文档中的所有 bean 定义定义的全局设置。
public interface BeanDefinitionDocumentReader {

	/**
	 * Read bean definitions from the given DOM document and
	 * register them with the registry in the given reader context.
	 * @param doc the DOM document
	 * @param readerContext the current context of the reader
	 * (includes the target registry and the resource being parsed)
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 */
	// 从给定的 DOM 文档中读取 bean 定义，并将它们注册到给定阅读器上下文中的注册表中
	// @param doc DOM 文档
	// @param readerContext 读取器的当前上下文（包括目标注册表和正在解析的资源）
	// 解析错误时@throws BeanDefinitionStoreException
	void registerBeanDefinitions(Document doc, XmlReaderContext readerContext)
			throws BeanDefinitionStoreException;

}
