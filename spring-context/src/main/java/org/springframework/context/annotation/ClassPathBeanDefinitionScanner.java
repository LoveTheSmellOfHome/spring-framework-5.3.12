/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A bean definition scanner that detects bean candidates on the classpath,
 * registering corresponding bean definitions with a given registry ({@code BeanFactory}
 * or {@code ApplicationContext}).
 *
 * <p>Candidate classes are detected through configurable type filters. The
 * default filters include classes that are annotated with Spring's
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Repository @Repository},
 * {@link org.springframework.stereotype.Service @Service}, or
 * {@link org.springframework.stereotype.Controller @Controller} stereotype.
 *
 * <p>Also supports Java EE 6's {@link javax.annotation.ManagedBean} and
 * JSR-330's {@link javax.inject.Named} annotations, if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotationConfigApplicationContext#scan
 * @see org.springframework.stereotype.Component
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.stereotype.Service
 * @see org.springframework.stereotype.Controller
 */
// 一个 bean 定义扫描器，它检测 classpath 上的 bean 候选者，使用给定的注册表（BeanFactory 或
// ApplicationContext）注册相应的 bean 定义，作用：通过类路径扫描得到 BeanDefinition 并注册
//
// 通过可配置的类型过滤器检测候选类。默认过滤器包括使用 Spring 的 @Component、@Repository、@Service 或
// @Controller 等模型注释的类。
//
// 还支持 Java EE 6 的 javax.annotation.ManagedBean 和 JSR-330 的 javax.inject.Named 注解（如果可用）
// 使用@Component 模式注解指定包路径扫描的方式,扫描相关包下的Class 加载成 bean.
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

	// BeanDefinitionRegistry
	private final BeanDefinitionRegistry registry;

	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

	// 自动装配候选模式，从 xml 中读入
	@Nullable
	private String[] autowireCandidatePatterns;

	// bean 的取名字神器
	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	// bean 作用域
	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	// 包括注解配置
	private boolean includeAnnotationConfig = true;


	/**
	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
	 * of a {@code BeanDefinitionRegistry}
	 */
	// 为给定的 bean 工厂创建一个新的 ClassPathBeanDefinitionScanner 。
	// 参形：
	//				registry - 以 BeanDefinitionRegistry 的形式将 bean 定义加载到其中的 BeanFactory
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
		this(registry, true);
	}

	/**
	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
	 * <p>If the passed-in bean factory does not only implement the
	 * {@code BeanDefinitionRegistry} interface but also the {@code ResourceLoader}
	 * interface, it will be used as default {@code ResourceLoader} as well. This will
	 * usually be the case for {@link org.springframework.context.ApplicationContext}
	 * implementations.
	 * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
	 * will be a {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
	 * environment will be used by this reader.  Otherwise, the reader will initialize and
	 * use a {@link org.springframework.core.env.StandardEnvironment}. All
	 * {@code ApplicationContext} implementations are {@code EnvironmentCapable}, while
	 * normal {@code BeanFactory} implementations are not.
	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
	 * of a {@code BeanDefinitionRegistry}
	 * @param useDefaultFilters whether to include the default filters for the
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service}, and
	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	// 为给定的 bean 工厂创建一个新的ClassPathBeanDefinitionScanner 。
	//
	// 如果传入的 bean factory 不仅实现了BeanDefinitionRegistry 接口，还实现了 ResourceLoader 接口，
	// 那么它也会被用作默认的 ResourceLoader 。这通常是 org.springframework.context.ApplicationContext 实现的情况。
	//
	// 如果给定一个普通的 BeanDefinitionRegistry ，默认的 ResourceLoader 将是一个
	// org.springframework.core.io.support.PathMatchingResourcePatternResolver 。
	//
	// 如果传入的 bean 工厂也实现了 EnvironmentCapable 它的环境将被这个读者使用。否则，阅读器将初始化并使用 StandardEnvironment 。
	// 所有 ApplicationContext 实现都是 EnvironmentCapable ，而普通的 BeanFactory 实现不是。
	// 参形：
	//				registry - 以 BeanDefinitionRegistry 的形式将 bean 定义加载到其中的 BeanFactory
	//				useDefaultFilters – 是否包含 @Component 、 @Repository 、 @Service和 @Controller 原型注解的默认过滤器
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
	}

	/**
	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
	 * using the given {@link Environment} when evaluating bean definition profile metadata.
	 * <p>If the passed-in bean factory does not only implement the {@code
	 * BeanDefinitionRegistry} interface but also the {@link ResourceLoader} interface, it
	 * will be used as default {@code ResourceLoader} as well. This will usually be the
	 * case for {@link org.springframework.context.ApplicationContext} implementations.
	 * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
	 * will be a {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
	 * of a {@code BeanDefinitionRegistry}
	 * @param useDefaultFilters whether to include the default filters for the
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service}, and
	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
	 * @param environment the Spring {@link Environment} to use when evaluating bean
	 * definition profile metadata
	 * @since 3.1
	 * @see #setResourceLoader
	 */
	// 为给定的 bean 工厂创建一个新的 ClassPathBeanDefinitionScanner ，并在评估 bean 定义配置文件元数据时使用给定的 Environment 。
	//
	// 如果传入的 bean factory 不仅实现了 BeanDefinitionRegistry 接口，还实现了 ResourceLoader 接口，
	// 它也会被用作默认的 ResourceLoader 。这通常是 org.springframework.context.ApplicationContext 实现的情况。
	//
	// 如果给定一个普通的 BeanDefinitionRegistry ，默认的 ResourceLoader 将是一个
	// org.springframework.core.io.support.PathMatchingResourcePatternResolver 。
	// 参形：
	//				registry - 以 BeanDefinitionRegistry 的形式将 bean 定义加载到其中的 BeanFactory
	//				useDefaultFilters – 是否包含 @Component 、 @Repository 、 @Service 和 @Controller 原型注解的默认过滤器
	//				environment – 评估 bean 定义配置文件元数据时使用的 Spring Environment
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment) {

		this(registry, useDefaultFilters, environment,
				(registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
	}

	/**
	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
	 * using the given {@link Environment} when evaluating bean definition profile metadata.
	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
	 * of a {@code BeanDefinitionRegistry}
	 * @param useDefaultFilters whether to include the default filters for the
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service}, and
	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
	 * @param environment the Spring {@link Environment} to use when evaluating bean
	 * definition profile metadata
	 * @param resourceLoader the {@link ResourceLoader} to use
	 * @since 4.3.6
	 */
	// 为给定的 bean 工厂创建一个新的 ClassPathBeanDefinitionScanner ，并在评估 bean 定义
	// 配置文件元数据时使用给定的 Environment 。
	// 参形：
	//			registry - 以 BeanDefinitionRegistry 的形式将 bean 定义加载到其中的 BeanFactory
	//			useDefaultFilters – 是否包含 @Component 、 @Repository 、 @Service和 @Controller 原型注解的默认过滤器
	//			environment – 评估 bean 定义配置文件元数据时使用的 Spring Environment
	//			resourceLoader – 要使用的 ResourceLoader
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment, @Nullable ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		if (useDefaultFilters) {
			// 将所有 @Component 注解加入到 List<TypeFilter> 集合过滤器中
			registerDefaultFilters();
		}
		// 设置环境
		setEnvironment(environment);
		// 设置资源加载器
		setResourceLoader(resourceLoader);
	}


	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	// 返回此扫描器操作的 BeanDefinitionRegistry
	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the defaults to use for detected beans.
	 * @see BeanDefinitionDefaults
	 */
	// 设置用于检测到的 BeanDefinitionDefaults 的默认值
	public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults =
				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
	}

	/**
	 * Return the defaults to use for detected beans (never {@code null}).
	 * @since 4.1
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	/**
	 * Set the name-matching patterns for determining autowire candidates.
	 * @param autowireCandidatePatterns the patterns to match against
	 */
	// 设置了自动绑定模式
	public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>Default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * Note that this will override any custom "scopedProxyMode" setting.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * @see #setScopedProxyMode
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}

	/**
	 * Specify the proxy behavior for non-singleton scoped beans.
	 * Note that this will override any custom "scopeMetadataResolver" setting.
	 * <p>The default is {@link ScopedProxyMode#NO}.
	 * @see #setScopeMetadataResolver
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * Specify whether to register annotation config post-processors.
	 * <p>The default is to register the post-processors. Turn this off
	 * to be able to ignore the annotations or to process them differently.
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	/**
	 * Perform a scan within the specified base packages.
	 * @param basePackages the packages to check for annotated classes
	 * @return number of beans registered
	 */
	// 在指定的基本包中执行扫描。
	// 参形：
	//				basePackages – 检查注释类的包
	// 返回值：      注册的 bean 数量
	public int scan(String... basePackages) {
		// 获取 bean 注册中心中 bean 定义的数量，一个 BeanDefinition 可以创建多个 bean，BeanDefinition 是唯一的
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

		// 扫描包向注册中心注册 Bean 定义
		doScan(basePackages);

		// Register annotation config processors, if necessary.
		// 如有必要，注册注解配置处理器
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}

		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}

	/**
	 * Perform a scan within the specified base packages,
	 * returning the registered bean definitions.
	 * <p>This method does <i>not</i> register an annotation config processor
	 * but rather leaves this up to the caller.
	 * @param basePackages the packages to check for annotated classes
	 * @return set of beans registered if any for tooling registration purposes (never {@code null})
	 */
	// 在指定的基本包内执行扫描，返回注册的 bean 定义。此方法不注册注解配置处理器，而是将其留给调用者
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		for (String basePackage : basePackages) {
			// 加载指定路径下所有类，获取类的 BeanDefinition 集合
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates) {
				// 获取 bean 的作用范围，默认是单例
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				// 设置作用域
				candidate.setScope(scopeMetadata.getScopeName());
				// 获取 beanName
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				// 如果是 AbstractBeanDefinition
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				// 如果是 AnnotatedBeanDefinition
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				// 检查容器中是否不存在 candidate BeanDefinition
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);

					// 重点
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}

	/**
	 * Apply further settings to the given bean definition,
	 * beyond the contents retrieved from scanning the component class.
	 * @param beanDefinition the scanned bean definition
	 * @param beanName the generated bean name for the given bean
	 */
	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
		// 设置默认属性
		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
		if (this.autowireCandidatePatterns != null) {
			// 设置 Autowire 候选
			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
		}
	}

	/**
	 * Register the specified bean with the given registry.
	 * <p>Can be overridden in subclasses, e.g. to adapt the registration
	 * process or to register further bean definitions for each scanned bean.
	 * @param definitionHolder the bean definition plus bean name for the bean
	 * @param registry the BeanDefinitionRegistry to register the bean with
	 */
	// 使用给定的注册表注册指定的 bean。
	// 可以在子类中覆盖，例如调整注册过程或为每个扫描的 bean 注册进一步的 bean 定义。
	// 参形：
	//			definitionHolder – bean 定义加上 bean 的 bean 名称
	//			registry - 注册 bean 的 BeanDefinitionRegistry
	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
		// 重点：注册 BeanDefinition
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}


	/**
	 * Check the given candidate's bean name, determining whether the corresponding
	 * bean definition needs to be registered or conflicts with an existing definition.
	 * @param beanName the suggested name for the bean
	 * @param beanDefinition the corresponding bean definition
	 * @return {@code true} if the bean can be registered as-is;
	 * {@code false} if it should be skipped because there is an
	 * existing, compatible bean definition for the specified name
	 * @throws ConflictingBeanDefinitionException if an existing, incompatible
	 * bean definition has been found for the specified name
	 */
	// 检查给定候选的 bean 名称，确定相应的 bean 定义是否需要注册或与现有定义冲突。
	// 参形：
	//				beanName – bean 的建议名称
	//				beanDefinition – 对应的 bean 定义
	// 返回值：
	//				如果 bean 可以按原样注册，则为 true ；如果由于指定名称存在现有的兼容 bean 定义而应该跳过它，则返回false
	// 抛出：
	//				ConflictingBeanDefinitionException – 如果为指定名称找到现有的、不兼容的 bean 定义
	//				IllegalStateException
	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return true;
		}
		// 从注册中心获取 BeanDefinition
		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
		// 获取原始的 BeanDefinition
		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
		if (originatingDef != null) {
			existingDef = originatingDef;
		}
		// 判断传入的 BeanDefinition 和容器中的 BeanDefinition 是否相同，相同返回 false
		if (isCompatible(beanDefinition, existingDef)) {
			return false;
		}
		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
	}

	/**
	 * Determine whether the given new bean definition is compatible with
	 * the given existing bean definition.
	 * <p>The default implementation considers them as compatible when the existing
	 * bean definition comes from the same source or from a non-scanning source.
	 * @param newDefinition the new bean definition, originated from scanning
	 * @param existingDefinition the existing bean definition, potentially an
	 * explicitly defined one or a previously generated one from scanning
	 * @return whether the definitions are considered as compatible, with the
	 * new definition to be skipped in favor of the existing definition
	 */
	// 确定给定的新 bean 定义是否与给定的现有 bean 定义兼容。
	// 当现有 bean 定义来自同一源或来自非扫描源时，默认实现认为它们是兼容的。
	// 参形：
	//				newDefinition – 新的 bean 定义，源自扫描
	//				existingDefinition – 现有的 bean 定义，可能是显式定义的定义或先前通过扫描生成的定义
	// 返回值：
	//				定义是否被认为是兼容的，新定义是否被跳过以支持现有定义
	protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
		return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
				(newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // scanned same file twice
				newDefinition.equals(existingDefinition));  // scanned equivalent class twice
	}


	/**
	 * Get the Environment from the given registry if possible, otherwise return a new
	 * StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
