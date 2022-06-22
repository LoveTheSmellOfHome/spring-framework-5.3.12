/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * Strategy interface for resolving messages, with support for the parameterization
 * and internationalization of such messages.
 *
 * <p>Spring provides two out-of-the-box implementations for production:
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}: built
 * on top of the standard {@link java.util.ResourceBundle}, sharing its limitations.
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}:
 * highly configurable, in particular with respect to reloading message definitions.
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
// 用于解析消息的策略接口，支持此类消息的参数化和国际化。
//
// Spring 为生产提供了两种开箱即用的实现：
//  >org.springframework.context.support.ResourceBundleMessageSource ：建立在标准java.util.ResourceBundle 之上，
//  共享其局限性。
//  >org.springframework.context.support.ReloadableResourceBundleMessageSource ：高度可配置，
//  特别是在重新加载消息定义方面。
//
// Spring 国际化接口的核心接口
public interface MessageSource {

	/**
	 * Try to resolve the message. Return default message if no message was found.
	 * @param code the message code to look up, e.g. 'calculator.noRateSet'.
	 * MessageSource users are encouraged to base message names on qualified class
	 * or package names, avoiding potential conflicts and ensuring maximum clarity.
	 * @param args an array of arguments that will be filled in for params within
	 * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
	 * or {@code null} if none
	 * @param defaultMessage a default message to return if the lookup fails
	 * @param locale the locale in which to do the lookup
	 * @return the resolved message if the lookup was successful, otherwise
	 * the default message passed as a parameter (which may be {@code null})
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	// 尝试解决该消息。如果未找到消息，则返回默认消息。
	// @param code 要查找的消息代码，例如'calculator.noRateSet' 文案模板编码
	// 鼓励 MessageSource 用户将消息名称基于限定的类或包名称，以避免潜在的冲突并确保最大程度的清晰度
	// @param args 将在消息中为参数填充的参数数组（参数在消息中类似于“{0}”、“{1,date}”、“{2,time}”），
	// 或 {@代码 null} 如果没有， 文案模板参数
	// @param defaultMessage 查找失败时返回的默认消息
	// @param locale 进行查找的语言环境，区域(Locale)
	// @return 如果查找成功则返回已解析的消息，否则默认消息作为参数传递（可能为 {@code null}）
	@Nullable
	String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

	/**
	 * Try to resolve the message. Treat as an error if the message can't be found.
	 * @param code the message code to look up, e.g. 'calculator.noRateSet'.
	 * MessageSource users are encouraged to base message names on qualified class
	 * or package names, avoiding potential conflicts and ensuring maximum clarity.
	 * @param args an array of arguments that will be filled in for params within
	 * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
	 * or {@code null} if none
	 * @param locale the locale in which to do the lookup
	 * @return the resolved message (never {@code null})
	 * @throws NoSuchMessageException if no corresponding message was found
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	// 尝试解决该消息。如果找不到该消息，则将其视为错误
	// @param code 要查找的消息代码，例如'calculator.noRateSet'
	// 鼓励 MessageSource 用户将消息名称基于限定的类或包名称，以避免潜在的冲突并确保最大程度的清晰度
	String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * Try to resolve the message using all the attributes contained within the
	 * {@code MessageSourceResolvable} argument that was passed in.
	 * <p>NOTE: We must throw a {@code NoSuchMessageException} on this method
	 * since at the time of calling this method we aren't able to determine if the
	 * {@code defaultMessage} property of the resolvable is {@code null} or not.
	 * @param resolvable the value object storing attributes required to resolve a message
	 * (may include a default message)
	 * @param locale the locale in which to do the lookup
	 * @return the resolved message (never {@code null} since even a
	 * {@code MessageSourceResolvable}-provided default message needs to be non-null)
	 * @throws NoSuchMessageException if no corresponding message was found
	 * (and no default message was provided by the {@code MessageSourceResolvable})
	 * @see MessageSourceResolvable#getCodes()
	 * @see MessageSourceResolvable#getArguments()
	 * @see MessageSourceResolvable#getDefaultMessage()
	 * @see java.text.MessageFormat
	 */
	// 尝试使用传入的 {@code MessageSourceResolvable} 参数中包含的所有属性解析消息
	// <p>注意：我们必须在这个方法上抛出一个 {@code NoSuchMessageException}，
	// 因为在调用这个方法时我们无法确定可解析的 {@code defaultMessage} 属性是 {@code null} 还是不是。
	// @param resolvable 存储解析消息所需的属性的值对象（可能包括默认消息）
	// @param locale 进行查找的语言环境
	// @return 已解析的消息（从不{@code null}，因为即使是 {@code MessageSourceResolvable} 提供的默认消息也需要非空）
	// @throws NoSuchMessageException 如果没有找到相应的消息（并且 {@code MessageSourceResolvable} 没有提供默认消息）
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
