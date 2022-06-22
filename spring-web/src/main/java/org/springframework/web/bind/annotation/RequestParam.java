/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Map;

/**
 * Annotation which indicates that a method parameter should be bound to a web
 * request parameter.
 *
 * <p>Supported for annotated handler methods in Spring MVC and Spring WebFlux
 * as follows:
 * <ul>
 * <li>In Spring MVC, "request parameters" map to query parameters, form data,
 * and parts in multipart requests. This is because the Servlet API combines
 * query parameters and form data into a single map called "parameters", and
 * that includes automatic parsing of the request body.
 * <li>In Spring WebFlux, "request parameters" map to query parameters only.
 * To work with all 3, query, form data, and multipart data, you can use data
 * binding to a command object annotated with {@link ModelAttribute}.
 * </ul>
 *
 * <p>If the method parameter type is {@link Map} and a request parameter name
 * is specified, then the request parameter value is converted to a {@link Map}
 * assuming an appropriate conversion strategy is available.
 *
 * <p>If the method parameter is {@link java.util.Map Map&lt;String, String&gt;} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * and a parameter name is not specified, then the map parameter is populated
 * with all request parameter names and values.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see RequestMapping
 * @see RequestHeader
 * @see CookieValue
 */
// 指示方法参数应绑定到 Web 请求参数的注解。
// 支持 Spring MVC 和 Spring WebFlux 中带注解的处理程序方法，如下所示:
// - 在 Spring MVC 中，“请求参数”映射到查询参数、表单数据和多部分请求中的部分。
//   这是因为 Servlet API 将查询参数和表单数据组合到一个称为“参数”的映射中，其中包括请求正文的自动解析
//
// - 在 Spring WebFlux 中，“请求参数”仅映射到查询参数。要使用所有 3、查询、表单数据和多部分数据，
//   您可以使用数据绑定到使用 ModelAttribute 注解的命令对象
//
// 如果方法参数类型是Map并且指定了请求参数名称，则假设适当的转换策略可用，则请求参数值将转换为 Map 。
//
// 如果方法参数是 Map<String, String> 或 MultiValueMap<String, String> 并且未指定参数名称，
// 则使用所有请求参数名称和值填充 map 参数。
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the request parameter to bind to.
	 * @since 4.2
	 */
	// 要绑定到的请求参数的名称，一般是 h5 标签中 name 属性的值
	// 这里的 value 可以不写的原因是:具体类上的方法名称是可以保留下来的，但在接口上面的方法名称保留不下来，
	// 比如 Spring Cloud 的 Stream 接口上面，在定义那些注解的时候通常把相关的东西加上去，在Java 标准里边，
	// jxrx 那种注解里边，它是必须把属性关联起来，因为它不能保证你所标注的接口一定是类，它要确认无论你是类或者接口，
	// 它都要进行强绑定
	// {@link MethodParameter} 中的属性 parameterName
	@AliasFor("value")
	String name() default "";

	/**
	 * Whether the parameter is required.
	 * <p>Defaults to {@code true}, leading to an exception being thrown
	 * if the parameter is missing in the request. Switch this to
	 * {@code false} if you prefer a {@code null} value if the parameter is
	 * not present in the request.
	 * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
	 * sets this flag to {@code false}.
	 */
	// 参数是否必填。
	//
	// 默认为true ，如果请求中缺少参数，则会引发异常。如果请求中不存在参数，则如果您更喜欢null值，请将其切换为false 。
	//
	// 或者，提供defaultValue ，隐式将此标志设置为false
	boolean required() default true;

	/**
	 * The default value to use as a fallback when the request parameter is
	 * not provided or has an empty value.
	 * <p>Supplying a default value implicitly sets {@link #required} to
	 * {@code false}.
	 */
	// 当请求参数未提供或具有空值时用作后备的默认值。
	// 提供默认值会隐式地将 required 设置为 false
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
