/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * Interface to provide configuration for a web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * <p>This interface adds a {@code getServletContext()} method to the generic
 * ApplicationContext interface, and defines a well-known application attribute name
 * that the root context must be bound to in the bootstrap process.
 *
 * <p>Like generic application contexts, web application contexts are hierarchical.
 * There is a single root context per application, while each servlet in the application
 * (including a dispatcher servlet in the MVC framework) has its own child context.
 *
 * <p>In addition to standard application context lifecycle capabilities,
 * WebApplicationContext implementations need to detect {@link ServletContextAware}
 * beans and invoke the {@code setServletContext} method accordingly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 19, 2001
 * @see ServletContextAware#setServletContext
 */
// 为 Web 应用程序提供配置的接口。这在应用程序运行时是只读的，但如果实现它可能会重新加载。
//
// <p>这个接口在通用的ApplicationContext接口中添加了一个{@code getServletContext()}方法，
// 并定义了一个众所周知的应用程序属性名称，在引导过程中必须绑定根上下文。
//
// <p>与通用应用程序上下文一样，Web 应用程序上下文也是分层的。每个应用程序有一个根上下文，
// 而应用程序中的每个 servlet（包括 MVC 框架中的调度程序 servlet）都有自己的子上下文。
//
// <p>除了标准的应用程序上下文生命周期功能外，WebApplicationContext 实现需要检测
// {@link ServletContextAware} bean 并相应地调用 {@code setServletContext} 方法。
//
// 针对 Servlet web 引擎实现增加了除了标准的的 "singleton" 和 "prototype" 之外，专门为 servlet 引擎增加了
// "request","session","applicaionContext" 作用域
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * Context attribute to bind root WebApplicationContext to on successful startup.
	 * <p>Note: If the startup of the root context fails, this attribute can contain
	 * an exception or error as value. Use WebApplicationContextUtils for convenient
	 * lookup of the root WebApplicationContext.
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	// 在成功启动时将根 WebApplicationContext 绑定到的上下文属性。<p>注意：如果根上下文启动失败，
	// 该属性可以包含异常或错误作为值。使用 WebApplicationContextUtils 可以方便地查找根 WebApplicationContext。
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * Scope identifier for request scope: "request".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	// 请求范围的范围标识符："request"。此外还支持标准范围 "singleton" 和 "prototype"
	String SCOPE_REQUEST = "request";

	/**
	 * Scope identifier for session scope: "session".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	// 请求范围的范围标识符："session"。此外还支持标准范围 "singleton" 和 "prototype"
	String SCOPE_SESSION = "session";

	/**
	 * Scope identifier for the global web application scope: "application".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	// 请求范围的范围标识符："application"。此外还支持标准范围 "singleton" 和 "prototype"
	String SCOPE_APPLICATION = "application";

	/**
	 * Name of the ServletContext environment bean in the factory.
	 * @see javax.servlet.ServletContext
	 */
	// 工厂中 ServletContext 环境 bean 的名称。
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * Name of the ServletContext init-params environment bean in the factory.
	 * <p>Note: Possibly merged with ServletConfig parameters.
	 * ServletConfig parameters override ServletContext parameters of the same name.
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	// 工厂中 ServletContext init-params 环境 bean 的名称。
	// <p>注意：可能与 ServletConfig 参数合并。 ServletConfig 参数覆盖同名的 ServletContext 参数。
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * Name of the ServletContext attributes environment bean in the factory.
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	// 工厂中 ServletContext 属性环境 bean 的名称
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * Return the standard Servlet API ServletContext for this application.
	 */
	// 返回此应用程序的标准 Servlet API ServletContext
	@Nullable
	ServletContext getServletContext();

}
