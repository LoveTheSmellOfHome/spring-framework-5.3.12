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

package org.springframework.beans.factory.config;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

/**
 * {@link PlaceholderConfigurerSupport} subclass that resolves ${...} placeholders against
 * {@link #setLocation local} {@link #setProperties properties} and/or system properties
 * and environment variables.
 *
 * <p>{@link PropertyPlaceholderConfigurer} is still appropriate for use when:
 * <ul>
 * <li>the {@code spring-context} module is not available (i.e., one is using Spring's
 * {@code BeanFactory} API as opposed to {@code ApplicationContext}).
 * <li>existing configuration makes use of the {@link #setSystemPropertiesMode(int) "systemPropertiesMode"}
 * and/or {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"} properties.
 * Users are encouraged to move away from using these settings, and rather configure property
 * source search order through the container's {@code Environment}; however, exact preservation
 * of functionality may be maintained by continuing to use {@code PropertyPlaceholderConfigurer}.
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 02.10.2003
 * @see #setSystemPropertiesModeName
 * @see PlaceholderConfigurerSupport
 * @see PropertyOverrideConfigurer
 * @deprecated as of 5.2; use {@code org.springframework.context.support.PropertySourcesPlaceholderConfigurer}
 * instead which is more flexible through taking advantage of the {@link org.springframework.core.env.Environment}
 * and {@link org.springframework.core.env.PropertySource} mechanisms.
 */
