/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * Java 5 enumeration of HTTP request methods. Intended for use with the
 * {@link RequestMapping#method()} attribute of the {@link RequestMapping} annotation.
 *
 * <p>Note that, by default, {@link org.springframework.web.servlet.DispatcherServlet}
 * supports GET, HEAD, POST, PUT, PATCH and DELETE only. DispatcherServlet will
 * process TRACE and OPTIONS with the default HttpServlet behavior unless explicitly
 * told to dispatch those request types as well: Check out the "dispatchOptionsRequest"
 * and "dispatchTraceRequest" properties, switching them to "true" if necessary.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see RequestMapping
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchTraceRequest
 */
// HTTP 请求方法的 Java 5 枚举。旨在与 RequestMapping 注释的 RequestMapping.method() 属性一起使用。
//
// 请注意，默认情况下， org.springframework.web.servlet.DispatcherServlet 仅支持
// GET、HEAD、POST、PUT、PATCH 和 DELETE。 DispatcherServlet 将使用默认的 HttpServlet 行为
// 处理 TRACE 和 OPTIONS，除非明确告知也分派这些请求类型：检查“dispatchOptionsRequest”
// 和“dispatchTraceRequest”属性，如有必要，将它们切换为“true”。
public enum RequestMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE

}
