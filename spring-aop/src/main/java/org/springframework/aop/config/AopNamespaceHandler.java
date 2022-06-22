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

package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} for the {@code aop} namespace.
 *
 * <p>Provides a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for the
 * {@code <aop:config>} tag. A {@code config} tag can include nested
 * {@code pointcut}, {@code advisor} and {@code aspect} tags.
 *
 * <p>The {@code pointcut} tag allows for creation of named
 * {@link AspectJExpressionPointcut} beans using a simple syntax:
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>Using the {@code advisor} tag you can configure an {@link org.springframework.aop.Advisor}
 * and have it applied to all relevant beans in you {@link org.springframework.beans.factory.BeanFactory}
 * automatically. The {@code advisor} tag supports both in-line and referenced
 * {@link org.springframework.aop.Pointcut Pointcuts}:
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
// aop命名空间的NamespaceHandler 。
// 为 BeanDefinitionParser 提供标签。 config 标签可以包括嵌套的 pointcut 、 advisor 和 aspect 标签。
// pointcut 标签允许使用简单的语法创建命名的 AspectJExpressionPointcut bean：
//   <aop:pointcut id="getNameCalls" expression="execution(* *..ITestBean.getName(..))"/>
//
// 使用 advisor 标签，您可以配置 org.springframework.aop.Advisor 并将其自动应用于
// org.springframework.beans.factory.BeanFactory 中的所有相关 bean。 advisor 标签支持内联和引用 Pointcuts ：
//   <aop:advisor id="getAgeAdvisor"
//       pointcut="execution(* *..ITestBean.getAge(..))"
//       advice-ref="getAgeCounter"/>
//
//   <aop:advisor id="getNameAdvisor"
//       pointcut-ref="getNameCalls"
//       advice-ref="getNameCounter"/>
//
// 提供 AOP 相关标签处理器的入口
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParsers} for the
	 * '{@code config}', '{@code spring-configured}', '{@code aspectj-autoproxy}'
	 * and '{@code scoped-proxy}' tags.
	 */
	// 为 'config', 'spring-configured', 'aspectj-autoproxy' and 'scoped-proxy'
	// 标签注册 BeanDefinitionParsers
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.5+ XSDs
		// 在 2.0 XSD 以及 2.5+ XSD 中
		// 将 AOP 中标签和对应的标签 baan 定义解析器一一关联,处理 spring-aop.xsd 中三个根元素
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace in 2.5+
		// 仅在 2.0 XSD 中：移至 2.5+ 中的上下文命名空间
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
