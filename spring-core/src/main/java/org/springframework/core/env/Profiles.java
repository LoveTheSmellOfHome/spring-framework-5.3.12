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

package org.springframework.core.env;

import java.util.function.Predicate;

/**
 * Profile predicate that may be {@linkplain Environment#acceptsProfiles(Profiles)
 * accepted} by an {@link Environment}.
 *
 * <p>May be implemented directly or, more usually, created using the
 * {@link #of(String...) of(...)} factory method.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
// {@link Environment} 可能会接受的 {@linkplain Environment#acceptsProfiles(Profiles) }的profile 推断
// <p>可以直接实现，或者更常见的是，使用 {@link of(String...) of(...)} 工厂方法创建。
@FunctionalInterface
public interface Profiles {

	/**
	 * Test if this {@code Profiles} instance <em>matches</em> against the given
	 * active profiles predicate.
	 * @param activeProfiles a predicate that tests whether a given profile is
	 * currently active
	 */
	// 测试此 {@code Profiles} 实例是否与给定的活动配置文件推断<em>匹配<em>。
	boolean matches(Predicate<String> activeProfiles);


	/**
	 * Create a new {@link Profiles} instance that checks for matches against
	 * the given <em>profile strings</em>.
	 * <p>The returned instance will {@linkplain Profiles#matches(Predicate) match}
	 * if any one of the given profile strings matches.
	 * <p>A profile string may contain a simple profile name (for example
	 * {@code "production"}) or a profile expression. A profile expression allows
	 * for more complicated profile logic to be expressed, for example
	 * {@code "production & cloud"}.
	 * <p>The following operators are supported in profile expressions.
	 * <ul>
	 * <li>{@code !} - A logical <em>NOT</em> of the profile or profile expression</li>
	 * <li>{@code &} - A logical <em>AND</em> of the profiles or profile expressions</li>
	 * <li>{@code |} - A logical <em>OR</em> of the profiles or profile expressions</li>
	 * </ul>
	 * <p>Please note that the {@code &} and {@code |} operators may not be mixed
	 * without using parentheses. For example {@code "a & b | c"} is not a valid
	 * expression; it must be expressed as {@code "(a & b) | c"} or
	 * {@code "a & (b | c)"}.
	 * <p>As of Spring Framework 5.1.17, two {@code Profiles} instances returned
	 * by this method are considered equivalent to each other (in terms of
	 * {@code equals()} and {@code hashCode()} semantics) if they are created
	 * with identical <em>profile strings</em>.
	 * @param profiles the <em>profile strings</em> to include
	 * @return a new {@link Profiles} instance
	 */
	// 创建一个新的 Profiles 实例，用于根据给定的配置文件字符串检查匹配项。
	// 如果给定的配置文件字符串中的任何一个匹配，则返回的实例将匹配。
	// 配置文件字符串可能包含简单的配置文件名称（例如"production"）或配置文件表达式。
	// 配置文件表达式允许表达更复杂的配置文件逻辑，例如"production & cloud".。
	// 配置文件表达式支持以下运算符：
	// ! - 配置文件或配置文件表达式的逻辑非
	// & - 配置文件或配置文件表达式的逻辑与
	// | - 配置文件或配置文件表达式的逻辑 OR
	// 请注意 & 和 |不使用括号不能混合运算符。例如“a & b | c”不是一个有效的表达式；它必须表示为“(a & b) | c”或“a & (b | c)”。
	// 从 Spring Framework 5.1.17 开始，如果此方法返回的两个 Profiles 实例是使用相同的配置文件字符串创建的，
	// 则它们被视为彼此等效（就 equals() 和 hashCode() 语义而言）。
	static Profiles of(String... profiles) {
		return ProfilesParser.parse(profiles);
	}

}
