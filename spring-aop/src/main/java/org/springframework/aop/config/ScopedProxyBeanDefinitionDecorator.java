/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionDecorator} responsible for parsing the
 * {@code <aop:scoped-proxy/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
// BeanDefinitionDecorator 负责解析标签
class ScopedProxyBeanDefinitionDecorator implements BeanDefinitionDecorator {

	// spring-aop.xsd 中 <scoped-proxy> 根元素解析
	private static final String PROXY_TARGET_CLASS = "proxy-target-class"; // 这个根元素的属性


	@Override
	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		// 默认是true
		boolean proxyTargetClass = true;
		if (node instanceof Element) {
			Element ele = (Element) node;
			// 如果 xml 中配置了 proxy-target-class 属性
			if (ele.hasAttribute(PROXY_TARGET_CLASS)) {
				proxyTargetClass = Boolean.parseBoolean(ele.getAttribute(PROXY_TARGET_CLASS));
			}
		}

		// Register the original bean definition as it will be referenced by the scoped proxy
		// and is relevant for tooling (validation, navigation).
		// 将 BeanDefinition 和 Bean 名称做一个封装
		BeanDefinitionHolder holder =
				ScopedProxyUtils.createScopedProxy(definition, parserContext.getRegistry(), proxyTargetClass);
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(definition.getBeanName());
		parserContext.getReaderContext().fireComponentRegistered(
				new BeanComponentDefinition(definition.getBeanDefinition(), targetBeanName));
		return holder;
	}

}
