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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Annotation for mapping web requests onto methods in request-handling classes
 * with flexible method signatures.
 *
 * <p>Both Spring MVC and Spring WebFlux support this annotation through a
 * {@code RequestMappingHandlerMapping} and {@code RequestMappingHandlerAdapter}
 * in their respective modules and package structure. For the exact list of
 * supported handler method arguments and return types in each, please use the
 * reference documentation links below:
 * <ul>
 * <li>Spring MVC
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-return-types">Return Values</a>
 * </li>
 * <li>Spring WebFlux
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-return-types">Return Values</a>
 * </li>
 * </ul>
 *
 * <p><strong>Note:</strong> This annotation can be used both at the class and
 * at the method level. In most cases, at the method level applications will
 * prefer to use one of the HTTP method specific variants
 * {@link GetMapping @GetMapping}, {@link PostMapping @PostMapping},
 * {@link PutMapping @PutMapping}, {@link DeleteMapping @DeleteMapping}, or
 * {@link PatchMapping @PatchMapping}.</p>
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @RequestMapping} and {@code @SessionAttributes} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PatchMapping
 */
// 用于将 Web 请求映射到具有灵活方法签名的请求处理类中的方法的注释。
//
// Spring MVC 和 Spring WebFlux 都通过各自模块和包结构中的 RequestMappingHandlerMapping
// 和 RequestMappingHandlerAdapter 支持此注解。有关每个支持的处理程序方法参数和返回类型的确切列表，请使用下面的参考文档链接：
// - pring MVC 方法参数和返回值
// - Spring WebFlux 方法参数和返回值
//
// 注意：这个注解可以在类和方法级别使用。在大多数情况下，在方法级别应用程序会更喜欢使用 HTTP 方法特定的变体
// @GetMapping 、 @PostMapping 、 @PutMapping 、 @DeleteMapping 或 @PatchMapping 。
//
// 注意：当使用控制器接口（例如用于 AOP 代理）时，请确保始终将所有映射注释 - 例如 @RequestMapping
// 和 @SessionAttributes - 放在控制器接口上而不是实现类上
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * Assign a name to this mapping.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used on both levels, a combined name is derived by concatenation
	 * with "#" as separator.
	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 */
	// 为该映射分配一个名称。
	// 在类型级别和方法级别都支持！当在两个级别上使用时，组合名称是通过以“#”作为分隔符的串联得出的。
	String name() default "";

	/**
	 * The primary mapping expressed by this annotation.
	 * <p>This is an alias for {@link #path}. For example,
	 * {@code @RequestMapping("/foo")} is equivalent to
	 * {@code @RequestMapping(path="/foo")}.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
	 * explicitly is effectively mapped to an empty path.
	 */
	// 此注释表示的主要映射。
	//
	// 这是path的别名。例如， @RequestMapping("/foo")等价于@RequestMapping(path="/foo") 。
	//
	// 在类型级别和方法级别都支持！当在类型级别使用时，所有方法级别的映射都继承此主映射，将其缩小到特定的处理程序方法。
	//
	// 注意：未显式映射到任何路径的处理程序方法有效地映射到空路径
	@AliasFor("path")
	String[] value() default {};

	/**
	 * The path mapping URIs (e.g. {@code "/profile"}).
	 * <p>Ant-style path patterns are also supported (e.g. {@code "/profile/**"}).
	 * At the method level, relative paths (e.g. {@code "edit"}) are supported
	 * within the primary mapping expressed at the type level.
	 * Path mapping URIs may contain placeholders (e.g. <code>"/${profile_path}"</code>).
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
	 * explicitly is effectively mapped to an empty path.
	 * @since 4.2
	 */
	// 路径映射 URI（例如"/profile" ）。
	//
	// 还支持 Ant 样式的路径模式（例如"/profile/**" ）。在方法级别，在类型级别表示的主映射中支持相对路径（例如"edit" ）。
	// 路径映射 URI 可能包含占位符（例如"/${profile_path}" ）。
	//
	// 在类型级别和方法级别都支持！当在类型级别使用时，所有方法级别的映射都继承此主映射，将其缩小到特定的处理程序方法。
	//
	// 注意：未显式映射到任何路径的处理程序方法有效地映射到空路径。
	@AliasFor("value")
	String[] path() default {};

	/**
	 * The HTTP request methods to map to, narrowing the primary mapping:
	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * HTTP method restriction.
	 */
	// 要映射到的 HTTP 请求方法，缩小主映射：GET、POST、HEAD、OPTIONS、PUT、PATCH、DELETE、TRACE。
	//
	// 在类型级别和方法级别都支持！在类型级别使用时，所有方法级别的映射都会继承此 HTTP 方法限制。
	RequestMethod[] method() default {};

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "myParam=myValue" style
	 * expressions, with a request only mapped if each such parameter is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "myParam!=myValue". "myParam" style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, "!myParam" style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * parameter restriction.
	 */
	// 映射请求的参数，缩小主映射。
	//
	// 适用于任何环境的相同格式：一系列 “myParam=myValue”样式表达式，仅在发现每个此类参数具有给定值时才映射请求。
	// 表达式可以通过使用“！=”运算符来否定，如“myParam！= myValue”。还支持“myParam”样式表达式，
	// 此类参数必须存在于请求中（允许具有任何值）。最后，“！myParam”样式表达式表明指定的参数不应该出现在请求中。
	//
	// 在类型级别和方法级别都支持！在类型级别使用时，所有方法级别映射都继承此参数限制
	String[] params() default {};

	/**
	 * The headers of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "My-Header=myValue" style
	 * expressions, with a request only mapped if each such header is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "My-Header!=myValue". "My-Header" style expressions are also supported,
	 * with such headers having to be present in the request (allowed to have
	 * any value). Finally, "!My-Header" style expressions indicate that the
	 * specified header is <i>not</i> supposed to be present in the request.
	 * <p>Also supports media type wildcards (*), for headers such as Accept
	 * and Content-Type. For instance,
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * will match requests with a Content-Type of "text/html", "text/plain", etc.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit this
	 * header restriction.
	 * @see org.springframework.http.MediaType
	 */
	// 映射请求的标头，缩小主映射。
	//
	// 适用于任何环境的相同格式：一系列 “My-Header=myValue”样式表达式，仅在发现每个此类标头具有给定值时才映射请求。
	// 可以使用“！=”运算符来否定表达式，如“My-Header！= myValue”。还支持“My-Header”样式表达式，
	// 此类标头必须出现在请求中（允许具有任何值）。最后，“!My-Header”样式表达式表明指定的标头不应该出现在请求中。
	//
	// 还支持媒体类型通配符 (*)，用于标头，例如 Accept 和 Content-Type。例如，
	//	   @RequestMapping(value = "/something", headers = "content-type=text/*")
	//
	//
	// 将匹配 Content-Type 为“text/html”、“text/plain”等的请求。
	//
	// 在类型级别和方法级别都支持！在类型级别使用时，所有方法级别映射都继承此标头限制。
	String[] headers() default {};

	/**
	 * Narrows the primary mapping by media types that can be consumed by the
	 * mapped handler. Consists of one or more media types one of which must
	 * match to the request {@code Content-Type} header. Examples:
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * consumes = MediaType.TEXT_PLAIN_VALUE
	 * </pre>
	 * Expressions can be negated by using the "!" operator, as in
	 * "!text/plain", which matches all requests with a {@code Content-Type}
	 * other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * If specified at both levels, the method level consumes condition overrides
	 * the type level condition.
	 * @see org.springframework.http.MediaType
	 * @see javax.servlet.http.HttpServletRequest#getContentType()
	 */
	// 按映射处理程序可以使用的媒体类型缩小主映射。由一种或多种媒体类型组成，其中一种媒体类型必须与请求的Content-Type标头匹配。例子：
	//	   consumes = "text/plain"
	//	   consumes = {"text/plain", "application/*"}
	//	   consumes = MediaType.TEXT_PLAIN_VALUE
	//
	// 可以使用“！”来否定表达式运算符，如 "!text/plain"，它匹配所有具有除 "text/plain" 以外的Content-Type的请求。
	// 在类型级别和方法级别都支持！如果在两个级别都指定，则方法级别的使用条件会覆盖类型级别的条件。
	String[] consumes() default {};

	/**
	 * Narrows the primary mapping by media types that can be produced by the
	 * mapped handler. Consists of one or more media types one of which must
	 * be chosen via content negotiation against the "acceptable" media types
	 * of the request. Typically those are extracted from the {@code "Accept"}
	 * header but may be derived from query parameters, or other. Examples:
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = MediaType.TEXT_PLAIN_VALUE
	 * produces = "text/plain;charset=UTF-8"
	 * </pre>
	 * <p>If a declared media type contains a parameter (e.g. "charset=UTF-8",
	 * "type=feed", "type=entry") and if a compatible media type from the request
	 * has that parameter too, then the parameter values must match. Otherwise
	 * if the media type from the request does not contain the parameter, it is
	 * assumed the client accepts any value.
	 * <p>Expressions can be negated by using the "!" operator, as in "!text/plain",
	 * which matches all requests with a {@code Accept} other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * If specified at both levels, the method level produces condition overrides
	 * the type level condition.
	 * @see org.springframework.http.MediaType
	 */
	// 按可以由映射处理程序生成的媒体类型缩小主要映射。由一种或多种媒体类型组成，其中一种必须通过针对
	// 请求的“可接受”媒体类型的内容协商来选择。通常，这些是从"Accept"标头中提取的，但可能来自查询参数或其他参数。例子：
	//	   produces = "text/plain"
	//	   produces = {"text/plain", "application/*"}
	//	   produces = MediaType.TEXT_PLAIN_VALUE
	//	   produces = "text/plain;charset=UTF-8"
	//
	// 如果声明的媒体类型包含参数（例如“charset=UTF-8”、“type=feed”、“type=entry”），并且请求中的兼容媒体类型
	// 也具有该参数，则参数值必须匹配.否则，如果请求中的媒体类型不包含参数，则假定客户端接受任何值。
	//
	// 可以使用“！”来否定表达式运算符，如 "!text/plain"，它匹配所有带有Accept而非 "text/plain" 的请求。
	//
	// 在类型级别和方法级别都支持！如果在两个级别都指定，则方法级别生成的条件会覆盖类型级别的条件。
	String[] produces() default {};

}
