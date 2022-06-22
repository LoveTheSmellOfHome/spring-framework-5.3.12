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

package org.springframework.web.servlet.config.annotation;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

/**
 * Defines callback methods to customize the Java-based configuration for
 * Spring MVC enabled via {@code @EnableWebMvc}.
 *
 * <p>{@code @EnableWebMvc}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
// 定义回调方法以自定义通过@EnableWebMvc启用的 Spring MVC 的基于 Java 的配置。
//
// @EnableWebMvc -annotated 配置类可以实现此接口以被回调并有机会自定义默认配置
public interface WebMvcConfigurer {

	/**
	 * Help with configuring {@link HandlerMapping} path matching options such as
	 * whether to use parsed {@code PathPatterns} or String pattern matching
	 * with {@code PathMatcher}, whether to match trailing slashes, and more.
	 * @since 4.0.3
	 * @see PathMatchConfigurer
	 */
	// 帮助配置 HandlerMapping 路径匹配选项，例如是否使用已解析 PathPatterns 或与 PathMatcher 匹配的字符串模式，是否匹配尾部斜杠等。
	default void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * Configure content negotiation options.
	 */
	// 配置内容协商选项
	default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * Configure asynchronous request handling options.
	 */
	// 配置异步请求处理选项
	default void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * Configure a handler to delegate unhandled requests by forwarding to the
	 * Servlet container's "default" servlet. A common use case for this is when
	 * the {@link DispatcherServlet} is mapped to "/" thus overriding the
	 * Servlet container's default handling of static resources.
	 */
	// 配置处理程序以通过转发到 Servlet 容器的“默认”servlet 来委派未处理的请求。
	// 一个常见的用例是当 DispatcherServlet 映射到“/”从而覆盖 Servlet 容器对静态资源的默认处理时
	default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * Add {@link Converter Converters} and {@link Formatter Formatters} in addition to the ones
	 * registered by default.
	 */
	// 除了默认注册的转换器和格式化程序之外，还添加 Converters和Formatters 程序
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Add Spring MVC lifecycle interceptors for pre- and post-processing of
	 * controller method invocations and resource handler requests.
	 * Interceptors can be registered to apply to all requests or be limited
	 * to a subset of URL patterns.
	 */
	// 添加 Spring MVC 生命周期拦截器，用于控制器方法调用和资源处理程序请求的预处理和后处理。
	// 可以注册拦截器以应用于所有请求或仅限于 URL 模式的子集
	default void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * Add handlers to serve static resources such as images, js, and, css
	 * files from specific locations under web application root, the classpath,
	 * and others.
	 * @see ResourceHandlerRegistry
	 */
	// 添加处理程序以从 Web 应用程序根目录、类路径等的特定位置提供静态资源，例如图像、js 和 css 文件
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * Configure "global" cross origin request processing. The configured CORS
	 * mappings apply to annotated controllers, functional endpoints, and static
	 * resources.
	 * <p>Annotated controllers can further declare more fine-grained config via
	 * {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}.
	 * In such cases "global" CORS configuration declared here is
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) combined}
	 * with local CORS configuration defined on a controller method.
	 * @since 4.2
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	// 配置“全局”跨源请求处理。配置的 CORS 映射适用于带注解的控制器、功能端点和静态资源。
	//
	// 带注释的控制器可以通过 @CrossOrigin 进一步声明更细粒度的配置。在这种情况下，此处声明的“全局”CORS 配置
	// 与控制器方法上定义的本地 CORS 配置combined 。
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure simple automated controllers pre-configured with the response
	 * status code and/or a view to render the response body. This is useful in
	 * cases where there is no need for custom controller logic -- e.g. render a
	 * home page, perform simple site URL redirects, return a 404 status with
	 * HTML content, a 204 with no content, and more.
	 * @see ViewControllerRegistry
	 */
	// 配置预先配置了响应状态代码和/或视图以呈现响应正文的简单自动化控制器。这在不需要自定义控制器逻辑的情况下很
	// 有用——例如呈现主页、执行简单的站点 URL 重定向、返回带有 HTML 内容的 404 状态、没有内容的 204 等等。
	default void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * Configure view resolvers to translate String-based view names returned from
	 * controllers into concrete {@link org.springframework.web.servlet.View}
	 * implementations to perform rendering with.
	 * @since 4.1
	 */
	// 配置视图解析器以将从控制器返回的基于字符串的视图名称转换为具体的 org.springframework.web.servlet.View 实现以执行渲染
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * Add resolvers to support custom controller method argument types.
	 * <p>This does not override the built-in support for resolving handler
	 * method arguments. To customize the built-in support for argument
	 * resolution, configure {@link RequestMappingHandlerAdapter} directly.
	 * @param resolvers initially an empty list
	 */
	// 添加解析器以支持自定义控制器方法参数类型。
	// 这不会覆盖对解析处理程序方法参数的内置支持。要自定义对参数解析的内置支持，请直接配置RequestMappingHandlerAdapter 。
	// 参形：
	//			解析器——最初是一个空列表
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Add handlers to support custom controller method return value types.
	 * <p>Using this option does not override the built-in support for handling
	 * return values. To customize the built-in support for handling return
	 * values, configure RequestMappingHandlerAdapter directly.
	 * @param handlers initially an empty list
	 */
	// 添加处理程序以支持自定义控制器方法返回值类型。
	// 使用此选项不会覆盖对处理返回值的内置支持。要自定义处理返回值的内置支持，请直接配置 RequestMappingHandlerAdapter。
	// 参形：
	//				handlers - 最初是一个空列表
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
	}

	/**
	 * Configure the {@link HttpMessageConverter HttpMessageConverter}s for
	 * reading from the request body and for writing to the response body.
	 * <p>By default, all built-in converters are configured as long as the
	 * corresponding 3rd party libraries such Jackson JSON, JAXB2, and others
	 * are present on the classpath.
	 * <p><strong>Note</strong> use of this method turns off default converter
	 * registration. Alternatively, use
	 * {@link #extendMessageConverters(java.util.List)} to modify that default
	 * list of converters.
	 * @param converters initially an empty list of converters
	 */
	// 将HttpMessageConverter配置为从请求正文读取和写入响应正文。
	//
	// 默认情况下，只要类路径中存在相应的第 3 方库（例如 Jackson JSON、JAXB2 等），就会配置所有内置转换器。
	//
	// 注意使用此方法会关闭默认转换器注册。或者，使用 extendMessageConverters(List) 修改该默认转换器列表。
	//
	// 参形：
	//				转换器——最初是一个空的转换器列表
	default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Extend or modify the list of converters after it has been, either
	 * {@link #configureMessageConverters(List) configured} or initialized with
	 * a default list.
	 * <p>Note that the order of converter registration is important. Especially
	 * in cases where clients accept {@link org.springframework.http.MediaType#ALL}
	 * the converters configured earlier will be preferred.
	 * @param converters the list of configured converters to be extended
	 * @since 4.1.3
	 */
	// 使用默认列表 configured 或初始化后，扩展或修改转换器列表。
	// 请注意，转换器注册的顺序很重要。特别是在客户端接受org.springframework.http.MediaType.ALL的情况下，之前配置的转换器将是首选。
	// 参形：
	//			转换器- 要扩展的已配置转换器列表
	default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Configure exception resolvers.
	 * <p>The given list starts out empty. If it is left empty, the framework
	 * configures a default set of resolvers, see
	 * {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)}.
	 * Or if any exception resolvers are added to the list, then the application
	 * effectively takes over and must provide, fully initialized, exception
	 * resolvers.
	 * <p>Alternatively you can use
	 * {@link #extendHandlerExceptionResolvers(List)} which allows you to extend
	 * or modify the list of exception resolvers configured by default.
	 * @param resolvers initially an empty list
	 * @see #extendHandlerExceptionResolvers(List)
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	// 配置异常解析器。
	//
	// 给定的列表开始为空。如果它留空，框架会配置一组默认的解析器，请参阅
	// WebMvcConfigurationSupport.addDefaultHandlerExceptionResolvers(List,
	// org.springframework.web.accept.ContentNegotiationManager) 。
	// 或者，如果将任何异常解析器添加到列表中，则应用程序有效地接管并且必须提供完全初始化的异常解析器。
	//
	// 或者，您可以使用extendHandlerExceptionResolvers(List) ，它允许您扩展或修改默认配置的异常解析器列表。
	//
	// 参形：
	//				解析器——最初是一个空列表
	default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Extending or modify the list of exception resolvers configured by default.
	 * This can be useful for inserting a custom exception resolver without
	 * interfering with default ones.
	 * @param resolvers the list of configured resolvers to extend
	 * @since 4.3
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	// 扩展或修改默认配置的异常解析器列表。这对于插入自定义异常解析器而不干扰默认异常解析器很有用。
	// 参形：
	//				resolvers – 要扩展的已配置解析器列表
	default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Provide a custom {@link Validator} instead of the one created by default.
	 * The default implementation, assuming JSR-303 is on the classpath, is:
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}.
	 * Leave the return value as {@code null} to keep the default.
	 */
	// 提供自定义 Validator ，而不是默认创建的验证器。假设 JSR-303 在类路径上，默认实现是：
	// org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean 。将返回值保留为null以保持默认值。
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * Provide a custom {@link MessageCodesResolver} for building message codes
	 * from data binding and validation error codes. Leave the return value as
	 * {@code null} to keep the default.
	 */
	// 提供自定义 MessageCodesResolver 用于从数据绑定和验证错误代码构建消息代码。将返回值保留为null以保持默认值
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

}