// @link PlaceholderConfigurerSupport} 子类，根据 {@link #setLocation local} {@link #setProperties properties}
// 和/或系统属性和环境变量解析 ${...} 占位符。
//
// <p>{@link PropertyPlaceholderConfigurer} 仍然适用于以下情况：
// <ul>
// <li>{@code spring-context} 模块不可用（即，使用 Spring 的 {@code BeanFactory} API 而不是 { @code ApplicationContext}）。
// <li>现有配置使用 {@link #setSystemPropertiesMode(int) "systemPropertiesMode"} 和或
// {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"} 属性。鼓励用户不要使用这些设置，
// 而是通过容器的 {@code Environment} 配置属性源搜索顺序；但是，可以通过继续使用 {@code PropertyPlaceholderConfigurer}
// 来精确保留功能。 <ul>
//
// Spring 3.1 前占位符处理组件，通过关联一个 Properties 资源文件作为数据源进行占位符替换。Spring 5 之后使用
// {@link PropertySourcesPlaceholderConfigurer} 作为增强类
// 接口是 {@link org.springframework.util.StringValueResolver}
@Deprecated
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

	/** Never check system properties. */
	// 从不检查系统属性
	public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

	/**
	 * Check system properties if not resolvable in the specified properties.
	 * This is the default.
	 */
	// 如果在指定的属性中无法解析，请检查系统属性。这是默认设置
	public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

	/**
	 * Check system properties first, before trying the specified properties.
	 * This allows system properties to override any other property source.
	 */
	// 在尝试指定的属性之前，首先检查系统属性。这允许系统属性覆盖任何其他属性源。
	public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;


	private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

	private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

	private boolean searchSystemEnvironment =
			!SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);


	/**
	 * Set the system property mode by the name of the corresponding constant,
	 * e.g. "SYSTEM_PROPERTIES_MODE_OVERRIDE".
	 * @param constantName name of the constant
	 * @see #setSystemPropertiesMode
	 */
	// 通过相应常量的名称设置系统属性模式，例如“SYSTEM_PROPERTIES_MODE_OVERRIDE”。
	// @param constantName 常量名
	public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
		this.systemPropertiesMode = constants.asNumber(constantName).intValue();
	}

	/**
	 * Set how to check system properties: as fallback, as override, or never.
	 * For example, will resolve ${user.dir} to the "user.dir" system property.
	 * <p>The default is "fallback": If not being able to resolve a placeholder
	 * with the specified properties, a system property will be tried.
	 * "override" will check for a system property first, before trying the
	 * specified properties. "never" will not check system properties at all.
	 * @see #SYSTEM_PROPERTIES_MODE_NEVER
	 * @see #SYSTEM_PROPERTIES_MODE_FALLBACK
	 * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
	 * @see #setSystemPropertiesModeName
	 */
	// 设置如何检查系统属性：作为回退、作为覆盖或从不。例如，将 {user.dir} 解析为“user.dir”系统属性。
	// <p>默认为“回退”：如果无法解析具有指定属性的占位符，将尝试使用系统属性。
	// “覆盖”将首先检查系统属性，然后再尝试指定的属性。 “从不”根本不会检查系统属性。
	public void setSystemPropertiesMode(int systemPropertiesMode) {
		this.systemPropertiesMode = systemPropertiesMode;
	}

	/**
	 * Set whether to search for a matching system environment variable
	 * if no matching system property has been found. Only applied when
	 * "systemPropertyMode" is active (i.e. "fallback" or "override"), right
	 * after checking JVM system properties.
	 * <p>Default is "true". Switch this setting off to never resolve placeholders
	 * against system environment variables. Note that it is generally recommended
	 * to pass external values in as JVM system properties: This can easily be
	 * achieved in a startup script, even for existing environment variables.
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	// 设置如果没有找到匹配的系统属性，是否搜索匹配的系统环境变量。
	// 仅在“systemPropertyMode”处于活动状态（即“回退”或“覆盖”）时，在检查 JVM 系统属性后立即应用。
	// <p>默认为“真”。关闭此设置以从不针对系统环境变量解析占位符。请注意，通常建议将外部值作为 JVM 系统属性传入：
	// 这可以在启动脚本中轻松实现，即使对于现有环境变量也是如此
	public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
		this.searchSystemEnvironment = searchSystemEnvironment;
	}

	/**
	 * Resolve the given placeholder using the given properties, performing
	 * a system properties check according to the given mode.
	 * <p>The default implementation delegates to {@code resolvePlaceholder
	 * (placeholder, props)} before/after the system properties check.
	 * <p>Subclasses can override this for custom resolution strategies,
	 * including customized points for the system properties check.
	 * @param placeholder the placeholder to resolve
	 * @param props the merged properties of this configurer
	 * @param systemPropertiesMode the system properties mode,
	 * according to the constants in this class
	 * @return the resolved value, of null if none
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty
	 * @see #resolvePlaceholder(String, java.util.Properties)
	 */
	// 使用给定的属性解析占位符，根据给定的模式执行系统属性检查
	// <p>在系统属性检查之前，默认实现委托给 {@code resolvePlaceholder (placeholder, props)}。
	// <p>子类可以为自定义解析策略覆盖此设置，包括用于系统属性检查的自定义点
	// @param placeholder 要解析的占位符
	// @param props 此配置器的合并属性
	// @param systemPropertiesMode 系统属性模式，根据这个类中的常量
	// @return 解析值，如果没有则为 null1122
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
		String propVal = null;
		if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
			propVal = resolveSystemProperty(placeholder);
		}
		if (propVal == null) {
			propVal = resolvePlaceholder(placeholder, props);
		}
		if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
			propVal = resolveSystemProperty(placeholder);
		}
		return propVal;
	}

	/**
	 * Resolve the given placeholder using the given properties.
	 * The default implementation simply checks for a corresponding property key.
	 * <p>Subclasses can override this for customized placeholder-to-key mappings
	 * or custom resolution strategies, possibly just using the given properties
	 * as fallback.
	 * <p>Note that system properties will still be checked before respectively
	 * after this method is invoked, according to the system properties mode.
	 * @param placeholder the placeholder to resolve
	 * @param props the merged properties of this configurer
	 * @return the resolved value, of {@code null} if none
	 * @see #setSystemPropertiesMode
	 */
	// 使用给定的属性解析给定的占位符。默认实现只是检查相应的属性键
	// <p>子类可以为自定义占位符到键映射或自定义解析策略覆盖它，可能只是使用给定的属性作为后备。
	// <p>注意，在调用该方法之前，系统属性仍会根据系统属性模式分别进行检查
	// @param placeholder 要解析的占位符
	// @param props 此配置器的合并属性
	// @return 解析值，{@code null} 如果没有
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props) {
		return props.getProperty(placeholder);
	}

	/**
	 * Resolve the given key as JVM system property, and optionally also as
	 * system environment variable if no matching system property has been found.
	 * @param key the placeholder to resolve as system property key
	 * @return the system property value, or {@code null} if not found
	 * @see #setSearchSystemEnvironment
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	// 将给定的 key 解析为 JVM 系统属性，如果没有找到匹配的系统属性，还可以选择将其解析为系统环境变量。
	// @param key 要解析为系统属性键的占位符
	// @return 系统属性值，如果未找到，则为 {@code null}
	@Nullable
	protected String resolveSystemProperty(String key) {
		try {
			String value = System.getProperty(key);
			if (value == null && this.searchSystemEnvironment) {
				value = System.getenv(key);
			}
			return value;
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access system property '" + key + "': " + ex);
			}
			return null;
		}
	}


	/**
	 * Visit each bean definition in the given bean factory and attempt to replace ${...} property
	 * placeholders with values from the given properties.
	 */
	// 访问给定 bean 工厂中的每个 bean 定义，并尝试用给定属性中的值替换 ${...} 属性占位符
	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {

		StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
		doProcessProperties(beanFactoryToProcess, valueResolver);
	}


	private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public PlaceholderResolvingStringValueResolver(Properties props) {
			this.helper = new PropertyPlaceholderHelper(
					placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
			this.resolver = new PropertyPlaceholderConfigurerResolver(props);
		}

		@Override
		@Nullable
		public String resolveStringValue(String strVal) throws BeansException {
			String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
			if (trimValues) {
				resolved = resolved.trim();
			}
			return (resolved.equals(nullValue) ? null : resolved);
		}
	}


	private final class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

		private final Properties props;

		private PropertyPlaceholderConfigurerResolver(Properties props) {
			this.props = props;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName,
					this.props, systemPropertiesMode);
		}
	}

}
