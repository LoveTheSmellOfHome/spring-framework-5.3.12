/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Specialization of {@link MapPropertySource} designed for use with
 * {@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}.
 * Compensates for constraints in Bash and other shells that do not allow for variables
 * containing the period character and/or hyphen character; also allows for uppercase
 * variations on property names for more idiomatic shell use.
 *
 * <p>For example, a call to {@code getProperty("foo.bar")} will attempt to find a value
 * for the original property or any 'equivalent' property, returning the first found:
 * <ul>
 * <li>{@code foo.bar} - the original name</li>
 * <li>{@code foo_bar} - with underscores for periods (if any)</li>
 * <li>{@code FOO.BAR} - original, with upper case</li>
 * <li>{@code FOO_BAR} - with underscores and upper case</li>
 * </ul>
 * Any hyphen variant of the above would work as well, or even mix dot/hyphen variants.
 *
 * <p>The same applies for calls to {@link #containsProperty(String)}, which returns
 * {@code true} if any of the above properties are present, otherwise {@code false}.
 *
 * <p>This feature is particularly useful when specifying active or default profiles as
 * environment variables. The following is not allowable under Bash:
 *
 * <pre class="code">spring.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * However, the following syntax is permitted and is also more conventional:
 *
 * <pre class="code">SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>Enable debug- or trace-level logging for this class (or package) for messages
 * explaining when these 'property name resolutions' occur.
 *
 * <p>This property source is included by default in {@link StandardEnvironment}
 * and all its subclasses.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
// 专为与 系统环境变量{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
// 一起使用而设计的 {@link MapPropertySource} 。 补偿 Bash 和其他 shell 中不允许包含句点字符和/或连字符的变量的约束；
// 还允许对属性名称进行大写变体，以便更惯用的 shell 使用。
// 例如，调用 {@code getProperty("foo.bar")} 将尝试查找原始属性或任何“等效”属性的值，返回第一个找到的值：
// 。{@code foo.bar} - 原始名称
// 。{@code foo_bar} - 下划线表示句点（如果有的话）
// 。{@code FOO.BAR} - 原始，大写
// 。{@code FOO_BAR} - 带下划线和大写
// 上述任何连字符变体也可以使用，甚至 dot/hyphen 混合点/连字符 变体。
// 这同样适用于对 {@link #containsProperty(String)} 调用，如果存在上述任何属性，则返回true ，否则返回false 。
// 在将活动或默认配置文件指定为环境变量时，此功能特别有用。 在 Bash 下以下是不允许的：
// spring.profiles.active=p1 java -classpath ... MyApp
// 但是，允许使用以下语法，并且也更传统：
// SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp
// 为此类（或包）启用调试或跟踪级日志记录，以获取解释这些“属性名称解析”何时发生的消息。
// 默认情况下，此属性源包含在StandardEnvironment及其所有子类中。
//
// Spring 內建的配置属性源 - 环境变量配置属性源
public class SystemEnvironmentPropertySource extends MapPropertySource {

	/**
	 * Create a new {@code SystemEnvironmentPropertySource} with the given name and
	 * delegating to the given {@code MapPropertySource}.
	 */
	// 使用给定的名称创建一个新的 {@code SystemEnvironmentPropertySource} 并
	// 委托给给定的 {@code MapPropertySource}
	public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	/**
	 * Return {@code true} if a property with the given name or any underscore/uppercase variant
	 * thereof exists in this property source.
	 */
	// 如果此属性源中存在具有给定名称的属性或其任何 下划线/大写 变体，则返回 {@code true}
	@Override
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * This implementation returns {@code true} if a property with the given name or
	 * any underscore/uppercase variant thereof exists in this property source.
	 */
	// 如果此属性源中存在具有给定名称的属性或其任何 下划线/大写 变体，则此实现将返回 {@code true}。
	@Override
	@Nullable
	public Object getProperty(String name) {
		String actualName = resolvePropertyName(name);
		if (logger.isDebugEnabled() && !name.equals(actualName)) {
			logger.debug("PropertySource '" + getName() + "' does not contain property '" + name +
					"', but found equivalent '" + actualName + "'");
		}
		return super.getProperty(actualName);
	}

	/**
	 * Check to see if this property source contains a property with the given name, or
	 * any underscore / uppercase variation thereof. Return the resolved name if one is
	 * found or otherwise the original name. Never returns {@code null}.
	 */
	// 检查此属性源是否包含具有给定名称的属性或其任何 下划线/大写 变体。如果找到，则返回解析的名称，
	// 否则返回原始名称。从不返回 {@code null}。
	protected final String resolvePropertyName(String name) {
		Assert.notNull(name, "Property name must not be null");
		String resolvedName = checkPropertyName(name);
		if (resolvedName != null) {
			return resolvedName;
		}
		String uppercasedName = name.toUpperCase();
		if (!name.equals(uppercasedName)) {
			resolvedName = checkPropertyName(uppercasedName);
			if (resolvedName != null) {
				return resolvedName;
			}
		}
		return name;
	}

	@Nullable
	private String checkPropertyName(String name) {
		// Check name as-is
		// 按原样检查名称
		if (containsKey(name)) {
			return name;
		}
		// Check name with just dots replaced
		// 检查名称仅替换点
		String noDotName = name.replace('.', '_');
		if (!name.equals(noDotName) && containsKey(noDotName)) {
			return noDotName;
		}
		// Check name with just hyphens replaced
		// 检查名称，仅替换连字符
		String noHyphenName = name.replace('-', '_');
		if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
			return noHyphenName;
		}
		// Check name with dots and hyphens replaced
		// 用点和连字符替换检查名称
		String noDotNoHyphenName = noDotName.replace('-', '_');
		if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
			return noDotNoHyphenName;
		}
		// Give up
		return null;
	}

	private boolean containsKey(String name) {
		return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
	}

	protected boolean isSecurityManagerPresent() {
		return (System.getSecurityManager() != null);
	}

}
