/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapter for live beans view exposure, building a snapshot of current beans
 * and their dependencies from either a local {@code ApplicationContext} (with a
 * local {@code LiveBeansView} bean definition) or all registered ApplicationContexts
 * (driven by the {@value #MBEAN_DOMAIN_PROPERTY_NAME} environment property).
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 * @see #getSnapshotAsJson()
 * @see org.springframework.web.context.support.LiveBeansViewServlet
 * @deprecated as of 5.3, in favor of using Spring Boot actuators for such needs
 */
// 用于实时 bean 视图公开的适配器，从本地 {@code ApplicationContext}（具有本地 {@code LiveBeansView} bean 定义）
// 或所有注册的 ApplicationContexts（由 {@value MBEAN_DOMAIN_PROPERTY_NAME} 驱动）构建当前 bean 及其依赖项的快照环境属性）
// 实时Bean快照
//
// LiveBeansView 在 Spring Boot 中有个 EndPoint,EndPoint 名字叫 Beans.
@Deprecated
public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

	/**
	 * The "MBean Domain" property name.
	 */
	public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

	/**
	 * The MBean application key.
	 */
	public static final String MBEAN_APPLICATION_KEY = "application";

	private static final Set<ConfigurableApplicationContext> applicationContexts = new LinkedHashSet<>();

	@Nullable
	private static String applicationName;


	static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
		// 通过外部化配置找到 mbeanDomain
		String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
		if (mbeanDomain != null) {
			synchronized (applicationContexts) {
				if (applicationContexts.isEmpty()) {
					try {
						// 利用 JMX(Java Management Extension) API:Java 管理扩展,创建 MBeanServer
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						applicationName = applicationContext.getApplicationName();
						// 向 MBeanServer 注册 bean
						server.registerMBean(new LiveBeansView(),
								new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
					catch (Throwable ex) {
						throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
					}
				}
				applicationContexts.add(applicationContext);
			}
		}
	}

	static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (applicationContexts) {
			if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
				try {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
					if (mbeanDomain != null) {
						server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
				}
				catch (Throwable ex) {
					throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
				}
				finally {
					applicationName = null;
				}
			}
		}
	}


	@Nullable
	private ConfigurableApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}


	/**
	 * Generate a JSON snapshot of current beans and their dependencies,
	 * finding all active ApplicationContexts through {@link #findApplicationContexts()},
	 * then delegating to {@link #generateJson(java.util.Set)}.
	 */
	// 生成当前 bean 及其依赖项的 JSON 快照，通过 {@link findApplicationContexts()} 查找所有活动的 ApplicationContext，
	// 然后委托给 {@link generateJson(java.util.Set)}。
	@Override
	public String getSnapshotAsJson() {
		Set<ConfigurableApplicationContext> contexts;
		if (this.applicationContext != null) {
			contexts = Collections.singleton(this.applicationContext);
		}
		else {
			contexts = findApplicationContexts();
		}
		return generateJson(contexts);
	}

	/**
	 * Find all applicable ApplicationContexts for the current application.
	 * <p>Called if no specific ApplicationContext has been set for this LiveBeansView.
	 * @return the set of ApplicationContexts
	 */
	// 如果当前bean的实时快照没有特点应用程序上下文，则调用查找当前应用的所有上下文
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		synchronized (applicationContexts) {
			return new LinkedHashSet<>(applicationContexts);
		}
	}

	/**
	 * Actually generate a JSON snapshot of the beans in the given ApplicationContexts.
	 * <p>This implementation doesn't use any JSON parsing libraries in order to avoid
	 * third-party library dependencies. It produces an array of context description
	 * objects, each containing a context and parent attribute as well as a beans
	 * attribute with nested bean description objects. Each bean object contains a
	 * bean, scope, type and resource attribute, as well as a dependencies attribute
	 * with a nested array of bean names that the present bean depends on.
	 * @param contexts the set of ApplicationContexts
	 * @return the JSON document
	 */
	// 实际上在给定的 ApplicationContexts 中生成 bean 的 JSON 快照。
	// 该实现不使用任何 JSON 解析库，以避免第三方库依赖。
	// 它生成一个上下文描述对象数组，每个对象包含一个上下文和父属性以及一个带有嵌套 bean 描述对象的 beans 属性。
	// 每个 bean 对象都包含一个 bean、范围、类型和资源属性，以及一个带有当前 bean 依赖的 bean 名称嵌套数组的依赖项属性
	//
	// 返回 Json 数据,把 Spring 应用上下文中(包括层次性应用上下文里的)所有的 Beans,做一个 Json 格式,然后进行相关操作
	protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
		StringBuilder result = new StringBuilder("[\n");
		for (Iterator<ConfigurableApplicationContext> it = contexts.iterator(); it.hasNext();) {
			ConfigurableApplicationContext context = it.next();
			result.append("{\n\"context\": \"").append(context.getId()).append("\",\n");
			if (context.getParent() != null) {
				result.append("\"parent\": \"").append(context.getParent().getId()).append("\",\n");
			}
			else {
				result.append("\"parent\": null,\n");
			}
			result.append("\"beans\": [\n");
			ConfigurableListableBeanFactory bf = context.getBeanFactory();
			String[] beanNames = bf.getBeanDefinitionNames();
			boolean elementAppended = false;
			for (String beanName : beanNames) {
				BeanDefinition bd = bf.getBeanDefinition(beanName);
				if (isBeanEligible(beanName, bd, bf)) {
					if (elementAppended) {
						result.append(",\n");
					}
					result.append("{\n\"bean\": \"").append(beanName).append("\",\n");
					result.append("\"aliases\": ");
					appendArray(result, bf.getAliases(beanName));
					result.append(",\n");
					String scope = bd.getScope();
					if (!StringUtils.hasText(scope)) {
						scope = BeanDefinition.SCOPE_SINGLETON;
					}
					result.append("\"scope\": \"").append(scope).append("\",\n");
					Class<?> beanType = bf.getType(beanName);
					if (beanType != null) {
						result.append("\"type\": \"").append(beanType.getName()).append("\",\n");
					}
					else {
						result.append("\"type\": null,\n");
					}
					result.append("\"resource\": \"").append(getEscapedResourceDescription(bd)).append("\",\n");
					result.append("\"dependencies\": ");
					appendArray(result, bf.getDependenciesForBean(beanName));
					result.append("\n}");
					elementAppended = true;
				}
			}
			result.append("]\n");
			result.append('}');
			if (it.hasNext()) {
				result.append(",\n");
			}
		}
		result.append(']');
		return result.toString();
	}

	/**
	 * Determine whether the specified bean is eligible for inclusion in the
	 * LiveBeansView JSON snapshot.
	 * @param beanName the name of the bean
	 * @param bd the corresponding bean definition
	 * @param bf the containing bean factory
	 * @return {@code true} if the bean is to be included; {@code false} otherwise
	 */
	// 确定指定的 bean 是否有资格包含在 LiveBeansView JSON 快照中
	protected boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
		return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE &&
				(!bd.isLazyInit() || bf.containsSingleton(beanName)));
	}

	/**
	 * Determine a resource description for the given bean definition and
	 * apply basic JSON escaping (backslashes, double quotes) to it.
	 * @param bd the bean definition to build the resource description for
	 * @return the JSON-escaped resource description
	 */
	@Nullable
	protected String getEscapedResourceDescription(BeanDefinition bd) {
		String resourceDescription = bd.getResourceDescription();
		if (resourceDescription == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(resourceDescription.length() + 16);
		for (int i = 0; i < resourceDescription.length(); i++) {
			char character = resourceDescription.charAt(i);
			if (character == '\\') {
				result.append('/');
			}
			else if (character == '"') {
				result.append("\\").append('"');
			}
			else {
				result.append(character);
			}
		}
		return result.toString();
	}

	private void appendArray(StringBuilder result, String[] arr) {
		result.append('[');
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(StringUtils.arrayToDelimitedString(arr, "\", \""));
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(']');
	}

}
