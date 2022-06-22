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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Workflow interface that allows for customized handler execution chains.
 * Applications can register any number of existing or custom interceptors
 * for certain groups of handlers, to add common preprocessing behavior
 * without needing to modify each handler implementation.
 *
 * <p>A HandlerInterceptor gets called before the appropriate HandlerAdapter
 * triggers the execution of the handler itself. This mechanism can be used
 * for a large field of preprocessing aspects, e.g. for authorization checks,
 * or common handler behavior like locale or theme changes. Its main purpose
 * is to allow for factoring out repetitive handler code.
 *
 * <p>In an asynchronous processing scenario, the handler may be executed in a
 * separate thread while the main thread exits without rendering or invoking the
 * {@code postHandle} and {@code afterCompletion} callbacks. When concurrent
 * handler execution completes, the request is dispatched back in order to
 * proceed with rendering the model and all methods of this contract are invoked
 * again. For further options and details see
 * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>Typically an interceptor chain is defined per HandlerMapping bean,
 * sharing its granularity. To be able to apply a certain interceptor chain
 * to a group of handlers, one needs to map the desired handlers via one
 * HandlerMapping bean. The interceptors themselves are defined as beans
 * in the application context, referenced by the mapping bean definition
 * via its "interceptors" property (in XML: a &lt;list&gt; of &lt;ref&gt;).
 *
 * <p>HandlerInterceptor is basically similar to a Servlet Filter, but in
 * contrast to the latter it just allows custom pre-processing with the option
 * of prohibiting the execution of the handler itself, and custom post-processing.
 * Filters are more powerful, for example they allow for exchanging the request
 * and response objects that are handed down the chain. Note that a filter
 * gets configured in web.xml, a HandlerInterceptor in the application context.
 *
 * <p>As a basic guideline, fine-grained handler-related preprocessing tasks are
 * candidates for HandlerInterceptor implementations, especially factored-out
 * common handler code and authorization checks. On the other hand, a Filter
 * is well-suited for request content and view content handling, like multipart
 * forms and GZIP compression. This typically shows when one needs to map the
 * filter to certain content types (e.g. images), or to all requests.
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
 * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
 * @see org.springframework.web.servlet.theme.ThemeChangeInterceptor
 * @see javax.servlet.Filter
 */
// 允许自定义处理程序执行链的工作流接口。应用程序可以为某些处理程序组注册任意数量的现有或自定义拦截器，
// 以添加常见的预处理行为，而无需修改每个处理程序实现。
//
// 在适当的 HandlerAdapter 触发处理程序本身的执行之前调用 HandlerInterceptor。这种机制可用于预处理方面的大领域，
// 例如授权检查，或常见的处理程序行为，如语言环境或主题更改。它的主要目的是允许分解重复的处理程序代码。
//
// 在异步处理场景中，处理程序可以在单独的线程中执行，而主线程退出而不渲染或调用 postHandle 和 afterCompletion 回调。
// 当并发处理程序执行完成时，请求被分派回来以继续渲染模型，并再次调用此合约的所有方法。有关更多选项和详细信息，
// 请参阅org.springframework.web.servlet.AsyncHandlerInterceptor
//
// 通常，每个 HandlerMapping bean 定义一个拦截器链，共享其粒度。为了能够将某个拦截器链应用于一组处理程序，
// 需要通过一个 HandlerMapping bean 映射所需的处理程序。拦截器本身被定义为应用程序上下文中的 bean，
// 映射 bean 定义通过其“拦截器”属性（在 XML 中：<ref> 的 <list>）引用。
//
// HandlerInterceptor 基本上类似于 Servlet 过滤器，但与后者相比，它只允许自定义预处理和禁止执行处理程序本身的执行，
// 以及自定义后处理。过滤器更强大，例如它们允许交换传递到链上的请求和响应对象。请注意，过滤器在 web.xml 中配置，
// 它是应用程序上下文中的 HandlerInterceptor。
//
// 作为基本准则，与处理程序相关的细粒度预处理任务是 HandlerInterceptor 实现的候选，尤其是分解出的通用处理程序代码和授权检查。
// 另一方面，过滤器非常适合请求内容和视图内容处理，例如多部分表单和 GZIP 压缩。
// 这通常显示何时需要将过滤器映射到某些内容类型（例如图像）或所有请求
public interface HandlerInterceptor {

