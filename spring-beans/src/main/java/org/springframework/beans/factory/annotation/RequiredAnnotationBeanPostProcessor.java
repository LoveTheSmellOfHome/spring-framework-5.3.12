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

package org.springframework.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that enforces required JavaBean properties to have been configured.
 * Required bean properties are detected through a Java 5 annotation:
 * by default, Spring's {@link Required} annotation.
 *
 * <p>The motivation for the existence of this BeanPostProcessor is to allow
 * developers to annotate the setter properties of their own classes with an
 * arbitrary JDK 1.5 annotation to indicate that the container must check
 * for the configuration of a dependency injected value. This neatly pushes
 * responsibility for such checking onto the container (where it arguably belongs),
 * and obviates the need (<b>in part</b>) for a developer to code a method that
 * simply checks that all required properties have actually been set.
 *
 * <p>Please note that an 'init' method may still need to be implemented (and may
 * still be desirable), because all that this class does is enforcing that a
 * 'required' property has actually been configured with a value. It does
 * <b>not</b> check anything else... In particular, it does not check that a
 * configured value is not {@code null}.
 *
 * <p>Note: A default RequiredAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom RequiredAnnotationBeanPostProcessor bean definition.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setRequiredAnnotationType
 * @see Required
 * @deprecated as of 5.1, in favor of using constructor injection for required settings
 * (or a custom {@link org.springframework.beans.factory.InitializingBean} implementation)
 */
