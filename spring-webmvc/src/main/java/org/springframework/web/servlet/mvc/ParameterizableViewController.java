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

package org.springframework.web.servlet.mvc;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Trivial controller that always returns a pre-configured view and optionally
 * sets the response status code. The view and status can be configured using
 * the provided configuration properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rossen Stoyanchev
 */
// 始终返回预配置视图并可选择设置响应状态代码的普通控制器。可以使用提供的配置属性来配置视图和状态
public class ParameterizableViewController extends AbstractController {

	// 定义视图
	@Nullable
	private Object view;

	// HttpStatus
	@Nullable
	private HttpStatus statusCode;

	private boolean statusOnly;


	public ParameterizableViewController() {
		super(false);
		setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}

	/**
	 * Set a view name for the ModelAndView to return, to be resolved by the
	 * DispatcherServlet via a ViewResolver. Will override any pre-existing
	 * view name or View.
	 */
	// 为要返回的 ModelAndView 设置视图名称，由 DispatcherServlet 通过 ViewResolver 解析。
	// 将覆盖任何预先存在的视图名称或视图
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * Return the name of the view to delegate to, or {@code null} if using a
	 * View instance.
	 */
	// 返回要委托给的视图的名称，如果使用 View 实例，则返回null
	@Nullable
	public String getViewName() {
		if (this.view instanceof String) {
			String viewName = (String) this.view;
			if (getStatusCode() != null && getStatusCode().is3xxRedirection()) {
				return viewName.startsWith("redirect:") ? viewName : "redirect:" + viewName;
			}
			else {
				return viewName;
			}
		}
		return null;
	}

	/**
	 * Set a View object for the ModelAndView to return.
	 * Will override any pre-existing view name or View.
	 * @since 4.1
	 */
	// 为要返回的 ModelAndView 设置一个 View 对象。将覆盖任何预先存在的视图名称或视图
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * Return the View object, or {@code null} if we are using a view name
	 * to be resolved by the DispatcherServlet via a ViewResolver.
	 * @since 4.1
	 */
	//返回 View 对象，如果我们使用由 DispatcherServlet 通过 ViewResolver 解析的视图名称，则返回null
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * Configure the HTTP status code that this controller should set on the
	 * response.
	 * <p>When a "redirect:" prefixed view name is configured, there is no need
	 * to set this property since RedirectView will do that. However this property
	 * may still be used to override the 3xx status code of {@code RedirectView}.
	 * For full control over redirecting provide a {@code RedirectView} instance.
	 * <p>If the status code is 204 and no view is configured, the request is
	 * fully handled within the controller.
	 * @since 4.1
	 */
	// 配置此控制器应在响应上设置的 HTTP 状态代码。
	//
	// 当配置了“redirect:”前缀视图名称时，无需设置此属性，因为 RedirectView 会这样做。
	// 但是，此属性仍可用于覆盖RedirectView的 3xx 状态代码。要完全控制重定向，请提供 RedirectView 实例。
	//
	// 如果状态码为 204 且未配置视图，则请求在控制器内完全处理
	public void setStatusCode(@Nullable HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Return the configured HTTP status code or {@code null}.
	 * @since 4.1
	 */
	// 返回配置的 HTTP 状态码或null
	@Nullable
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}


	/**
	 * The property can be used to indicate the request is considered fully
	 * handled within the controller and that no view should be used for rendering.
	 * Useful in combination with {@link #setStatusCode}.
	 * <p>By default this is set to {@code false}.
	 * @since 4.1
	 */
	// 该属性可用于指示请求被视为在控制器内完全处理，并且不应使用视图进行渲染。与 setStatusCode 结合使用很有用。
	//
	// 默认情况下，这设置为 false
	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	/**
	 * Whether the request is fully handled within the controller.
	 */
	// 请求是否在控制器内得到完全处理
	public boolean isStatusOnly() {
		return this.statusOnly;
	}


	/**
	 * Return a ModelAndView object with the specified view name.
	 * <p>The content of the {@link RequestContextUtils#getInputFlashMap
	 * "input" FlashMap} is also added to the model.
	 * @see #getViewName()
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String viewName = getViewName();

		if (getStatusCode() != null) {
			if (getStatusCode().is3xxRedirection()) {
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, getStatusCode());
			}
			else {
				response.setStatus(getStatusCode().value());
				if (getStatusCode().equals(HttpStatus.NO_CONTENT) && viewName == null) {
					return null;
				}
			}
		}

		if (isStatusOnly()) {
			return null;
		}

		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(RequestContextUtils.getInputFlashMap(request));
		if (viewName != null) {
			modelAndView.setViewName(viewName);
		}
		else {
			modelAndView.setView(getView());
		}
		return modelAndView;
	}

	@Override
	public String toString() {
		return "ParameterizableViewController [" + formatStatusAndView() + "]";
	}

	private String formatStatusAndView() {
		StringBuilder sb = new StringBuilder();
		if (this.statusCode != null) {
			sb.append("status=").append(this.statusCode);
		}
		if (this.view != null) {
			sb.append(sb.length() != 0 ? ", " : "");
			String viewName = getViewName();
			sb.append("view=").append(viewName != null ? "\"" + viewName + "\"" : this.view);
		}
		return sb.toString();
	}
}
