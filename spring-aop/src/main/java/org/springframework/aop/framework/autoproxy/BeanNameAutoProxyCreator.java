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

package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Auto proxy creator that identifies beans to proxy via a list of names.
 * Checks for direct, "xxx*", and "*xxx" matches.
 *
 * <p>For configuration details, see the javadoc of the parent class
 * AbstractAutoProxyCreator. Typically, you will specify a list of
 * interceptor names to apply to all identified beans, via the
 * "interceptorNames" property.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see #setBeanNames
 * @see #isMatch
 * @see #setInterceptorNames
 * @see AbstractAutoProxyCreator
 */
// 自动代理创建者，通过名称列表标识要代理的 bean。 检查直接、“xxx*”和“*xxx”匹配。
//
// 有关配置的详细信息，请参见父类 AbstractAutoProxyCreator 的 javadoc。 通常，您将通过“interceptorNames” 属性
// 指定要应用于所有已识别 bean 的拦截器名称列表。
//
// 自动动态代理(3 种实现之一)：通过 Bean 名称来进行关联自动动态代理，另外两种
// {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator }
// {@link org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator }
//
// 自动代理第二种实现：根据 Bean 名称匹配实现,支持模糊匹配
@SuppressWarnings("serial")
public class BeanNameAutoProxyCreator extends AbstractAutoProxyCreator {

	private static final String[] NO_ALIASES = new String[0];

	// 可以指定多个 beanNames,如果配置的是 * 号就就把所有的 bean 拿进来
	@Nullable
	private List<String> beanNames;


	/**
	 * Set the names of the beans that should automatically get wrapped with proxies.
	 * A name can specify a prefix to match by ending with "*", e.g. "myBean,tx*"
	 * will match the bean named "myBean" and all beans whose name start with "tx".
	 * <p><b>NOTE:</b> In case of a FactoryBean, only the objects created by the
	 * FactoryBean will get proxied. This default behavior applies as of Spring 2.0.
	 * If you intend to proxy a FactoryBean instance itself (a rare use case, but
	 * Spring 1.2's default behavior), specify the bean name of the FactoryBean
	 * including the factory-bean prefix "&amp;": e.g. "&amp;myFactoryBean".
	 * @see org.springframework.beans.factory.FactoryBean
	 * @see org.springframework.beans.factory.BeanFactory#FACTORY_BEAN_PREFIX
	 */
	// 设置应该自动被代理包装的 bean 的名称。 一个名称可以指定一个前缀以“*”结尾来匹配，例如“myBean,tx*”
	// 将匹配名为“myBean”的bean和所有名称以“tx”开头的bean。
	//
	// 注意：对于 FactoryBean，只有 FactoryBean 创建的对象会被代理。 此默认行为适用于 Spring 2.0。
	// 如果您打算代理 FactoryBean 实例本身（一种罕见的用例，但 Spring 1.2 的默认行为），请指定 FactoryBean 的 bean 名称，
	// 包括 factory-bean 前缀“&”：例如“&myFactoryBean”。
	public void setBeanNames(String... beanNames) {
		Assert.notEmpty(beanNames, "'beanNames' must not be empty");
		this.beanNames = new ArrayList<>(beanNames.length);
		for (String mappedName : beanNames) {
			this.beanNames.add(StringUtils.trimWhitespace(mappedName));
		}
	}


	/**
	 * Delegate to {@link AbstractAutoProxyCreator#getCustomTargetSource(Class, String)}
	 * if the bean name matches one of the names in the configured list of supported
	 * names, returning {@code null} otherwise.
	 * @since 5.3
	 * @see #setBeanNames(String...)
	 */
	// 如果 bean 名称与配置的受支持名称列表中的名称之一匹配，则委托给
	// AbstractAutoProxyCreator.getCustomTargetSource(Class, String) ，否则返回null
	@Override
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		return (isSupportedBeanName(beanClass, beanName) ?
				super.getCustomTargetSource(beanClass, beanName) : null);
	}

	/**
	 * Identify as a bean to proxy if the bean name matches one of the names in
	 * the configured list of supported names.
	 * @see #setBeanNames(String...)
	 */
	// 如果 bean 名称与配置的受支持名称列表中的名称之一匹配，则标识为要代理的 bean。
	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		return (isSupportedBeanName(beanClass, beanName) ?
				PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : DO_NOT_PROXY);
	}

	/**
	 * Determine if the bean name for the given bean class matches one of the names
	 * in the configured list of supported names.
	 * @param beanClass the class of the bean to advise
	 * @param beanName the name of the bean
	 * @return {@code true} if the given bean name is supported
	 * @see #setBeanNames(String...)
	 */
	// 确定给定 bean 类的 bean 名称是否与配置的受支持名称列表中的名称之一匹配。
	// 参形：
	//			beanClass – 要建议的 bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			如果支持给定的 bean 名称，则为true
	private boolean isSupportedBeanName(Class<?> beanClass, String beanName) {
		if (this.beanNames != null) {
			boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
			for (String mappedName : this.beanNames) {
				if (isFactoryBean) {
					if (!mappedName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
						continue;
					}
					mappedName = mappedName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
				}
				if (isMatch(beanName, mappedName)) {
					return true;
				}
			}

			BeanFactory beanFactory = getBeanFactory();
			String[] aliases = (beanFactory != null ? beanFactory.getAliases(beanName) : NO_ALIASES);
			for (String alias : aliases) {
				for (String mappedName : this.beanNames) {
					if (isMatch(alias, mappedName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determine if the given bean name matches the mapped name.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
	 * as well as direct equality. Can be overridden in subclasses.
	 * @param beanName the bean name to check
	 * @param mappedName the name in the configured list of names
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	// 确定给定的 bean 名称是否与映射的名称匹配。
	// 默认实现检查“xxx*”、“*xxx”和“*xxx*”匹配，以及直接相等。 可以在子类中覆盖。
	// 参形：
	//			beanName – 要检查的 bean 名称
	//			mappedName – 配置的名称列表中的名称
	// 返回值：
	//			如果名称匹配
	protected boolean isMatch(String beanName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, beanName);
	}

}