	/**
	 * Interception point before the execution of a handler. Called after
	 * HandlerMapping determined an appropriate handler object, but before
	 * HandlerAdapter invokes the handler.
	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
	 * of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	 * typically sending an HTTP error or writing a custom response.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation returns {@code true}.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return {@code true} if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
	 * that this interceptor has already dealt with the response itself.
	 * @throws Exception in case of errors
	 */
	// 处理程序执行之前的拦截点。在 HandlerMapping 确定适当的处理程序对象之后，但在 HandlerAdapter 调用处理程序之前调用。
	//
	// DispatcherServlet 处理执行链中的处理程序，该处理程序由任意数量的拦截器组成，处理程序本身位于最后。
	// 使用此方法，每个拦截器都可以决定中止执行链，通常是发送 HTTP 错误或编写自定义响应。
	//
	// 注意：特殊注意事项适用于异步请求处理。有关更多详细信息，请参阅 AsyncHandlerInterceptor 。
	//
	// 默认实现返回 true 。
	// 参形：
	// 					request – 当前的 HTTP 请求
	// 					response – 当前的 HTTP 响应
	// 					handler - 选择执行的处理程序，
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * Interception point after successful execution of a handler.
	 * Called after HandlerAdapter actually invoked the handler, but before the
	 * DispatcherServlet renders the view. Can expose additional model objects
	 * to the view via the given ModelAndView.
	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
	 * of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can post-process an execution,
	 * getting applied in inverse order of the execution chain.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation is empty.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
	 * execution, for type and/or instance examination
	 * @param modelAndView the {@code ModelAndView} that the handler returned
	 * (can also be {@code null})
	 * @throws Exception in case of errors
	 */
	// 成功执行处理程序后的拦截点。在 HandlerAdapter 实际调用处理程序之后，但在 DispatcherServlet 呈现视图之前调用。
	// 可以通过给定的 ModelAndView 向视图公开其他模型对象。
	//
	// DispatcherServlet 处理执行链中的处理程序，该处理程序由任意数量的拦截器组成，处理程序本身位于最后。
	// 使用此方法，每个拦截器都可以对执行进行后处理，以执行链的相反顺序应用。
	//
	// 注意：特殊注意事项适用于异步请求处理。有关更多详细信息，请参阅AsyncHandlerInterceptor 。
	//
	// 默认实现为空。
	//
	// 参形：
	//					request – 当前的 HTTP 请求
	//					response – 当前的 HTTP 响应
	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * Callback after completion of request processing, that is, after rendering
	 * the view. Will be called on any outcome of handler execution, thus allows
	 * for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's {@code preHandle}
	 * method has successfully completed and returned {@code true}!
	 * <p>As with the {@code postHandle} method, the method will be invoked on each
	 * interceptor in the chain in reverse order, so the first interceptor will be
	 * the last to be invoked.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation is empty.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
	 * execution, for type and/or instance examination
	 * @param ex any exception thrown on handler execution, if any; this does not
	 * include exceptions that have been handled through an exception resolver
	 * @throws Exception in case of errors
	 */
	// 请求处理完成后的回调，即渲染视图后。将在处理程序执行的任何结果上调用，从而允许适当的资源清理。
	//
	// 注意：仅当此拦截器的preHandle方法成功完成并返回true时才会调用！
	//
	// 与 postHandle 方法一样，该方法将在链中的每个拦截器上以相反的顺序调用，因此第一个拦截器将是最后一个被调用的拦截器。
	//
	// 注意：特殊注意事项适用于异步请求处理。有关更多详细信息，请参阅 AsyncHandlerInterceptor 。
	//
	// 默认实现为空。
	// 参形：
	//				request – 当前的 HTTP 请求
	//				response – 当前的 HTTP 响应
	//				handler - 开始异步执行的处理程序（
	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
