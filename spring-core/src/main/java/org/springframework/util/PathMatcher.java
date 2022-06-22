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

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Strategy interface for {@code String}-based path matching.
 *
 * <p>Used by {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping},
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 *
 * <p>The default implementation is {@link AntPathMatcher}, supporting the
 * Ant-style pattern syntax.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
// 基于String的路径匹配的策略接口。
// 由org.springframework.core.io.support.PathMatchingResourcePatternResolver 、
// org.springframework.web.servlet.handler.AbstractUrlHandlerMapping和
// org.springframework.web.servlet.mvc.WebContentInterceptor
// 默认实现是AntPathMatcher ，支持 Ant 风格的模式语法
//
// 路径匹配器
public interface PathMatcher {

	/**
	 * Does the given {@code path} represent a pattern that can be matched
	 * by an implementation of this interface?
	 * <p>If the return value is {@code false}, then the {@link #match}
	 * method does not have to be used because direct equality comparisons
	 * on the static path Strings will lead to the same result.
	 * @param path the path to check
	 * @return {@code true} if the given {@code path} represents a pattern
	 */
	// 给定的path是否表示可以与此接口的实现匹配的模式？
	// 如果返回值为false ，则不必使用match方法，因为在静态路径 Strings 上的直接相等比较将导致相同的结果。
	//形参：path – 要检查的路径
	//返回值：如果给定的path代表一个模式，则为true
	boolean isPattern(String path);

	/**
	 * Match the given {@code path} against the given {@code pattern},
	 * according to this PathMatcher's matching strategy.
	 * @param pattern the pattern to match against
	 * @param path the path to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	// 根据此 PathMatcher 的匹配策略，将给定path与给定pattern匹配。
	// 形参：模式 - 要匹配的模式
	// 路径 - 测试路径
	// 返回值：如果提供的path匹配，则为true否则为false
	boolean match(String pattern, String path);

	/**
	 * Match the given {@code path} against the corresponding part of the given
	 * {@code pattern}, according to this PathMatcher's matching strategy.
	 * <p>Determines whether the pattern at least matches as far as the given base
	 * path goes, assuming that a full path may then match as well.
	 * @param pattern the pattern to match against
	 * @param path the path to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	// 根据此 PathMatcher 的匹配策略，将给定path与给定pattern的相应部分匹配。
	// 确定模式是否至少匹配给定的基本路径，假设完整路径也可能匹配。
	// @param pattern - 要匹配的模式
	// @param path - 测试路径
	//返回值：如果提供的path匹配，则为true否则为false
	boolean matchStart(String pattern, String path);

	/**
	 * Given a pattern and a full path, determine the pattern-mapped part.
	 * <p>This method is supposed to find out which part of the path is matched
	 * dynamically through an actual pattern, that is, it strips off a statically
	 * defined leading path from the given full path, returning only the actually
	 * pattern-matched part of the path.
	 * <p>For example: For "myroot/*.html" as pattern and "myroot/myfile.html"
	 * as full path, this method should return "myfile.html". The detailed
	 * determination rules are specified to this PathMatcher's matching strategy.
	 * <p>A simple implementation may return the given full path as-is in case
	 * of an actual pattern, and the empty String in case of the pattern not
	 * containing any dynamic parts (i.e. the {@code pattern} parameter being
	 * a static path that wouldn't qualify as an actual {@link #isPattern pattern}).
	 * A sophisticated implementation will differentiate between the static parts
	 * and the dynamic parts of the given path pattern.
	 * @param pattern the path pattern
	 * @param path the full path to introspect
	 * @return the pattern-mapped part of the given {@code path}
	 * (never {@code null})
	 */
	// 给定模式和完整路径，确定模式映射部分。
	// 该方法应该通过实际模式找出路径的哪一部分动态匹配，也就是说，它从给定的完整路径中剥离静态定义的
	// 前导路径，仅返回路径中实际模式匹配的部分
	// 例如：对于“myroot/*.html”作为模式和“myroot/myfile.html”作为完整路径，这个方法应该返回“myfile.html”。
	// 该PathMatcher的匹配策略指定了详细的判断规则。
	// 一个简单的实现可以在实际模式的情况下按原样返回给定的完整路径，这个方法应该返回“myfile.html”。
	// 该PathMatcher的匹配策略指定了详细的判断规则。
	// 一个简单的实现可以在实际模式的情况下按原样返回给定的完整路径，在模式不包含任何动态部分的情况下
	// 返回空字符串（即pattern参数是一个静态路径，不符合实际pattern ）。 复杂的实现将区分给定路径模式的静态部分和动态部分
	// @param pattern - 路径模式
	// @param pattern - 完整的路径
	// @return 给定path的模式映射部分（从不为null ）
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * Given a pattern and a full path, extract the URI template variables. URI template
	 * variables are expressed through curly brackets ('{' and '}').
	 * <p>For example: For pattern "/hotels/{hotel}" and path "/hotels/1", this method will
	 * return a map containing "hotel" &rarr; "1".
	 * @param pattern the path pattern, possibly containing URI templates
	 * @param path the full path to extract template variables from
	 * @return a map, containing variable names as keys; variables values as values
	 */
	// 给定模式和完整路径，提取 URI 模板变量。 URI 模板变量通过大括号（“{”和“}”）表示。
	// 例如：对于 pattern “/hotels/{hotel}”和路径“/hotels/1”，此方法将返回一个包含“hotel”→“1”的Map。
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * Given a full path, returns a {@link Comparator} suitable for sorting patterns
	 * in order of explicitness for that path.
	 * <p>The full algorithm used depends on the underlying implementation,
	 * but generally, the returned {@code Comparator} will
	 * {@linkplain java.util.List#sort(java.util.Comparator) sort}
	 * a list so that more specific patterns come before generic patterns.
	 * @param path the full path to use for comparison
	 * @return a comparator capable of sorting patterns in order of explicitness
	 */
	// 给定完整路径，返回一个 Comparator 适合按该路径的显式顺序对模式进行排序
	// 使用的完整算法取决于底层实现，但通常，返回的 Comparator 将对列表进行排序，以便更具体的模式出现在通用模式之前。
	// @param pattern - 用于比较的完整路径
	// @return 能够按明确性顺序对模式进行排序的比较器
	Comparator<String> getPatternComparator(String path);

	/**
	 * Combines two patterns into a new pattern that is returned.
	 * <p>The full algorithm used for combining the two pattern depends on the underlying implementation.
	 * @param pattern1 the first pattern
	 * @param pattern2 the second pattern
	 * @return the combination of the two patterns
	 * @throws IllegalArgumentException when the two patterns cannot be combined
	 */
	// 将两个模式组合成一个返回的新模式。
	// <p>用于组合这两种模式的完整算法取决于底层实现。
	String combine(String pattern1, String pattern2);

}
