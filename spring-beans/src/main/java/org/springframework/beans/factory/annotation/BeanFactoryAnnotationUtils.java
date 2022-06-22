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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenience methods performing bean lookups related to Spring-specific annotations,
 * for example Spring's {@link Qualifier @Qualifier} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1.2
 * @see BeanFactoryUtils
 */
// 执行与 Spring 特定注解相关的 bean 查找的便捷方法，例如 Spring 的 @Qualifier 注解。
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * Retrieve all bean of type {@code T} from the given {@code BeanFactory} declaring a
	 * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
	 * qualifier, or having a bean name matching the given qualifier.
	 * @param beanFactory the factory to get the target beans from (also searching ancestors)
	 * @param beanType the type of beans to retrieve
	 * @param qualifier the qualifier for selecting among all type matches
	 * @return the matching beans of type {@code T}
	 * @throws BeansException if any of the matching beans could not be created
	 * @since 5.1.1
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	// 从给定的BeanFactory中检索所有类型为T的 bean，声明一个限定符（例如，通过  或@Qualifier ）匹配给定
	// 的限定符，或具有匹配给定限定符的 bean 名称。
	// 参形：
	//				beanFactory – 从中获取目标 bean 的工厂（也搜索祖先）
	//				beanType – 要检索的 bean 的类型
	//				qualifier – 在所有类型匹配中进行选择的限定符
	// 返回值：
	//				T类型的匹配 bean
	// 抛出：
	//				BeansException – 如果无法创建任何匹配的 bean
	public static <T> Map<String, T> qualifiedBeansOfType(
			ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {

		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
		Map<String, T> result = new LinkedHashMap<>(4);
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
				// 依赖查找
				result.put(beanName, beanFactory.getBean(beanName, beanType));
			}
		}
		return result;
	}

	/**
	 * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a
	 * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
	 * qualifier, or having a bean name matching the given qualifier.
	 * @param beanFactory the factory to get the target bean from (also searching ancestors)
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 * @throws NoUniqueBeanDefinitionException if multiple matching beans of type {@code T} found
	 * @throws NoSuchBeanDefinitionException if no matching bean of type {@code T} found
	 * @throws BeansException if the bean could not be created
	 * @see BeanFactoryUtils#beanOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	// 从给定的BeanFactory中获取一个T类型的 bean，声明一个限定符（例如通过  或@Qualifier ）匹配
	// 给定的限定符，或具有匹配给定限定符的 bean 名称。
	// 参形：
	//				beanFactory – 从中获取目标 bean 的工厂（也搜索祖先）
	//				beanType – 要检索的 bean 的类型
	//				qualifier – 在多个 bean 匹配之间进行选择的限定符
	// 返回值：
	//				T类型的匹配 bean（从不为null ）
	// 抛出：
	//				NoUniqueBeanDefinitionException – 如果找到多个匹配的T类型 bean
	//				NoSuchBeanDefinitionException – 如果没有找到T类型的匹配 bean
	//				BeansException – 如果无法创建 bean
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ListableBeanFactory) {
			// Full qualifier matching supported.
			// 支持完整的限定符匹配
			return qualifiedBeanOfType((ListableBeanFactory) beanFactory, beanType, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name.
			// 回退：至少通过 bean 名称找到目标 bean。
			// 依赖查找
			return beanFactory.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for bean name '" + qualifier +
					"'! (Note: Qualifier matching not supported because given " +
					"BeanFactory does not implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a qualifier
	 * (e.g. {@code <qualifier>} or {@code @Qualifier}) matching the given qualifier).
	 * @param bf the factory to get the target bean from
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 */
	// 从给定的BeanFactory中获取一个T类型的 bean，声明一个限定符（例如  或@Qualifier ）匹配给定的限定符）。
	// 参形：
	//			bf – 从中获取目标 bean 的工厂
	//			beanType – 要检索的 bean 的类型
	//			qualifier – 在多个 bean 匹配之间进行选择的限定符
	// 返回值：
	//			T类型的匹配 bean（从不为null ）
	private static <T> T qualifiedBeanOfType(ListableBeanFactory bf, Class<T> beanType, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
		String matchingBean = null;
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, bf)) {
				if (matchingBean != null) {
					throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
				}
				matchingBean = beanName;
			}
		}
		if (matchingBean != null) {
			return bf.getBean(matchingBean, beanType);
		}
		else if (bf.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name - probably a manually registered singleton.
			// 后备：至少通过 bean 名称找到目标 bean - 可能是手动注册的单例。
			return bf.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * Check whether the named bean declares a qualifier of the given name.
	 * @param qualifier the qualifier to match
	 * @param beanName the name of the candidate bean
	 * @param beanFactory the factory from which to retrieve the named bean
	 * @return {@code true} if either the bean definition (in the XML case)
	 * or the bean's factory method (in the {@code @Bean} case) defines a matching
	 * qualifier value (through {@code <qualifier>} or {@code @Qualifier})
	 * @since 5.0
	 */
	// 检查命名 bean 是否声明了给定名称的限定符。
	// 参形：
	//				qualifier -- 要匹配的限定符
	//				beanName – 候选 bean 的名称
	//				beanFactory – 从中检索命名 bean 的工厂
	// 返回值：
	//				true bean 定义（在 XML 情况下）或 bean 的工厂方法（在@Bean情况下）定义了
	//				匹配的限定符值（通过  或@Qualifier ）
	public static boolean isQualifierMatch(
			Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

		// Try quick bean name or alias match first...
		if (qualifier.test(beanName)) {
			return true;
		}
		if (beanFactory != null) {
			for (String alias : beanFactory.getAliases(beanName)) {
				if (qualifier.test(alias)) {
					return true;
				}
			}
			try {
				Class<?> beanType = beanFactory.getType(beanName);
				if (beanFactory instanceof ConfigurableBeanFactory) {
					BeanDefinition bd = ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName);
					// Explicit qualifier metadata on bean definition? (typically in XML definition)
					// bean定义的显式限定符元数据？ （通常在 XML 定义中）
					if (bd instanceof AbstractBeanDefinition) {
						AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
						AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
						if (candidate != null) {
							Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
							if (value != null && qualifier.test(value.toString())) {
								return true;
							}
						}
					}
					// Corresponding qualifier on factory method? (typically in configuration class)
					// 工厂方法的相应限定符？ （通常在配置类中）
					if (bd instanceof RootBeanDefinition) {
						Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
						if (factoryMethod != null) {
							Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
							if (targetAnnotation != null) {
								return qualifier.test(targetAnnotation.value());
							}
						}
					}
				}
				// Corresponding qualifier on bean implementation class? (for custom user types)
				// bean实现类的相应限定符？ （用于自定义用户类型）
				if (beanType != null) {
					Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
					if (targetAnnotation != null) {
						return qualifier.test(targetAnnotation.value());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore - can't compare qualifiers for a manually registered singleton object
			}
		}
		return false;
	}

}