// org.springframework.beans.factory.config.BeanPostProcessor实现，强制要求的 JavaBean 属性已经被配置。 
// 必需的 bean 属性是通过 Java 5 注解检测的：默认情况下，Spring 的 @Required 注解。
//	
// 这个 BeanPostProcessor 存在的动机是允许开发人员使用任意 JDK 1.5 注解来注解他们自己类的 setter 属性，
// 以指示容器必须检查依赖注入值的配置。 这巧妙地将此类检查的责任推到了容器上（可以说是它所属的地方），并且无需（部分地）开发人员
// 编写一种方法来简单地检查所有必需的属性是否已实际设置。
//	
// 请注意，“init”方法可能仍然需要实现（并且可能仍然是可取的），因为该类所做的只是强制“必需”属性实际上已经配置了一个值。 
// 它不检查任何东西......特别是，它不检查一个配置的值非 null 。
//	
// 注意：默认的 RequiredAnnotationBeanPostProcessor 将由“context:annotation-config”和“context:component-scan”
// XML 标签注册。 如果您打算指定自定义的 RequiredAnnotationBeanPostProcessor bean 定义，请删除或关闭那里的默认注解配置。
// 已弃用
// 从 5.1 开始，赞成使用构造函数注入进行所需的设置（或自定义org.springframework.beans.factory.InitializingBean实现）
@Deprecated
public class RequiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	/**
	 * Bean definition attribute that may indicate whether a given bean is supposed
	 * to be skipped when performing this post-processor's required property check.
	 * @see #shouldSkip
	 */
	// Bean 定义属性，可以指示在执行此后处理器所需的属性检查时是否应该跳过给定的 Bean。
	public static final String SKIP_REQUIRED_CHECK_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(RequiredAnnotationBeanPostProcessor.class, "skipRequiredCheck");


	private Class<? extends Annotation> requiredAnnotationType = Required.class;

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * Cache for validated bean names, skipping re-validation for the same bean.
	 */
	// 缓存已验证的 bean 名称，跳过对同一 bean 的重新验证
	private final Set<String> validatedBeanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


	/**
	 * Set the 'required' annotation type, to be used on bean property
	 * setter methods.
	 * <p>The default required annotation type is the Spring-provided
	 * {@link Required} annotation.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a property value
	 * is required.
	 */
	// 设置 'required' 注解类型，用于 bean 属性设置方法。
	// 默认的 required 注解类型是 Spring 提供的 @Required 注解。
	// 这个 setter 属性存在以便开发人员可以提供他们自己的（非 Spring 特定的）注解类型来指示需要一个属性值
	public void setRequiredAnnotationType(Class<? extends Annotation> requiredAnnotationType) {
		Assert.notNull(requiredAnnotationType, "'requiredAnnotationType' must not be null");
		this.requiredAnnotationType = requiredAnnotationType;
	}

	/**
	 * Return the 'required' annotation type.
	 */
	// 返回“必需”注解类型
	protected Class<? extends Annotation> getRequiredAnnotationType() {
		return this.requiredAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		if (!this.validatedBeanNames.contains(beanName)) {
			if (!shouldSkip(this.beanFactory, beanName)) {
				List<String> invalidProperties = new ArrayList<>();
				for (PropertyDescriptor pd : pds) {
					if (isRequiredProperty(pd) && !pvs.contains(pd.getName())) {
						invalidProperties.add(pd.getName());
					}
				}
				if (!invalidProperties.isEmpty()) {
					throw new BeanInitializationException(buildExceptionMessage(invalidProperties, beanName));
				}
			}
			this.validatedBeanNames.add(beanName);
		}
		return pvs;
	}

	/**
	 * Check whether the given bean definition is not subject to the annotation-based
	 * required property check as performed by this post-processor.
	 * <p>The default implementations check for the presence of the
	 * {@link #SKIP_REQUIRED_CHECK_ATTRIBUTE} attribute in the bean definition, if any.
	 * It also suggests skipping in case of a bean definition with a "factory-bean"
	 * reference set, assuming that instance-based factories pre-populate the bean.
	 * @param beanFactory the BeanFactory to check against
	 * @param beanName the name of the bean to check against
	 * @return {@code true} to skip the bean; {@code false} to process it
	 */
	// 检查给定的 bean 定义是否不受此后处理器执行的基于注解的必需属性检查的约束。
	// 默认实现检查 bean 定义中是否存在 SKIP_REQUIRED_CHECK_ATTRIBUTE 属性（如果有）。
	// 它还建议在 bean 定义带有“工厂 bean”参考集的情况下跳过，假设基于实例的工厂预先填充了 bean。
	//形参：
	//			beanFactory – 要检查的 BeanFactory
	//			beanName – 要检查的 bean 的名称
	//返回值：
	//			true跳过 bean； false处理它
	protected boolean shouldSkip(@Nullable ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory == null || !beanFactory.containsBeanDefinition(beanName)) {
			return false;
		}
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
		if (beanDefinition.getFactoryBeanName() != null) {
			return true;
		}
		Object value = beanDefinition.getAttribute(SKIP_REQUIRED_CHECK_ATTRIBUTE);
		return (value != null && (Boolean.TRUE.equals(value) || Boolean.parseBoolean(value.toString())));
	}

	/**
	 * Is the supplied property required to have a value (that is, to be dependency-injected)?
	 * <p>This implementation looks for the existence of a
	 * {@link #setRequiredAnnotationType "required" annotation}
	 * on the supplied {@link PropertyDescriptor property}.
	 * @param propertyDescriptor the target PropertyDescriptor (never {@code null})
	 * @return {@code true} if the supplied property has been marked as being required;
	 * {@code false} if not, or if the supplied property does not have a setter method
	 */
	// 提供的属性是否需要有值（即依赖注入）？
	// 此实现在提供的  {@link PropertyDescriptor property}，property上查找
	// {@link #setRequiredAnnotationType "required" annotation}"@required" annotation 的存在。
	//形参：
	//			propertyDescriptor – 目标 PropertyDescriptor（从不为null ）
	//返回值：
	//			true 如果提供的属性已被标记为@Required;
	//			如果没有，或者如果提供的属性没有 setter 方法，则为false
	protected boolean isRequiredProperty(PropertyDescriptor propertyDescriptor) {
		Method setter = propertyDescriptor.getWriteMethod();
		return (setter != null && AnnotationUtils.getAnnotation(setter, getRequiredAnnotationType()) != null);
	}

	/**
	 * Build an exception message for the given list of invalid properties.
	 * @param invalidProperties the list of names of invalid properties
	 * @param beanName the name of the bean
	 * @return the exception message
	 */
	// 为给定的无效属性列表构建异常消息。
	// 形参：
	//			invalidProperties – 无效属性的名称列表
	//			beanName – bean 的名称
	// 返回值：
	//		异常消息
	private String buildExceptionMessage(List<String> invalidProperties, String beanName) {
		int size = invalidProperties.size();
		StringBuilder sb = new StringBuilder();
		sb.append(size == 1 ? "Property" : "Properties");
		for (int i = 0; i < size; i++) {
			String propertyName = invalidProperties.get(i);
			if (i > 0) {
				if (i == (size - 1)) {
					sb.append(" and");
				}
				else {
					sb.append(',');
				}
			}
			sb.append(" '").append(propertyName).append('\'');
		}
		sb.append(size == 1 ? " is" : " are");
		sb.append(" required for bean '").append(beanName).append('\'');
		return sb.toString();
	}

}
