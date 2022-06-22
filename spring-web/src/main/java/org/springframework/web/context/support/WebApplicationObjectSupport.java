/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * Convenient superclass for application objects running in a {@link WebApplicationContext}.
 * Provides {@code getWebApplicationContext()}, {@code getServletContext()}, and
 * {@code getTempDir()} accessors.
 *
 * <p>Note: It is generally recommended to use individual callback interfaces for the actual
 * callbacks needed. This broad base class is primarily intended for use within the framework,
 * in case of {@link ServletContext} access etc typically being needed.
 *
 * @author Juergen Hoeller
 * @since 28.08.2003
 * @see SpringBeanAutowiringSupport
 */
// 在 WebApplicationContext 中运行的应用程序对象的方便超类。提供 getWebApplicationContext() 、
// getServletContext()和 getTempDir()访问器。
//
// 注意：对于实际需要的回调，通常建议使用单独的回调接口。这个广泛的基类主要用于框架内，以防通常需要 ServletContext 访问等。
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {

	// ServletContext
	@Nullable
	private ServletContext servletContext;


	@Override
	public final void setServletContext(ServletContext servletContext) {
		if (servletContext != this.servletContext) {
			this.servletContext = servletContext;
			// 初始化 ServletContext
			initServletContext(servletContext);
		}
	}

	/**
	 * Overrides the base class behavior to enforce running in an ApplicationContext.
	 * All accessors will throw IllegalStateException if not running in a context.
	 * @see #getApplicationContext()
	 * @see #getMessageSourceAccessor()
	 * @see #getWebApplicationContext()
	 * @see #getServletContext()
	 * @see #getTempDir()
	 */
	// 覆盖基类行为以强制在 ApplicationContext 中运行
	// 如果不在上下文中运行，所有访问器都将抛出 IllegalStateException
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * Calls {@link #initServletContext(javax.servlet.ServletContext)} if the
	 * given ApplicationContext is a {@link WebApplicationContext}.
	 */
	// 如果给定的 ApplicationContext 是 WebApplicationContext ，则调用 initServletContext(ServletContext) 。
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		if (this.servletContext == null && context instanceof WebApplicationContext) {
			this.servletContext = ((WebApplicationContext) context).getServletContext();
			if (this.servletContext != null) {
				initServletContext(this.servletContext);
			}
		}
	}

	/**
	 * Subclasses may override this for custom initialization based
	 * on the ServletContext that this application object runs in.
	 * <p>The default implementation is empty. Called by
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}
	 * as well as {@link #setServletContext(javax.servlet.ServletContext)}.
	 * @param servletContext the ServletContext that this application object runs in
	 * (never {@code null})
	 */
	// 子类可以根据运行此应用程序对象的 ServletContext 覆盖此自定义初始化。
	//
	// 默认实现为空。由initApplicationContext(ApplicationContext)
	// 和 setServletContext setServletContext(ServletContext)调用。
	//
	// 参形：
	//			servletContext – 运行此应用程序对象的 ServletContext（从不为null ）
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * Return the current application context as WebApplicationContext.
	 * <p><b>NOTE:</b> Only use this if you actually need to access
	 * WebApplicationContext-specific functionality. Preferably use
	 * {@code getApplicationContext()} or {@code getServletContext()}
	 * else, to be able to run in non-WebApplicationContext environments as well.
	 * @throws IllegalStateException if not running in a WebApplicationContext
	 * @see #getApplicationContext()
	 */
	// 将当前应用程序上下文作为 WebApplicationContext 返回。
	//
	// 注意：仅当您确实需要访问特定于 WebApplicationContext 的功能时才使用它。最好使用 getApplicationContext()
	// 或 getServletContext() else，以便能够在非 WebApplicationContext 环境中运行。
	// 抛出：
	//			IllegalStateException – 如果不在 WebApplicationContext 中运行
	@Nullable
	protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		ApplicationContext ctx = getApplicationContext();
		if (ctx instanceof WebApplicationContext) {
			return (WebApplicationContext) getApplicationContext();
		}
		else if (isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run in a WebApplicationContext but in: " + ctx);
		}
		else {
			return null;
		}
	}

	/**
	 * Return the current ServletContext.
	 * @throws IllegalStateException if not running within a required ServletContext
	 * @see #isContextRequired()
	 */
	// 返回当前的 ServletContext。
	// 抛出：
	//				IllegalStateException – 如果不在所需的 ServletContext 中运行
	@Nullable
	protected final ServletContext getServletContext() throws IllegalStateException {
		if (this.servletContext != null) {
			return this.servletContext;
		}
		ServletContext servletContext = null;
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			servletContext = wac.getServletContext();
		}
		if (servletContext == null && isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run within a ServletContext. Make sure the object is fully configured!");
		}
		return servletContext;
	}

	/**
	 * Return the temporary directory for the current web application,
	 * as provided by the servlet container.
	 * @return the File representing the temporary directory
	 * @throws IllegalStateException if not running within a ServletContext
	 * @see org.springframework.web.util.WebUtils#getTempDir(javax.servlet.ServletContext)
	 */
	// 返回当前 Web 应用程序的临时目录，由 servlet 容器提供。
	// 返回值：
	//			代表临时目录的文件
	// 抛出：
	//			IllegalStateException – 如果不在 ServletContext 中运行
	protected final File getTempDir() throws IllegalStateException {
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "ServletContext is required");
		return WebUtils.getTempDir(servletContext);
	}

}
