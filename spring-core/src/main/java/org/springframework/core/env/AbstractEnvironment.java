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

package org.springframework.core.env;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link Environment} implementations. Supports the notion of
 * reserved default profile names and enables specifying active and default profiles
 * through the {@link #ACTIVE_PROFILES_PROPERTY_NAME} and
 * {@link #DEFAULT_PROFILES_PROPERTY_NAME} properties.
 *
 * <p>Concrete subclasses differ primarily on which {@link PropertySource} objects they
 * add by default. {@code AbstractEnvironment} adds none. Subclasses should contribute
 * property sources through the protected {@link #customizePropertySources(MutablePropertySources)}
 * hook, while clients should customize using {@link ConfigurableEnvironment#getPropertySources()}
 * and working against the {@link MutablePropertySources} API.
 * See {@link ConfigurableEnvironment} javadoc for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 */
// {@link Environment} 实现的抽象基类。支持保留默认配置文件名称的概念，并允许通过 {@link #ACTIVE_PROFILES_PROPERTY_NAME} 和
// {@link #DEFAULT_PROFILES_PROPERTY_NAME} 属性指定活动和默认配置文件
// <p>具体子类的主要区别在于它们默认添加的 {@link PropertySource} 对象。 {@code AbstractEnvironment} 不添加任何内容。
// 子类应该通过受保护的 {@link #customizePropertySources(MutablePropertySources)} 钩子贡献属性源，
// 而客户端应该使用 {@link ConfigurableEnvironment#getPropertySources()} 进行自定义并针对
// {@link MutablePropertySources} API 工作。有关用法示例，请参阅 {@link ConfigurableEnvironment} javadoc。
public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * System property that instructs Spring to ignore system environment variables,
	 * i.e. to never attempt to retrieve such a variable via {@link System#getenv()}.
	 * <p>The default is "false", falling back to system environment variable checks if a
	 * Spring environment property (e.g. a placeholder in a configuration String) isn't
	 * resolvable otherwise. Consider switching this flag to "true" if you experience
	 * log warnings from {@code getenv} calls coming from Spring, e.g. on WebSphere
	 * with strict SecurityManager settings and AccessControlExceptions warnings.
	 * @see #suppressGetenvAccess()
	 */
	// 指示 Spring 忽略系统环境变量的系统属性，即永远不要尝试通过 {@link System#getenv()} 检索此类变量。
	// <p>默认为“false”，回退到系统环境变量检查 Spring 环境属性（例如配置字符串中的占位符）是否无法解析。
	// 如果您遇到来自 Spring 的 {@code getenv} 调用的日志警告，请考虑将此标志切换为“true”，例如在具有严格 SecurityManager
	// 设置和 AccessControlExceptions 警告的 WebSphere 上。
	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";

	/**
	 * Name of property to set to specify active profiles: {@value}. Value may be comma
	 * delimited.
	 * <p>Note that certain shell environments such as Bash disallow the use of the period
	 * character in variable names. Assuming that Spring's {@link SystemEnvironmentPropertySource}
	 * is in use, this property may be specified as an environment variable as
	 * {@code SPRING_PROFILES_ACTIVE}.
	 * @see ConfigurableEnvironment#setActiveProfiles
	 */
	// 要设置以指定激活配置文件的属性名称：{@value}。值可以用逗号分隔
	// <p>请注意，某些 shell 环境（例如 Bash）不允许在变量名称中使用句点字符。假设 Spring 的
	// {@link SystemEnvironmentPropertySource} 正在使用中，则可以将此属性指定为环境变量，如 {@code SPRING_PROFILES_ACTIVE}。
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * Name of property to set to specify profiles active by default: {@value}. Value may
	 * be comma delimited.
	 * <p>Note that certain shell environments such as Bash disallow the use of the period
	 * character in variable names. Assuming that Spring's {@link SystemEnvironmentPropertySource}
	 * is in use, this property may be specified as an environment variable as
	 * {@code SPRING_PROFILES_DEFAULT}.
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	// 要设置以指定默认情况下处于活动状态的配置文件的属性名称：{@value}。值可以用逗号分隔
	// <p>请注意，某些 shell 环境（例如 Bash）不允许在变量名称中使用句点字符。假设 Spring 的
	// {@link SystemEnvironmentPropertySource} 正在使用中，则可以将此属性指定为环境变量 {@code SPRING_PROFILES_DEFAULT}。
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	/**
	 * Name of reserved default profile name: {@value}. If no default profile names are
	 * explicitly and no active profile names are explicitly set, this profile will
	 * automatically be activated by default.
	 * @see #getReservedDefaultProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	// 保留的默认配置文件名称：{@value}。如果没有明确的默认配置文件名称并且没有明确设置活动配置文件名称，则默认情况下将自动激活此配置文件
	protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<String> activeProfiles = new LinkedHashSet<>();

	private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());

	private final MutablePropertySources propertySources;

	private final ConfigurablePropertyResolver propertyResolver;


	/**
	 * Create a new {@code Environment} instance, calling back to
	 * {@link #customizePropertySources(MutablePropertySources)} during construction to
	 * allow subclasses to contribute or manipulate {@link PropertySource} instances as
	 * appropriate.
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	// 创建一个新的 {@code Environment} 实例，在构造期间回调 {@link #customizePropertySources(MutablePropertySources)}
	// 以允许子类根据需要贡献或操作 {@link PropertySource} 实例。
	public AbstractEnvironment() {
		this(new MutablePropertySources());
	}

	/**
	 * Create a new {@code Environment} instance with a specific
	 * {@link MutablePropertySources} instance, calling back to
	 * {@link #customizePropertySources(MutablePropertySources)} during
	 * construction to allow subclasses to contribute or manipulate
	 * {@link PropertySource} instances as appropriate.
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	// 使用特定的 {@link MutablePropertySources} 实例创建一个新的 {@code Environment} 实例，在构造期间回调
	// {@link CustomizePropertySources(MutablePropertySources)} 以允许子类酌情贡献或操作 {@link PropertySource} 实例。
	protected AbstractEnvironment(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.propertyResolver = createPropertyResolver(propertySources);
		customizePropertySources(propertySources);
	}


	/**
	 * Factory method used to create the {@link ConfigurablePropertyResolver}
	 * instance used by the Environment.
	 * @since 5.3.4
	 * @see #getPropertyResolver()
	 */
	// 用于创建环境使用的 {@link ConfigurablePropertyResolver} 实例的工厂方法
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return new PropertySourcesPropertyResolver(propertySources);
	}

	/**
	 * Return the {@link ConfigurablePropertyResolver} being used by the
	 * {@link Environment}.
	 * @since 5.3.4
	 * @see #createPropertyResolver(MutablePropertySources)
	 */
	// 返回 {@link Environment} 使用的 {@link ConfigurablePropertyResolver}
	protected final ConfigurablePropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	/**
	 * Customize the set of {@link PropertySource} objects to be searched by this
	 * {@code Environment} during calls to {@link #getProperty(String)} and related
	 * methods.
	 *
	 * <p>Subclasses that override this method are encouraged to add property
	 * sources using {@link MutablePropertySources#addLast(PropertySource)} such that
	 * further subclasses may call {@code super.customizePropertySources()} with
	 * predictable results. For example:
	 *
	 * <pre class="code">
	 * public class Level1Environment extends AbstractEnvironment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // no-op from base class
	 *         propertySources.addLast(new PropertySourceA(...));
	 *         propertySources.addLast(new PropertySourceB(...));
	 *     }
	 * }
	 *
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>In this arrangement, properties will be resolved against sources A, B, C, D in that
	 * order. That is to say that property source "A" has precedence over property source
	 * "D". If the {@code Level2Environment} subclass wished to give property sources C
	 * and D higher precedence than A and B, it could simply call
	 * {@code super.customizePropertySources} after, rather than before adding its own:
	 *
	 * <pre class="code">
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>The search order is now C, D, A, B as desired.
	 *
	 * <p>Beyond these recommendations, subclasses may use any of the {@code add*},
	 * {@code remove}, or {@code replace} methods exposed by {@link MutablePropertySources}
	 * in order to create the exact arrangement of property sources desired.
	 *
	 * <p>The base implementation registers no property sources.
	 *
	 * <p>Note that clients of any {@link ConfigurableEnvironment} may further customize
	 * property sources via the {@link #getPropertySources()} accessor, typically within
	 * an {@link org.springframework.context.ApplicationContextInitializer
	 * ApplicationContextInitializer}. For example:
	 *
	 * <pre class="code">
	 * ConfigurableEnvironment env = new StandardEnvironment();
	 * env.getPropertySources().addLast(new PropertySourceX(...));
	 * </pre>
	 *
	 * <h2>A warning about instance variable access</h2>
	 * <p>Instance variables declared in subclasses and having default initial values should
	 * <em>not</em> be accessed from within this method. Due to Java object creation
	 * lifecycle constraints, any initial value will not yet be assigned when this
	 * callback is invoked by the {@link #AbstractEnvironment()} constructor, which may
	 * lead to a {@code NullPointerException} or other problems. If you need to access
	 * default values of instance variables, leave this method as a no-op and perform
	 * property source manipulation and instance variable access directly within the
	 * subclass constructor. Note that <em>assigning</em> values to instance variables is
	 * not problematic; it is only attempting to read default values that must be avoided.
	 * @see MutablePropertySources
	 * @see PropertySourcesPropertyResolver
	 * @see org.springframework.context.ApplicationContextInitializer
	 */
	// 自定义在调用getProperty(String)和相关方法期间要由此Environment搜索的PropertySource对象集。
	// 鼓励覆盖此方法的子类使用MutablePropertySources.addLast(PropertySource)添加属性源，以便进一步的子类可以
	// 调用super.customizePropertySources()并获得可预测的结果。 例如：
	//	   public class Level1Environment extends AbstractEnvironment {
	//	       @Override
	//	       protected void customizePropertySources(MutablePropertySources propertySources) {
	//	           super.customizePropertySources(propertySources); // no-op from base class
	//	           propertySources.addLast(new PropertySourceA(...));
	//	           propertySources.addLast(new PropertySourceB(...));
	//	       }
	//	   }
	//
	//	   public class Level2Environment extends Level1Environment {
	//	       @Override
	//	       protected void customizePropertySources(MutablePropertySources propertySources) {
	//	           super.customizePropertySources(propertySources); // add all from superclass
	//	           propertySources.addLast(new PropertySourceC(...));
	//	           propertySources.addLast(new PropertySourceD(...));
	//	       }
	//	   }
	//
	// 在这种安排下，属性将按照源 A、B、C、D 的顺序进行解析。 也就是说，属性源“A”优先于属性源“D”。 如果Level2Environment子类
	// 希望给属性源 C 和 D 比 A 和 B 更高的优先级，它可以简单地调用super.customizePropertySources之后，而不是在添加自己的之前：
	//	   public class Level2Environment extends Level1Environment {
	//	       @Override
	//	       protected void customizePropertySources(MutablePropertySources propertySources) {
	//	           propertySources.addLast(new PropertySourceC(...));
	//	           propertySources.addLast(new PropertySourceD(...));
	//	           super.customizePropertySources(propertySources); // add all from superclass
	//	       }
	//	   }
	//
	// 搜索顺序现在是 C、D、A、B。
	// 除了这些建议之外，子类可以使用任何由MutablePropertySources公开的add* 、 remove或replace方法来创建所需的属性源的精确排列。
	// 基本实现不注册任何属性源。
	// 请注意，任何ConfigurableEnvironment客户端都可以通过getPropertySources()访问器进一步自定义属性源，通常
	// 在ApplicationContextInitializer 。 例如：
	//	   ConfigurableEnvironment env = new StandardEnvironment();
	//	   env.getPropertySources().addLast(new PropertySourceX(...));
	//
	// 关于实例变量访问的警告
	// 在子类中具有默认的初始值声明实例变量不应该从这个方法中访问。 由于Java对象创建生命周期的限制，
	// AbstractEnvironment()构造函数调用该回调函数时，尚未赋值任何初始值，可能导致NullPointerException等问题。
	// 如果您需要访问实例变量的默认值，请将此方法保留为无操作，并直接在子类构造函数中执行属性源操作和实例变量访问。
	// 需要注意的是，以实例变量赋值，是没有问题的; 它只是试图读取必须避免的默认值。
	protected void customizePropertySources(MutablePropertySources propertySources) {
	}

	/**
	 * Return the set of reserved default profile names. This implementation returns
	 * {@value #RESERVED_DEFAULT_PROFILE_NAME}. Subclasses may override in order to
	 * customize the set of reserved names.
	 * @see #RESERVED_DEFAULT_PROFILE_NAME
	 * @see #doGetDefaultProfiles()
	 */
	// 返回一组保留的默认配置文件名称。此实现返回{@value #RESERVED_DEFAULT_PROFILE_NAME}。子类可以重写以自定义保留名称集
	protected Set<String> getReservedDefaultProfiles() {
		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	// ConfigurableEnvironment 接口的实现
	//---------------------------------------------------------------------

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}

	/**
	 * Return the set of active profiles as explicitly set through
	 * {@link #setActiveProfiles} or if the current set of active profiles
	 * is empty, check for the presence of {@link #doGetActiveProfilesProperty()}
	 * and assign its value to the set of active profiles.
	 * @see #getActiveProfiles()
	 * @see #doGetActiveProfilesProperty()
	 */
	// 返回通过 {@link #setActiveProfiles}显式设置的活动配置文件集，或者如果当前活动配置文件集为空，
	// 则检查 {@link #doGetActiveProfilesProperty()} 是否存在并将其值分配给活动配置文件集
	protected Set<String> doGetActiveProfiles() {
		synchronized (this.activeProfiles) {
			if (this.activeProfiles.isEmpty()) {
				String profiles = doGetActiveProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.activeProfiles;
		}
	}

	/**
	 * Return the property value for the active profiles.
	 * @since 5.3.4
	 * @see #ACTIVE_PROFILES_PROPERTY_NAME
	 */
	// 返回活动配置文件的属性值
	@Nullable
	protected String doGetActiveProfilesProperty() {
		return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Override
	public void setActiveProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profiles " + Arrays.asList(profiles));
		}
		synchronized (this.activeProfiles) {
			this.activeProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.activeProfiles.add(profile);
			}
		}
	}

	@Override
	public void addActiveProfile(String profile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profile '" + profile + "'");
		}
		validateProfile(profile);
		doGetActiveProfiles();
		synchronized (this.activeProfiles) {
			this.activeProfiles.add(profile);
		}
	}


	@Override
	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	/**
	 * Return the set of default profiles explicitly set via
	 * {@link #setDefaultProfiles(String...)} or if the current set of default profiles
	 * consists only of {@linkplain #getReservedDefaultProfiles() reserved default
	 * profiles}, then check for the presence of {@link #doGetActiveProfilesProperty()}
	 * and assign its value (if any) to the set of default profiles.
	 * @see #AbstractEnvironment()
	 * @see #getDefaultProfiles()
	 * @see #getReservedDefaultProfiles()
	 * @see #doGetDefaultProfilesProperty()
	 */
	// 返回通过 {@link #setDefaultProfiles(String...)} 显式设置的默认配置文件集，或者如果当前默认配置文件集仅包含保留的默认配置文件，
	// {@linkplain #getReservedDefaultProfiles() reserved default profiles}
	// 则检查 {@link #doGetActiveProfilesProperty()} 的存在并将其值（如果有）分配给一组默认配置文件
	protected Set<String> doGetDefaultProfiles() {
		synchronized (this.defaultProfiles) {
			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
				String profiles = doGetDefaultProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.defaultProfiles;
		}
	}

	/**
	 * Return the property value for the default profiles.
	 * @since 5.3.4
	 * @see #DEFAULT_PROFILES_PROPERTY_NAME
	 */
	// 返回默认配置文件的属性值
	@Nullable
	protected String doGetDefaultProfilesProperty() {
		return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
	}

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * <p>Calling this method removes overrides any reserved default profiles
	 * that may have been added during construction of the environment.
	 * @see #AbstractEnvironment()
	 * @see #getReservedDefaultProfiles()
	 */
	// 如果没有其他配置文件通过 {@link #setActiveProfiles} 显式激活，则指定默认激活的配置文件集。
	// <p>调用此方法会删除覆盖在构建环境期间可能已添加的任何保留的默认配置文件。
	@Override
	public void setDefaultProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		synchronized (this.defaultProfiles) {
			this.defaultProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.defaultProfiles.add(profile);
			}
		}
	}

	@Override
	@Deprecated
	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		for (String profile : profiles) {
			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
				if (!isProfileActive(profile.substring(1))) {
					return true;
				}
			}
			else if (isProfileActive(profile)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean acceptsProfiles(Profiles profiles) {
		Assert.notNull(profiles, "Profiles must not be null");
		return profiles.matches(this::isProfileActive);
	}

	/**
	 * Return whether the given profile is active, or if active profiles are empty
	 * whether the profile should be active by default.
	 * @throws IllegalArgumentException per {@link #validateProfile(String)}
	 */
	// 返回给定的配置文件是否处于活动状态，或者如果活动配置文件为空，则该配置文件是否应该在默认情况下处于活动状态
	protected boolean isProfileActive(String profile) {
		validateProfile(profile);
		Set<String> currentActiveProfiles = doGetActiveProfiles();
		return (currentActiveProfiles.contains(profile) ||
				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
	}

	/**
	 * Validate the given profile, called internally prior to adding to the set of
	 * active or default profiles.
	 * <p>Subclasses may override to impose further restrictions on profile syntax.
	 * @throws IllegalArgumentException if the profile is null, empty, whitespace-only or
	 * begins with the profile NOT operator (!).
	 * @see #acceptsProfiles
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 */
	// 验证给定的配置文件，在添加到活动或默认配置文件集之前在内部调用
	// <p>子类可以重写以对配置文件语法施加进一步的限制
	// @throws IllegalArgumentException 如果配置文件为 null、empty、whitespace-only或 以配置文件 NOT 运算符 (!) 开头。
	protected void validateProfile(String profile) {
		if (!StringUtils.hasText(profile)) {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
		}
		if (profile.charAt(0) == '!') {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
		}
	}

	@Override
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemProperties() {
		try {
			return (Map) System.getProperties();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getProperty(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system property '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemEnvironment() {
		if (suppressGetenvAccess()) {
			return Collections.emptyMap();
		}
		try {
			return (Map) System.getenv();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getenv(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system environment variable '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	/**
	 * Determine whether to suppress {@link System#getenv()}/{@link System#getenv(String)}
	 * access for the purposes of {@link #getSystemEnvironment()}.
	 * <p>If this method returns {@code true}, an empty dummy Map will be used instead
	 * of the regular system environment Map, never even trying to call {@code getenv}
	 * and therefore avoiding security manager warnings (if any).
	 * <p>The default implementation checks for the "spring.getenv.ignore" system property,
	 * returning {@code true} if its value equals "true" in any case.
	 * @see #IGNORE_GETENV_PROPERTY_NAME
	 * @see SpringProperties#getFlag
	 */
	// 确定是否出于 {@link #getSystemEnvironment()} 的目的禁止
	// {@link System#getenv()}/{@link System#getenv(String)} 访问。
	//
	// <p>如果这个方法返回 {@code true}，一个空的虚拟 Map 将被用来代替常规的系统环境 Map，
	// 甚至从不尝试调用 {@code getenv} 从而避免安全管理器警告（如果有的话）
	//
	// <p>默认实现检查“spring.getenv.ignore”系统属性，如果它的值在任何情况下都等于 "true"，则返回{@code true}。
	protected boolean suppressGetenvAccess() {
		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
	}

	@Override
	public void merge(ConfigurableEnvironment parent) {
		for (PropertySource<?> ps : parent.getPropertySources()) {
			if (!this.propertySources.contains(ps.getName())) {
				this.propertySources.addLast(ps);
			}
		}
		String[] parentActiveProfiles = parent.getActiveProfiles();
		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
			synchronized (this.activeProfiles) {
				Collections.addAll(this.activeProfiles, parentActiveProfiles);
			}
		}
		String[] parentDefaultProfiles = parent.getDefaultProfiles();
		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
			synchronized (this.defaultProfiles) {
				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
				Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	// ConfigurablePropertyResolver 接口的实现
	//---------------------------------------------------------------------

	@Override
	public ConfigurableConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}

	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}

	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		this.propertyResolver.setRequiredProperties(requiredProperties);
	}

	@Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyResolver interface
	// PropertyResolver 接口的实现
	//---------------------------------------------------------------------

	@Override
	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		// 委托给 ConfigurablePropertyResolver
		return this.propertyResolver.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.propertyResolver.getProperty(key, defaultValue);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return this.propertyResolver.getProperty(key, targetType, defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		// 解析 ${...} 占位符
		return this.propertyResolver.resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
	}

}
