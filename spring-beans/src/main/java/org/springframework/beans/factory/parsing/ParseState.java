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

package org.springframework.beans.factory.parsing;

import java.util.ArrayDeque;

import org.springframework.lang.Nullable;

/**
 * Simple {@link ArrayDeque}-based structure for tracking the logical position during
 * a parsing process. {@link Entry entries} are added to the ArrayDeque at each point
 * during the parse phase in a reader-specific manner.
 *
 * <p>Calling {@link #toString()} will render a tree-style view of the current logical
 * position in the parse phase. This representation is intended for use in error messages.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
// 简单的基于 {@link ArrayDeque} 的结构，用于在解析过程中跟踪逻辑位置。
// {@link Entry 条目} 在解析阶段以特定于阅读器的方式在每个点添加到 ArrayDeque。
//
// <p>调用 {@link toString()} 将在解析阶段呈现当前逻辑位置的树形视图。此表示旨在用于错误消息
public final class ParseState {

	/**
	 * Internal {@link ArrayDeque} storage.
	 */
	// 内部 {@link ArrayDeque} 存储
	private final ArrayDeque<Entry> state;


	/**
	 * Create a new {@code ParseState} with an empty {@link ArrayDeque}.
	 */
	// 使用空的 {@link ArrayDeque} 创建一个新的 {@code ParseState}。
	public ParseState() {
		this.state = new ArrayDeque<>();
	}

	/**
	 * Create a new {@code ParseState} whose {@link ArrayDeque} is a clone
	 * of the state in the passed-in {@code ParseState}.
	 */
	// 创建一个新的 {@code ParseState}，其 {@link ArrayDeque} 是传入的 {@code ParseState} 中状态的克隆
	private ParseState(ParseState other) {
		this.state = other.state.clone();
	}


	/**
	 * Add a new {@link Entry} to the {@link ArrayDeque}.
	 */
	// 向 {@link ArrayDeque} 添加一个新的 {@link Entry}
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * Remove an {@link Entry} from the {@link ArrayDeque}.
	 */
	// 从 {@link ArrayDeque} 中删除 {@link Entry}。
	public void pop() {
		this.state.pop();
	}

	/**
	 * Return the {@link Entry} currently at the top of the {@link ArrayDeque} or
	 * {@code null} if the {@link ArrayDeque} is empty.
	 */
	// 如果 {@link ArrayDeque} 为空，则返回当前位于 {@link ArrayDeque} 顶部的 {@link Entry} 或
	// {@code null}。
	@Nullable
	public Entry peek() {
		return this.state.peek();
	}

	/**
	 * Create a new instance of {@link ParseState} which is an independent snapshot
	 * of this instance.
	 */
	// 创建一个新的 {@link ParseState} 实例，它是这个实例的独立快照
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * Returns a tree-style representation of the current {@code ParseState}.
	 */
	// 返回当前 {@code ParseState} 的树样式表示
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		int i = 0;
		for (ParseState.Entry entry : this.state) {
			if (i > 0) {
				sb.append('\n');
				for (int j = 0; j < i; j++) {
					sb.append('\t');
				}
				sb.append("-> ");
			}
			sb.append(entry);
			i++;
		}
		return sb.toString();
	}


	/**
	 * Marker interface for entries into the {@link ParseState}.
	 */
	// 进入 {@link ParseState} 的标记接口
	public interface Entry {
	}

}
