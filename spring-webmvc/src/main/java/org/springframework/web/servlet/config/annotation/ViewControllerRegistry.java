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

package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assists with the registration of simple automated controllers pre-configured
 * with status code and/or a view.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
// 协助注册预先配置有状态代码和/或视图的简单自动化控制器
public class ViewControllerRegistry {

	@Nullable
	private ApplicationContext applicationContext;

	private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

	private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

	private int order = 1;


	/**
	 * Class constructor with {@link ApplicationContext}.
	 * @since 4.3.12
	 */
	public ViewControllerRegistry(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * Map a URL path or pattern to a view controller to render a response with
	 * the configured status code and view.
	 * <p>Patterns such as {@code "/admin/**"} or {@code "/articles/{articlename:\\w+}"}
	 * are supported. For pattern syntax see {@link PathPattern} when parsed
	 * patterns are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 * <p><strong>Note:</strong> If an {@code @RequestMapping} method is mapped
	 * to a URL for any HTTP method then a view controller cannot handle the
	 * same URL. For this reason it is recommended to avoid splitting URL
	 * handling across an annotated controller and a view controller.
	 */
	// 将 URL 路径或模式映射到视图控制器，以使用配置的状态代码和视图呈现响应。
	//
	// 支持"/admin/**"或"/articles/{articlename:\\w+}"等模式。有关模式语法，请参阅 enabled 解析
	// 模式时的 AntPathMatcher ，否则请参阅PathPattern 。语法与PathPattern基本相同，更适合 Web 使用且更高效。
	//
	// 注意：如果 @RequestMapping 方法映射到任何 HTTP 方法的 URL，则视图控制器无法处理相同的 URL。
	// 出于这个原因，建议避免在带注释的控制器和视图控制器之间拆分 URL 处理
	public ViewControllerRegistration addViewController(String urlPathOrPattern) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPathOrPattern);
		registration.setApplicationContext(this.applicationContext);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Map a view controller to the given URL path or pattern in order to redirect
	 * to another URL.
	 * <p>For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 * <p>By default the redirect URL is expected to be relative to the current
	 * ServletContext, i.e. as relative to the web application root.
	 * @since 4.1
	 */
	// 将视图控制器映射到给定的 URL 路径或模式，以便重定向到另一个 URL。
	//
	// 有关模式语法，请参阅 enabled 解析模式时的 AntPathMatcher ，否则请参阅 PathPattern 。
	// 语法与 PathPattern 基本相同，更适合 Web 使用且更高效。
	//
	// 默认情况下，重定向 URL 应与当前的 ServletContext 相关，即与 Web 应用程序根相关。
	public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
		RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
		registration.setApplicationContext(this.applicationContext);
		this.redirectRegistrations.add(registration);
		return registration;
	}

	/**
	 * Map a simple controller to the given URL path (or pattern) in order to
	 * set the response status to the given code without rendering a body.
	 * <p>For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 * @since 4.1
	 */
	// 将一个简单的控制器映射到给定的 URL 路径（或模式），以便在不呈现正文的情况下将响应状态设置为给定的代码。
	//
	// 有关模式语法，请参阅enabled解析模式时的AntPathMatcher ，否则请参阅PathPattern 。
	// 语法与PathPattern基本相同，更适合 Web 使用且更高效。
	public void addStatusController(String urlPath, HttpStatus statusCode) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registration.setApplicationContext(this.applicationContext);
		registration.setStatusCode(statusCode);
		registration.getViewController().setStatusOnly(true);
		this.registrations.add(registration);
	}

	/**
	 * Specify the order to use for the {@code HandlerMapping} used to map view
	 * controllers relative to other handler mappings configured in Spring MVC.
	 * <p>By default this is set to 1, i.e. right after annotated controllers,
	 * which are ordered at 0.
	 */
	public void setOrder(int order) {
		this.order = order;
	}


	/**
	 * Return the {@code HandlerMapping} that contains the registered view
	 * controller mappings, or {@code null} for no registrations.
	 * @since 4.3.12
	 */
	// 返回包含已注册视图控制器映射的 HandlerMapping ，或 null 表示没有注册
	@Nullable
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
			return null;
		}

		Map<String, Object> urlMap = new LinkedHashMap<>();
		for (ViewControllerRegistration registration : this.registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}

		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

}
