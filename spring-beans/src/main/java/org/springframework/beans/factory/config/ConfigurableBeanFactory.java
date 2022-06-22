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

package org.springframework.beans.factory.config;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

/**
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link org.springframework.beans.factory.BeanFactory}
 * interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * needs. This extended interface is just meant to allow for framework-internal
 * plug'n'play and for special access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 */
// 大多数 bean 工厂要实现的配置接口。除了 {@link org.springframework.beans.factory.BeanFactory}
// 接口中的 bean factory client 方法之外，还提供配置 bean factory 的工具。
//
// <p>这个 bean factory 接口不打算在正常的应用程序代码中使用：
// 坚持 {@link org.springframework.beans.factory.BeanFactory} 或
// {@link org.springframework.beans.factory.ListableBeanFactory} 来满足典型需求.
// 这个扩展接口只是为了允许框架内部的即插即用和对 bean 工厂配置方法的特殊访问
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 * @see #registerScope
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * Scope identifier for the standard prototype scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 * @see #registerScope
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * Set the parent of this bean factory.
	 * <p>Note that the parent cannot be changed: It should only be set outside
	 * a constructor if it isn't available at the time of factory instantiation.
	 * @param parentBeanFactory the parent BeanFactory
	 * @throws IllegalStateException if this factory is already associated with
	 * a parent BeanFactory
	 * @see #getParentBeanFactory()
	 */
	// 设置这个 bean 工厂的父级。
	// <p>请注意，父级不能更改：如果在工厂实例化时它不可用，则只能在构造函数之外设置它
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * Set the class loader to use for loading bean classes.
	 * Default is the thread context class loader.
	 * <p>Note that this class loader will only apply to bean definitions
	 * that do not carry a resolved bean class yet. This is the case as of
	 * Spring 2.0 by default: Bean definitions only carry bean class names,
	 * to be resolved once the factory processes the bean definition.
	 * @param beanClassLoader the class loader to use,
	 * or {@code null} to suggest the default class loader
	 */
	// 设置用于加载 bean 类的类加载器。默认是线程上下文类加载器。
	// <p>请注意，此类加载器仅适用于尚未携带已解析 bean 类的 bean 定义。
	// 这是 Spring 2.0 默认情况下的情况：Bean 定义只携带 bean 类名，一旦工厂处理了 bean 定义就被解析。
	// @param beanClassLoader 要使用的类加载器，或者 {@code null} 建议使用默认的类加载器
	void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);

	/**
	 * Return this factory's class loader for loading bean classes
	 * (only {@code null} if even the system ClassLoader isn't accessible).
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Specify a temporary ClassLoader to use for type matching purposes.
	 * Default is none, simply using the standard bean ClassLoader.
	 * <p>A temporary ClassLoader is usually just specified if
	 * <i>load-time weaving</i> is involved, to make sure that actual bean
	 * classes are loaded as lazily as possible. The temporary loader is
	 * then removed once the BeanFactory completes its bootstrap phase.
	 * @since 2.5
	 */
	// 指定用于类型匹配目的的临时类加载器。默认为 none，只需使用标准 bean ClassLoader。
	// <p>如果涉及<i>加载时编织<i>，通常只指定临时类加载器，以确保尽可能延迟加载实际的bean类。
	// 一旦 BeanFactory 完成其引导阶段，临时加载器就会被删除
	void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

	/**
	 * Return the temporary ClassLoader to use for type matching purposes,
	 * if any.
	 * @since 2.5
	 */
	// 返回临时 ClassLoader 以用于类型匹配目的（如果有）
	@Nullable
	ClassLoader getTempClassLoader();

	/**
	 * Set whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes. Default is on.
	 * <p>Turn this flag off to enable hot-refreshing of bean definition objects
	 * and in particular bean classes. If this flag is off, any creation of a bean
	 * instance will re-query the bean class loader for newly resolved classes.
	 */
	// 设置是否缓存 bean 元数据，例如给定的 bean 定义（以合并方式）和解析的 bean 类。默认开启。
	// <p>关闭此标志以启用 bean 定义对象和特定 bean 类的热刷新。如果此标志关闭，
	// 则任何 bean 实例的创建都将重新查询 bean 类加载器以获取新解析的类。
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * Return whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes.
	 */
	// 返回是否缓存 bean 元数据，例如给定的 bean 定义（以合并方式）和解析的 bean 类
	boolean isCacheBeanMetadata();

	/**
	 * Specify the resolution strategy for expressions in bean definition values.
	 * <p>There is no expression support active in a BeanFactory by default.
	 * An ApplicationContext will typically set a standard expression strategy
	 * here, supporting "#{...}" expressions in a Unified EL compatible style.
	 * @since 3.0
	 */
	// 指定 bean 定义值中表达式的解析策略。
	// <p>默认情况下，BeanFactory 中没有激活的表达式支持。
	// ApplicationContext 通常会在此处设置标准表达式策略，以统一兼容支持“#{...}”EL样式表达式。
	void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);

	/**
	 * Return the resolution strategy for expressions in bean definition values.
	 * @since 3.0
	 */
	// 返回 bean 定义值中表达式的解析策略
	@Nullable
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 * @since 3.0
	 */
	// 指定用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代方法。
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 * @since 3.0
	 */
	// 返回关联的 ConversionService（如果有）
	@Nullable
	ConversionService getConversionService();

	/**
	 * Add a PropertyEditorRegistrar to be applied to all bean creation processes.
	 * <p>Such a registrar creates new PropertyEditor instances and registers them
	 * on the given registry, fresh for each bean creation attempt. This avoids
	 * the need for synchronization on custom editors; hence, it is generally
	 * preferable to use this method instead of {@link #registerCustomEditor}.
	 * @param registrar the PropertyEditorRegistrar to register
	 */
	// 添加要应用于所有 bean 创建过程的 PropertyEditorRegistrar。
	// <p>这样的注册器创建新的 PropertyEditor 实例并将它们注册到给定的注册表中，每次创建 bean 时都是新鲜的。
	// 这避免了在自定义编辑器上同步的需要；因此，通常最好使用此方法而不是 {@link registerCustomEditor}。
	// @param registrar 要注册的 PropertyEditorRegistrar
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * Register the given custom property editor for all properties of the
	 * given type. To be invoked during factory configuration.
	 * <p>Note that this method will register a shared custom editor instance;
	 * access to that instance will be synchronized for thread-safety. It is
	 * generally preferable to use {@link #addPropertyEditorRegistrar} instead
	 * of this method, to avoid for the need for synchronization on custom editors.
	 * @param requiredType type of the property
	 * @param propertyEditorClass the {@link PropertyEditor} class to register
	 */
	// 为给定类型的所有属性注册给定的自定义属性编辑器。在出厂配置期间调用。
	// <p>注意这个方法会注册一个共享的自定义编辑器实例；为了线程安全，对该实例的访问将被同步。
	// 通常最好使用 {@link addPropertyEditorRegistrar} 而不是此方法，以避免需要在自定义编辑器上进行同步。
	// @param requiredType 属性的类型 @param propertyEditorClass 要注册的 {@link PropertyEditor} 类
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	// 使用已向此 BeanFactory 注册的自定义编辑器初始化给定的 PropertyEditorRegistry。
	// @param registry 要初始化的 PropertyEditorRegistry
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * Set a custom type converter that this BeanFactory should use for converting
	 * bean property values, constructor argument values, etc.
	 * <p>This will override the default PropertyEditor mechanism and hence make
	 * any custom editors or custom editor registrars irrelevant.
	 * @since 2.5
	 * @see #addPropertyEditorRegistrar
	 * @see #registerCustomEditor
	 */
	// 设置一个自定义类型转换器，此 BeanFactory 应该使用它来转换 bean 属性值、构造函数参数值等。
	// <p>这将覆盖默认的 PropertyEditor 机制，从而使任何自定义编辑器或自定义编辑器注册器变得无关紧要。
	// @since 2.5 @see addPropertyEditorRegistrar @see registerCustomEditor
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * Obtain a type converter as used by this BeanFactory. This may be a fresh
	 * instance for each call, since TypeConverters are usually <i>not</i> thread-safe.
	 * <p>If the default PropertyEditor mechanism is active, the returned
	 * TypeConverter will be aware of all custom editors that have been registered.
	 * @since 2.5
	 */
	// 获取此 BeanFactory 使用的类型转换器。
	// 这可能是每次调用的新实例，因为 TypeConverters 通常 <i>not<i> 线程安全。
	// <p>如果默认的 PropertyEditor 机制处于活动状态，则返回的 TypeConverter 将知道所有已注册的自定义编辑器。
	TypeConverter getTypeConverter();

	/**
	 * Add a String resolver for embedded values such as annotation attributes.
	 * @param valueResolver the String resolver to apply to embedded values
	 * @since 3.0
	 */
	// 为嵌入值（例如注释属性）添加字符串解析器。 @param valueResolver 应用于嵌入值的字符串解析器
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * Determine whether an embedded value resolver has been registered with this
	 * bean factory, to be applied through {@link #resolveEmbeddedValue(String)}.
	 * @since 4.3
	 */
	// 确定嵌入值解析器是否已注册到此 bean 工厂，以通过 {@link resolveEmbeddedValue(String)} 应用。
	boolean hasEmbeddedValueResolver();

	/**
	 * Resolve the given embedded value, e.g. an annotation attribute.
	 * @param value the value to resolve
	 * @return the resolved value (may be the original value as-is)
	 * @since 3.0
	 */
	// 解析给定的嵌入值，例如一个注释属性。
	@Nullable
	String resolveEmbeddedValue(String value);

	/**
	 * Add a new BeanPostProcessor that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 * <p>Note: Post-processors submitted here will be applied in the order of
	 * registration; any ordering semantics expressed through implementing the
	 * {@link org.springframework.core.Ordered} interface will be ignored. Note
	 * that autodetected post-processors (e.g. as beans in an ApplicationContext)
	 * will always be applied after programmatically registered ones.
	 * @param beanPostProcessor the post-processor to register
	 */
	// 添加一个新的 BeanPostProcessor，它将应用于此工厂创建的 bean。在出厂配置期间调用。
	// <p>注：此处提交的后处理器将按注册顺序申请；通过实现 {@link org.springframework.core.Ordered} 接口表达的
	// 任何排序语义都将被忽略。请注意，自动检测的后处理器（例如作为 ApplicationContext 中的 bean）将始终在以编程方
	// 式注册的后处理器之后应用。 @param beanPostProcessor 要注册的后处理器
	// beanFactory 和 BeanPostProcessor 有且仅有这么一种方式进行注册，
	// {@link AbstractApplicationContext}#registerBeanPostProcessors
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * Return the current number of registered BeanPostProcessors, if any.
	 */
	// 返回当前注册的 BeanPostProcessor 数量（如果有）
	int getBeanPostProcessorCount();

	/**
	 * Register the given scope, backed by the given Scope implementation.
	 * @param scopeName the scope identifier
	 * @param scope the backing Scope implementation
	 */
	// 注册给定的范围，由给定的 Scope 实现支持。
	void registerScope(String scopeName, Scope scope);

	/**
	 * Return the names of all currently registered scopes.
	 * <p>This will only return the names of explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 * @return the array of scope names, or an empty array if none
	 * @see #registerScope
	 */
	// 返回所有当前注册 Scope 的名称。
	// <p>这只会返回显式注册的 Scope 的名称。不会暴露诸如“单例”和“原型”之类的内置范围。
	// @return 范围名称数组，如果没有则为空数组
	// @see registerScope
	String[] getRegisteredScopeNames();

	/**
	 * Return the Scope implementation for the given scope name, if any.
	 * <p>This will only return explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 * @param scopeName the name of the scope
	 * @return the registered Scope implementation, or {@code null} if none
	 * @see #registerScope
	 */
	// 返回给定范围名称的 Scope 实现（如果有）。
	// <p>这只会返回显式注册的 Scope。
	// 不会暴露诸如“单例”和“原型”之类的内置范围。
	// @param scopeName 作用域的名称
	// @return 注册的作用域实现，或者 {@code null} 如果没有
	// @see registerScope
	@Nullable
	Scope getRegisteredScope(String scopeName);

	/**
	 * Set the {@code ApplicationStartup} for this bean factory.
	 * <p>This allows the application context to record metrics during application startup.
	 * @param applicationStartup the new application startup
	 * @since 5.3
	 */
	// 为这个 bean 工厂设置 {@code ApplicationStartup}。
	// <p>这允许应用程序上下文在应用程序启动期间记录指标。
	// @param applicationStartup 新的应用程序启动
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * Return the {@code ApplicationStartup} for this bean factory.
	 * @since 5.3
	 */
	// 返回此 bean 工厂的 {@code ApplicationStartup}。
	ApplicationStartup getApplicationStartup();

	/**
	 * Provides a security access control context relevant to this factory.
	 * @return the applicable AccessControlContext (never {@code null})
	 * @since 3.0
	 */
	// 提供与此工厂相关的安全访问控制上下文。 @return 适用的 AccessControlContext（从不{@code null}）
	AccessControlContext getAccessControlContext();

	/**
	 * Copy all relevant configuration from the given other factory.
	 * <p>Should include all standard configuration settings as well as
	 * BeanPostProcessors, Scopes, and factory-specific internal settings.
	 * Should not include any metadata of actual bean definitions,
	 * such as BeanDefinition objects and bean name aliases.
	 * @param otherFactory the other BeanFactory to copy from
	 */
	// 从给定的其他工厂复制所有相关配置
	// <p>应包括所有标准配置设置以及 BeanPostProcessors、范围和工厂特定的内部设置。
	// 不应包含实际 bean 定义的任何元数据，例如 BeanDefinition 对象和 bean 名称别名。
	// @param otherFactory 要从中复制的另一个 BeanFactory
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * Given a bean name, create an alias. We typically use this method to
	 * support names that are illegal within XML ids (used for bean names).
	 * <p>Typically invoked during factory configuration, but can also be
	 * used for runtime registration of aliases. Therefore, a factory
	 * implementation should synchronize alias access.
	 * @param beanName the canonical name of the target bean
	 * @param alias the alias to be registered for the bean
	 * @throws BeanDefinitionStoreException if the alias is already in use
	 */
	// 给定一个 bean 名称，创建一个别名。我们通常使用此方法来支持 XML id 中非法的名称（用于 bean 名称）。
	// <p>通常在工厂配置期间调用，但也可用于别名的运行时注册。因此，工厂实现应该同步别名访问。
	// @param beanName 目标 bean 的规范名称
	// @param alias 为 bean 注册的别名
	// @throws BeanDefinitionStoreException 如果别名已被使用
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 * @since 2.5
	 */
	// 解析在此工厂中注册的所有别名目标名称和别名，将给定的 StringValueResolver 应用于它们
	// <p>例如，值解析器可以解析目标 bean 名称甚至别名中的占位符。
	// @param valueResolver 要应用的 StringValueResolver
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * Return a merged BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * Considers bean definitions in ancestor factories as well.
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) BeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean definition with the given name
	 * @since 2.5
	 */
	// 返回给定 bean 名称的合并 BeanDefinition，如有必要，将子 bean 定义与其父级合并。 还要考虑
	// 祖先工厂中的 bean 定义。即递归合并，直到Object
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Determine whether the bean with the given name is a FactoryBean.
	 * @param name the name of the bean to check
	 * @return whether the bean is a FactoryBean
	 * ({@code false} means the bean exists but is not a FactoryBean)
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 2.5
	 */
	// 确定具有给定名称的 bean 是否为 FactoryBean。
	// @param name 要检查的 bean 的名称
	// @return bean 是否是 FactoryBean（{@code false} 表示 bean 存在但不是 FactoryBean）
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Explicitly control the current in-creation status of the specified bean.
	 * For container-internal use only.
	 * @param beanName the name of the bean
	 * @param inCreation whether the bean is currently in creation
	 * @since 3.1
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * Determine whether the specified bean is currently in creation.
	 * @param beanName the name of the bean
	 * @return whether the bean is currently in creation
	 * @since 2.5
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 * @since 2.5
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 * @since 2.5
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 * @since 2.5
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to its bean definition.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 * @param beanName the name of the bean definition
	 * @param beanInstance the bean instance to destroy
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * Destroy the specified scoped bean in the current target scope, if any.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 * @param beanName the name of the scoped bean
	 */
	void destroyScopedBean(String beanName);

	/**
	 * Destroy all singleton beans in this factory, including inner beans that have
	 * been registered as disposable. To be called on shutdown of a factory.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 */
	void destroySingletons();

}
