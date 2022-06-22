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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Extension of the {@link BeanFactory} interface to be implemented by bean factories
 * that can enumerate all their bean instances, rather than attempting bean lookup
 * by name one by one as requested by clients. BeanFactory implementations that
 * preload all their bean definitions (such as XML-based factories) may implement
 * this interface.
 *
 * <p>If this is a {@link HierarchicalBeanFactory}, the return values will <i>not</i>
 * take any BeanFactory hierarchy into account, but will relate only to the beans
 * defined in the current factory. Use the {@link BeanFactoryUtils} helper class
 * to consider beans in ancestor factories too.
 *
 * <p>The methods in this interface will just respect bean definitions of this factory.
 * They will ignore any singleton beans that have been registered by other means like
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}'s
 * {@code registerSingleton} method, with the exception of
 * {@code getBeanNamesForType} and {@code getBeansOfType} which will check
 * such manually registered singletons too. Of course, BeanFactory's {@code getBean}
 * does allow transparent access to such special beans as well. However, in typical
 * scenarios, all beans will be defined by external bean definitions anyway, so most
 * applications don't need to worry about this differentiation.
 *
 * <p><b>NOTE:</b> With the exception of {@code getBeanDefinitionCount}
 * and {@code containsBeanDefinition}, the methods in this interface
 * are not designed for frequent invocation. Implementations may be slow.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16 April 2001
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 */
// {@link BeanFactory} 接口的扩展由可以枚举其所有 bean 实例的 bean 工厂实现，而不是按照客户的要求通过名称一一尝试 bean 查找。
// 预加载所有 bean 定义的 BeanFactory 实现（例如基于 XML 的工厂）可以实现此接口
//
// <p>如果这是一个 {@link HierarchicalBeanFactory}，返回值将<i>不<i>考虑任何 BeanFactory 层次结构，而只会与当前工厂中
// 定义的 bean 相关。使用 {@link BeanFactoryUtils} 辅助类也可以考虑祖先工厂中的 bean。
//
// <p>这个接口中的方法只会尊重这个工厂的 bean 定义。他们将忽略通过其他方式注册的任何单例 bean，
// 例如 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 的 {@code registerSingleton} 方法，
// 除了 {@code getBeanNamesForType} 和 {@代码 getBeansOfType} 也将检查此类手动注册的单例。当然，
// BeanFactory 的 {@code getBean} 也允许透明访问这些特殊的 bean。但是，在典型的场景中，无论如何，所有的 bean 都会被外部 beanDefinition
// 定义，所以大多数应用程序不需要担心这种区分。
//
// <p><b>注意：<b> 除了 {@code getBeanDefinitionCount} 和 {@code containsBeanDefinition}，
// 此接口中的方法不是为频繁调用而设计的。实施可能很慢
//
// 在 Spring IoC 容器里边有且仅有一种默认实现就是 ListableBeanFactory
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 */
	// 检查此 bean 工厂是否包含具有给定名称的 bean 定义。 <p>不考虑该工厂可能参与的任何层次结构，
	// 并忽略通过除 bean 定义之外的其他方式注册的任何单例 bean。
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the number of beans defined in the factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @return the number of beans defined in the factory
	 */
	// 返回工厂中定义的 bean 数量。
	// 不考虑该工厂可能参与的任何层次结构，并忽略通过除 bean 定义之外的其他方式注册的任何单例 bean。
	// @return 工厂定义的bean数量
	int getBeanDefinitionCount();

	/**
	 * Return the names of all beans defined in this factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @return the names of all beans defined in this factory,
	 * or an empty array if none defined
	 */
	// 返回此工厂中定义的所有 bean 的名称。
	// <p>不考虑该工厂可能参与的任何层次结构，并忽略通过除 bean 定义之外的其他方式注册的任何单例 bean。
	// @return 在这个工厂中定义的所有 bean 的名称，如果没有定义，则返回一个空数组
	String[] getBeanDefinitionNames();

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 * with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 */
	// 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项
	// @param requiredType 类型 bean 必须匹配；可以是接口或超类
	// @param allowEagerInit 基于流的访问是否可以初始化<i>lazy-init singletons<i> 和
	// <i>由FactoryBeans<i>（或通过带有“factory-bean”引用的工厂方法）创建的对象以进行类型检查
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 * @param requiredType type the bean must match; can be a generic type declaration.
	 * Note that collection types are not supported here, in contrast to reflective
	 * injection points. For programmatically retrieving a list of beans matching a
	 * specific type, specify the actual bean type as an argument here and subsequently
	 * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 * with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	// 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	// 形参：requiredType – bean 必须匹配的类型； 可以是泛型类型声明。 请注意，与反射注入点相比，此处不支持集合类型。
	// 要以编程方式检索与特定类型匹配的 bean 列表，请在此处指定实际 bean 类型作为参数，然后使用 ObjectProvider.orderedStream()
	// 或其延迟流/迭代选项。
	// allowEagerInit – 基于流的访问是否可以初始化延迟初始化单例和由 FactoryBeans 创建的对象（或通过具有“工厂bean”
	// 引用的工厂方法）以进行类型检查
	// 返回值：相应的提供者句柄
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
	 * be it singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeanNamesForType(type, true, true)}.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 * @param type the generically typed class or interface to match
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @since 4.2
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 */
	// 在 FactoryBeans 的情况下，根据 bean 定义或 {@code getObjectType} 的值来判断，返回与给定类型（包括子类）匹配的 bean 的名称。
	// <p><b>注意：这个方法只内省顶级bean。<b>它<i>不<i>检查可能匹配指定类型的嵌套bean。
	// <p>是否考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。如果 FactoryBean 创建的对象不匹配，
	// 则原始 FactoryBean 本身将与类型匹配。
	// <p>不考虑该工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的 {@code beanNamesForTypeInducingAncestors}
	// 也将 bean 包含在祖先工厂中。
	// <p>注意：是否<i>不<i>忽略通过bean定义以外的其他方式注册的单例bean。
	// <p>此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。
	// 在大多数实现中，结果将与 {@code getBeanNamesForType(type, true, true)} 相同。
	// <p>此方法返回的Bean名称应始终按照后端配置中定义的顺序<i>返回Bean名称，并尽可能返回。
	// @param 键入通用类型的类或接口以匹配
	// @return 与给定对象类型（包括子类）匹配的 bean（或由 FactoryBeans 创建的对象）的名称，如果没有，则为空数组
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
	 * which means that FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
	 * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
	 * (which doesn't require initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 * @param type the generically typed class or interface to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @since 5.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType, boolean, boolean)
	 */
	// 返回与给定类型（包括子类）匹配的 bean 的名称，从 bean 定义或 FactoryBeans 的情况下的getObjectType值判断。
	// 注意：此方法仅内省顶级 bean。 它不检查嵌套豆可能匹配指定类型为好。
	// 如果设置了“allowEagerInit”标志，则考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	// 如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型匹配。 如果未设置“allowEagerInit”，
	// 则只会检查原始 FactoryBeans（不需要初始化每个 FactoryBean）。
	// 不考虑该工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的 beanNamesForTypeIncludingAncestors
	// 也将 bean 包含在祖先工厂中。
	// 注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	// 此方法返回的bean名称应该始终按照后端配置中定义的顺序返回bean名称，尽可能。
	//
	// 型参：
	//			type – 要匹配的通用类型的类或接口
	//			includeNonSingletons – 是否也包含原型或作用域 bean 或仅包含单例（也适用于 FactoryBeans）
	//			allowEagerInit – 是否为类型检查初始化由 FactoryBeans （或由带有“factory-bean”引用的工厂方法）创建的
	//			惰性初始化单例和对象。 请注意，FactoryBeans 需要急切地初始化以确定它们的类型：因此请注意，
	//			为此标志传入“true”将初始化 FactoryBeans 和“factory-bean”引用。
	//返回值：
	//			与给定对象类型（包括子类）匹配的 bean（或由 FactoryBeans 创建的对象）的名称，如果没有，则为空数组
	String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
	 * be it singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeanNamesForType(type, true, true)}.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all bean names
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	// 返回与给定类型（包括子类）匹配的 bean 的名称，从 bean 定义或 FactoryBeans 的情况下的getObjectType值判断。
	// 注意：此方法仅内省顶级 bean。 它不检查嵌套豆可能匹配指定类型为好。
	//
	// 不考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。 如果 FactoryBean 创建的对象不匹配，
	// 则原始 FactoryBean 本身将与类型匹配。
	//
	// 不考虑该工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的beanNamesForTypeIncludingAncestors 也将 bean
	// 包含在祖先工厂中。
	// 注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	// 此版本的getBeanNamesForType匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。 在大多数实现中，结果将与
	// getBeanNamesForType(type, true, true) 。
	// 此方法返回的bean名称应该始终按照后端配置中定义的顺序返回bean名称，尽可能。
	// 形参：
	// 			type – 要匹配的类或接口，或者所有 bean 名称为null
	// 返回值：
	//			与给定对象类型（包括子类）匹配的 bean（或由 FactoryBeans 创建的对象）的名称，如果没有，则为空数组
	String[] getBeanNamesForType(@Nullable Class<?> type);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
	 * which means that FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
	 * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
	 * (which doesn't require initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all bean names
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	// 返回与给定类型（包括子类）匹配的 bean 的名称，从 bean 定义或 FactoryBeans 的情况下的getObjectType值判断
	// 注意：此方法仅内省顶级 bean。 它不检查嵌套豆可能匹配指定类型为好。
	//
	// 如果设置了“allowEagerInit”标志，则考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	// 如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型匹配。 如果未设置“allowEagerInit”，
	// 则只会检查原始 FactoryBeans（不需要初始化每个 FactoryBean）。
	//
	// 注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	// 此方法返回的bean名称应该始终按照后端配置中定义的顺序返回bean名称，尽可能。
	// 形参：
	// 			type – 要匹配的类或接口，或者所有 bean 名称为null
	//			includeNonSingletons – 是否也包含原型或作用域 bean 或仅包含单例（也适用于 FactoryBeans）
	//			allowEagerInit – 是否为类型检查初始化由 FactoryBeans （或由带有“factory-bean”引用的工厂方法）创建的
	//			惰性初始化单例和对象。 请注意，FactoryBeans 需要急切地初始化以确定它们的类型：因此请注意，为此标志传入“true”
	//			将初始化 FactoryBeans 和“factory-bean”引用。
	// 返回值：
	//			与给定对象类型（包括子类）匹配的 bean（或由 FactoryBeans 创建的对象）的名称，如果没有，则为空数组
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * Return the bean instances that match the given object type (including
	 * subclasses), judging from either bean definitions or the value of
	 * {@code getObjectType} in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beansOfTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of getBeansOfType matches all kinds of beans, be it
	 * singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeansOfType(type, true, true)}.
	 * <p>The Map returned by this method should always return bean names and
	 * corresponding bean instances <i>in the order of definition</i> in the
	 * backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all concrete beans
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @since 1.1.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	// 返回与给定对象类型（包括子类）匹配的 bean 实例，从 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值判断。
	// <p><b>注意：这个方法只内省顶级bean。<b>它<i>不<i>检查可能匹配指定类型的嵌套bean。
	// <p>是否考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。如果 FactoryBean 创建的对象不匹配，
	// 则原始 FactoryBean 本身将与类型匹配。
	// <p>不考虑该工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的 {@code beansOfTypeInducingAncestors}
	// 也将 bean 包含在祖先工厂中。
	// <p>注意：是否<i>不<i>忽略通过bean定义以外的其他方式注册的单例bean。
	// <p>这个版本的 getBeansOfType 匹配所有类型的 bean，无论是单例、原型还是 FactoryBean。在大多数实现中，
	// 结果将与 {@code getBeansOfType(type, true, true)} 相同。
	// <p>这个方法返回的Map应该总是在后端配置中按照定义<i>的顺序返回bean名称和对应的bean实例<i>，尽可能的。
	// @param 键入要匹配的类或接口，或者 {@code null} 用于所有具体 bean
	// @return 具有匹配 bean 的 Map，包含 bean 名称作为键和相应的 bean 实例作为值
	//
	// 集合类型查找：安全，这个 bean 异常，指的是 bean 本身创建有问题，比如抽象类你去获取 bean
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;

	/**
	 * Return the bean instances that match the given object type (including
	 * subclasses), judging from either bean definitions or the value of
	 * {@code getObjectType} in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
	 * which means that FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
	 * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
	 * (which doesn't require initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beansOfTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>The Map returned by this method should always return bean names and
	 * corresponding bean instances <i>in the order of definition</i> in the
	 * backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all concrete beans
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	// 返回与给定对象类型（包括子类）匹配的 bean 实例，从 bean 定义或在 FactoryBeans 情况下的getObjectType值判断。
	// 注意：此方法仅内省顶级 bean。 它不检查嵌套豆可能匹配指定类型为好
	//
	// 如果设置了“allowEagerInit”标志，则考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	// 如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型匹配。 如果未设置“allowEagerInit”，
	// 则只会检查原始 FactoryBeans（不需要初始化每个 FactoryBean）
	//
	// 不考虑该工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的 beansOfTypeIncludingAncestors 也将 bean 包含在祖先工厂中。
	// 注意：不会忽略通过 bean 定义以外的其他方式注册的单例 bean。
	// 这个方法返回的Map应该尽可能按照后端配置中定义的顺序返回bean名称和对应的bean实例。
	// 形参：
	// 			type – 要匹配的类或接口，或者所有具体 bean 的null
	//			includeNonSingletons – 是否也包含原型或作用域 bean 或仅包含单例（也适用于 FactoryBeans）
	//			allowEagerInit – 是否为类型检查初始化由 FactoryBeans （或由带有“factory-bean”引用的工厂方法）创建
	//			的惰性初始化单例和对象。 请注意，FactoryBeans 需要急切地初始化以确定它们的类型：因此请注意，为此标志
	//			传入“true”将初始化 FactoryBeans 和“factory-bean”引用。
	//返回值：
	//			具有匹配 bean 的 Map，包含 bean 名称作为键和相应的 bean 实例作为值
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * Find all names of beans which are annotated with the supplied {@link Annotation}
	 * type, without creating corresponding bean instances yet.
	 * <p>Note that this method considers objects created by FactoryBeans, which means
	 * that FactoryBeans will get initialized in order to determine their object type.
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the names of all matching beans
	 * @since 4.0
	 * @see #findAnnotationOnBean
	 */
	// 查找使用提供的 {@link Annotation} 类型注释的 bean 的所有名称，而不创建相应的 bean 实例。
	// <p>请注意，此方法考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化以确定它们的对象类型
	// @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	// @return 所有匹配 bean 的名称
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * Find all beans which are annotated with the supplied {@link Annotation} type,
	 * returning a Map of bean names with corresponding bean instances.
	 * <p>Note that this method considers objects created by FactoryBeans, which means
	 * that FactoryBeans will get initialized in order to determine their object type.
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @since 3.0
	 * @see #findAnnotationOnBean
	 */
	// 查找使用提供的 {@link Annotation} 类型注释的所有 bean，返回带有相应 bean 实例的 bean 名称的 Map。
	// <p>请注意，此方法考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化以确定它们的对象类型
	// @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	// @return 带有匹配 bean 的 Map，包含 bean 名称作为键和相应的 bean 实例作为值
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * Find an {@link Annotation} of {@code annotationType} on the specified bean,
	 * traversing its interfaces and super classes if no annotation can be found on
	 * the given class itself, as well as checking the bean's factory method (if any).
	 * @param beanName the name of the bean to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the annotation of the given type if found, or {@code null} otherwise
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 3.0
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 */
	// 在指定的 bean 上找到 {@code annotationType} 的 {@link Annotation}，如果在给定的类本身上找不到注解，
	// 则遍历其接口和超类，并检查 bean 的工厂方法（如果有）
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

}
