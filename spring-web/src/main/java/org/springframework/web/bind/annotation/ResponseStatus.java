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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

import java.lang.annotation.*;

/**
 * Marks a method or exception class with the status {@link #code} and
 * {@link #reason} that should be returned.
 *
 * <p>The status code is applied to the HTTP response when the handler
 * method is invoked and overrides status information set by other means,
 * like {@code ResponseEntity} or {@code "redirect:"}.
 *
 * <p><strong>Warning</strong>: when using this annotation on an exception
 * class, or when setting the {@code reason} attribute of this annotation,
 * the {@code HttpServletResponse.sendError} method will be used.
 *
 * <p>With {@code HttpServletResponse.sendError}, the response is considered
 * complete and should not be written to any further. Furthermore, the Servlet
 * container will typically write an HTML error page therefore making the
 * use of a {@code reason} unsuitable for REST APIs. For such cases it is
 * preferable to use a {@link org.springframework.http.ResponseEntity} as
 * a return type and avoid the use of {@code @ResponseStatus} altogether.
 *
 * <p>Note that a controller class may also be annotated with
 * {@code @ResponseStatus} which is then inherited by all {@code @RequestMapping}
 * and {@code @ExceptionHandler} methods in that class and its subclasses unless
 * overridden by a local {@code @ResponseStatus} declaration on the method.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
 */
// 使用应返回的状态 code 和 reason 标记方法或异常类。
//
// 当调用处理程序方法并覆盖通过其他方式设置的状态信息时，状态代码将应用于 HTTP 响应，例如 ResponseEntity 或 "redirect:" 。
//
// 警告：在异常类上使用此注解，或设置此注解的 reason 属性时，将使用 HttpServletResponse.sendError方法。
//
// 使用 HttpServletResponse.sendError ，响应被认为是完整的，不应再写入任何内容。此外，Servlet
// 容器通常会编写一个 HTML 错误页面，因此会使用不适合 REST API 的reason 。对于这种情况，最好使用
// org.springframework.http.ResponseEntity作为返回类型，并完全避免使用 @ResponseStatus 。
//
// 请注意，控制器类也可以用 @ResponseStatus 注释，然后由该类及其子类中的所有 @RequestMapping 和 @ExceptionHandler
// 方法继承，除非被方法上的本地 @ResponseStatus 声明覆盖
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

	/**
	 * Alias for {@link #code}.
	 */
	@AliasFor("code")
	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * The status <em>code</em> to use for the response.
	 * <p>Default is {@link HttpStatus#INTERNAL_SERVER_ERROR}, which should
	 * typically be changed to something more appropriate.
	 * @since 4.2
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	// 用于响应的状态代码。
	// 默认值为 HttpStatus.INTERNAL_SERVER_ERROR ，通常应将其更改为更合适的内容。
	@AliasFor("value")
	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * The <em>reason</em> to be used for the response.
	 * <p>Defaults to an empty string which will be ignored. Set the reason to a
	 * non-empty value to have it used for the response.
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
	 */
	// 用于响应的原因。
	// 默认为将被忽略的空字符串。将原因设置为非空值以将其用于响应。
	String reason() default "";

}
