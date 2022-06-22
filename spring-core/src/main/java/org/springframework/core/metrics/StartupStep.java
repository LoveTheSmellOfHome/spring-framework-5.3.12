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

package org.springframework.core.metrics;

import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * Step recording metrics about a particular phase or action happening during the {@link ApplicationStartup}.
 *
 * <p>The lifecycle of a {@code StartupStep} goes as follows:
 * <ol>
 * <li>the step is created and starts by calling {@link ApplicationStartup#start(String) the application startup}
 * and is assigned a unique {@link StartupStep#getId() id}.
 * <li>we can then attach information with {@link Tags} during processing
 * <li>we then need to mark the {@link #end()} of the step
 * </ol>
 *
 * <p>Implementations can track the "execution time" or other metrics for steps.
 *
 * @author Brian Clozel
 * @since 5.3
 */
// 步骤记录有关在ApplicationStartup期间发生的特定阶段或操作的指标。
// StartupStep的生命周期如下：
// 		1.该步骤是通过调用the application startup来创建和the application startup ，并被分配一个唯一的id 。
//		2.然后我们可以在处理过程中使用StartupStep.Tags附加信息
//		3.然后我们需要标记步骤的end()
// 实现可以跟踪“执行时间”或步骤的其他指标。
public interface StartupStep {

	/**
	 * Return the name of the startup step.
	 * <p>A step name describes the current action or phase. This technical
	 * name should be "." namespaced and can be reused to describe other instances of
	 * similar steps during application startup.
	 */
	// 返回启动步骤的名称。
	// 步骤名称描述当前操作或阶段。 此技术名称应为“.”。 命名空间，并且可以重用于描述应用程序启动期间类似步骤的其他实例
	String getName();

	/**
	 * Return the unique id for this step within the application startup.
	 */
	// 在应用程序启动中返回此步骤的唯一 ID
	long getId();

	/**
	 * Return, if available, the id of the parent step.
	 * <p>The parent step is the step that was started the most recently
	 * when the current step was created.
	 */
	// 如果可用，返回父步骤的 id。
	// 父步骤是创建当前步骤时最近启动的步骤
	@Nullable
	Long getParentId();

	/**
	 * Add a {@link Tag} to the step.
	 * @param key tag key
	 * @param value tag value
	 */
	// 将StartupStep.Tag添加到步骤。
	// 形参：
	//		key – 标签键
	//		value - 标签值
	StartupStep tag(String key, String value);

	/**
	 * Add a {@link Tag} to the step.
	 * @param key tag key
	 * @param value {@link Supplier} for the tag value
	 */
	// 将StartupStep.Tag添加到步骤。
	// 形参：
	// 		key – 标签键
	//		value – 标签值的Supplier
	StartupStep tag(String key, Supplier<String> value);

	/**
	 * Return the {@link Tag} collection for this step.
	 */
	// 返回此步骤的StartupStep.Tag集合。
	Tags getTags();

	/**
	 * Record the state of the step and possibly other metrics like execution time.
	 * <p>Once ended, changes on the step state are not allowed.
	 */
	// 记录步骤的状态以及可能的其他指标，例如执行时间。
	// 结束后，不允许更改步骤状态
	void end();


	/**
	 * Immutable collection of {@link Tag}.
	 */
	// StartupStep.Tag不可变集合。
	interface Tags extends Iterable<Tag> {
	}


	/**
	 * Simple key/value association for storing step metadata.
	 */
	// 用于存储步骤元数据的简单 key/value 关联
	interface Tag {

		/**
		 * Return the {@code Tag} name.
		 */
		// 返回Tag名称
		String getKey();

		/**
		 * Return the {@code Tag} value.
		 */
		// 返回Tag值
		String getValue();
	}

}
