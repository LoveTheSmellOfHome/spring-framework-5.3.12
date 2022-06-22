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

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * A property accessor is able to read from (and possibly write to) an object's properties.
 *
 * <p>This interface places no restrictions, and so implementors are free to access properties
 * directly as fields or through getters or in any other way they see as appropriate.
 *
 * <p>A resolver can optionally specify an array of target classes for which it should be
 * called. However, if it returns {@code null} from {@link #getSpecificTargetClasses()},
 * it will be called for all property references and given a chance to determine if it
 * can read or write them.
 *
 * <p>Property resolvers are considered to be ordered, and each will be called in turn.
 * The only rule that affects the call order is that any resolver naming the target
 * class directly in {@link #getSpecificTargetClasses()} will be called first, before
 * the general resolvers.
 *
 * @author Andy Clement
 * @since 3.0
 */
// 属性访问器能够读取（并可能写入）对象的属性
//
// 这个接口没有任何限制，因此实现者可以自由地直接作为字段或通过 getter 或以他们认为合适的任何其他方式访问属性
//
// 解析器可以选择性地指定应为其调用的目标类数组。 但是，如果它从 getSpecificTargetClasses() 返回null ，
// 它将为所有属性引用调用，并有机会确定它是否可以读取或写入它们。
//
// 属性解析器被认为是有序的，每个解析器将被依次调用。 影响调用顺序的唯一规则是任何直接
// 在 getSpecificTargetClasses() 命名目标类的解析器将在通用解析器之前首先被调用。
public interface PropertyAccessor {

	/**
	 * Return an array of classes for which this resolver should be called.
	 * <p>Returning {@code null} indicates this is a general resolver that
	 * can be called in an attempt to resolve a property on any type.
	 * @return an array of classes that this resolver is suitable for
	 * (or {@code null} if a general resolver)
	 */
	// 返回应为其调用此解析器的类数组
	// 返回null表示这是一个通用解析器，可以在尝试解析任何类型的属性时调用它
	// 返回值：此解析器适用的类数组（如果是通用解析器，则为null ）
	@Nullable
	Class<?>[] getSpecificTargetClasses();

	/**
	 * Called to determine if a resolver instance is able to access a specified property
	 * on a specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the property is being accessed
	 * @param name the name of the property being accessed
	 * @return true if this resolver is able to read the property
	 * @throws AccessException if there is any problem determining whether the property can be read
	 */
	// 调用以确定解析器实例是否能够访问指定目标对象上的指定属性。
	// 形参：
	// EvaluationContext- 尝试访问的评估上下文
	// target – 访问属性的目标对象
	// name - 正在访问的属性的名称
	// 返回值：如果此解析器能够读取该属性，则为 true
	// AccessException - 如果确定是否可以读取属性有任何问题
	boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * Called to read a property from a specified target object.
	 * Should only succeed if {@link #canRead} also returns {@code true}.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the property is being accessed
	 * @param name the name of the property being accessed
	 * @return a TypedValue object wrapping the property value read and a type descriptor for it
	 * @throws AccessException if there is any problem accessing the property value
	 */
	// 调用以从指定的目标对象读取属性。 只有 在canRead 也返回true时才应该成功。
	TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * Called to determine if a resolver instance is able to write to a specified
	 * property on a specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the property is being accessed
	 * @param name the name of the property being accessed
	 * @return true if this resolver is able to write to the property
	 * @throws AccessException if there is any problem determining whether the
	 * property can be written to
	 */
	// 调用以确定解析器实例是否能够写入指定目标对象上的指定属性
	boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * Called to write to a property on a specified target object.
	 * Should only succeed if {@link #canWrite} also returns {@code true}.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the property is being accessed
	 * @param name the name of the property being accessed
	 * @param newValue the new value for the property
	 * @throws AccessException if there is any problem writing to the property value
	 */
	// 调用以写入指定目标对象上的属性。 只有在 canWrite 也返回true时才应该成功
	void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException;

}
