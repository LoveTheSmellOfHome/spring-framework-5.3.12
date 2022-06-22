/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ThreadFactory;

import org.springframework.util.CustomizableThreadCreator;

/**
 * Implementation of the {@link java.util.concurrent.ThreadFactory} interface,
 * allowing for customizing the created threads (name, priority, etc).
 *
 * <p>See the base class {@link org.springframework.util.CustomizableThreadCreator}
 * for details on the available configuration options.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setThreadNamePrefix
 * @see #setThreadPriority
 */
// ThreadFactory 接口的实现，允许自定义创建的线程（名称、优先级等）。
// 有关可用配置选项的详细信息，请参阅基类 CustomizableThreadCreator 。
@SuppressWarnings("serial")
public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {

	/**
	 * Create a new CustomizableThreadFactory with default thread name prefix.
	 */
	// 使用默认线程名称前缀创建一个新的 CustomizableThreadFactory
	public CustomizableThreadFactory() {
		super();
	}

	/**
	 * Create a new CustomizableThreadFactory with the given thread name prefix.
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	// 使用给定的线程名称前缀创建一个新的 CustomizableThreadFactory。
	// 参形：
	//			threadNamePrefix – 用于新创建线程名称的前缀
	public CustomizableThreadFactory(String threadNamePrefix) {
		super(threadNamePrefix);
	}


	@Override
	public Thread newThread(Runnable runnable) {
		return createThread(runnable);
	}

}
