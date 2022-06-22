/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation;

import java.util.List;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * Stores and exposes information about data-binding and validation
 * errors for a specific object.
 *
 * <p>Field names can be properties of the target object (e.g. "name"
 * when binding to a customer object), or nested fields in case of
 * subobjects (e.g. "address.street"). Supports subtree navigation
 * via {@link #setNestedPath(String)}: for example, an
 * {@code AddressValidator} validates "address", not being aware
 * that this is a subobject of customer.
 *
 * <p>Note: {@code Errors} objects are single-threaded.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
 */
// 存储和公开有关特定对象的数据绑定和验证错误的信息
// 字段名称可以是目标对象的属性（例如绑定到客户对象时的“名称”），或者子对象的嵌套字段（例如“address.street”）。
// 通过 setNestedPath(String) 支持子树导航：例如，AddressValidator 验证“地址”，不知道这是客户的子对象。
// 注意：错误对象是单线程的
// 两个职责：1.组织我们的错误 2.存储我们的错误
// 错误收集器：通常来讲是一个集合，收集错误，并根据相关的国际化文案，存储文案 {@link org.springframework.validation.ValidationUtils}
public interface Errors {

	/**
	 * The separator between path elements in a nested path,
	 * for example in "customer.name" or "customer.address.street".
	 * <p>"." = same as the
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR nested property separator}
	 * in the beans package.
	 */
	// 嵌套路径中路径元素之间的分隔符，例如在“customer.name”或“customer.address.street”中。
	// "." = 与 beans 包中的嵌套属性分隔符相同
	String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;


	/**
	 * Return the name of the bound root object.
	 */
	// 返回绑定的根对象的名称。
	String getObjectName();

	/**
	 * Allow context to be changed so that standard validators can validate
	 * subtrees. Reject calls prepend the given path to the field names.
	 * <p>For example, an address validator could validate the subobject
	 * "address" of a customer object.
	 * @param nestedPath nested path within this object,
	 * e.g. "address" (defaults to "", {@code null} is also acceptable).
	 * Can end with a dot: both "address" and "address." are valid.
	 */
	// 允许更改上下文，以便标准验证器可以验证子树。 拒绝调用将给定的路径添加到字段名称之前。
	// 例如，地址验证器可以验证客户对象的子对象“地址”。
	// 形参：NestedPath – 此对象中的嵌套路径，例如"address"（默认为""， null也是可以接受的）。 可以以
	// 		点结尾："address" and "address."。 是有效的。
	void setNestedPath(String nestedPath);

	/**
	 * Return the current nested path of this {@link Errors} object.
	 * <p>Returns a nested path with a dot, i.e. "address.", for easy
	 * building of concatenated paths. Default is an empty String.
	 */
	// 返回此 Errors 对象的当前嵌套路径
	// 返回带有点的嵌套路径，即"address."，以便于构建连接路径。默认为空字符串。
	String getNestedPath();

	/**
	 * Push the given sub path onto the nested path stack.
	 * <p>A {@link #popNestedPath()} call will reset the original
	 * nested path before the corresponding
	 * {@code pushNestedPath(String)} call.
	 * <p>Using the nested path stack allows to set temporary nested paths
	 * for subobjects without having to worry about a temporary path holder.
	 * <p>For example: current path "spouse.", pushNestedPath("child") &rarr;
	 * result path "spouse.child."; popNestedPath() &rarr; "spouse." again.
	 * @param subPath the sub path to push onto the nested path stack
	 * @see #popNestedPath
	 */
	// 将给定的子路径推送到嵌套路径堆栈上
	// popNestedPath() 调用将在相应的 pushNestedPath(String) 调用之前重置原始嵌套路径。
	// 使用嵌套路径堆栈允许为子对象设置临时嵌套路径，而不必担心临时路径持有者。
	// 例如：当前路径"spouse.", pushNestedPath("child") → 结果路径"spouse.child."; popNestedPath() → "spouse." 再次。
	void pushNestedPath(String subPath);

	/**
	 * Pop the former nested path from the nested path stack.
	 * @throws IllegalStateException if there is no former nested path on the stack
	 * @see #pushNestedPath
	 */
	// 从嵌套路径堆栈中弹出前一个嵌套路径。
	void popNestedPath() throws IllegalStateException;

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 */
	// 使用给定的错误描述为整个目标对象注册一个全局错误
	void reject(String errorCode);

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param defaultMessage fallback default message
	 */
	// 使用给定的错误描述为整个目标对象注册一个全局错误。
	void reject(String errorCode, String defaultMessage);

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 */
	// 使用给定的错误描述为整个目标对象注册一个全局错误。
	void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @see #getNestedPath()
	 */
	// 使用给定的错误描述为当前对象的指定字段注册字段错误（考虑当前嵌套路径，如果有）
	// <p>字段名称可以是 {@code null} 或空字符串以指示当前对象本身而不是它的一个字段。
	// 如果当前对象是顶级对象，这可能会导致嵌套对象图中的相应字段错误或全局错误。
	void rejectValue(@Nullable String field, String errorCode);

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @param defaultMessage fallback default message
	 * @see #getNestedPath()
	 */
	// 使用给定的错误描述为当前对象的指定字段注册字段错误（考虑当前嵌套路径，如果有）。
	// 字段名称可以为 null 或空 String 以指示当前对象本身而不是它的一个字段。如果当前对象是顶级对象，
	// 这可能会导致嵌套对象图中的相应字段错误或全局错误。
	void rejectValue(@Nullable String field, String errorCode, String defaultMessage);

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 * @see #getNestedPath()
	 */
	// 使用给定的错误描述为当前对象的指定字段注册字段错误（考虑当前嵌套路径，如果有）。
	// 字段名称可以为 null 或空 String 以指示当前对象本身而不是它的一个字段。如果当前对象是顶级对象，
	// 这可能会导致嵌套对象图中的相应字段错误或全局错误。
	void rejectValue(@Nullable String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * Add all errors from the given {@code Errors} instance to this
	 * {@code Errors} instance.
	 * <p>This is a convenience method to avoid repeated {@code reject(..)}
	 * calls for merging an {@code Errors} instance into another
	 * {@code Errors} instance.
	 * <p>Note that the passed-in {@code Errors} instance is supposed
	 * to refer to the same target object, or at least contain compatible errors
	 * that apply to the target object of this {@code Errors} instance.
	 * @param errors the {@code Errors} instance to merge in
	 */
	// 将给定 Errors 实例中的所有错误添加到此 Errors 实例。
	// 这是一种方便的方法，可避免重复拒绝（..）调用以将 Errors 实例合并到另一个 Errors 实例中。
	// 请注意，传入的 Errors 实例应该引用相同的目标对象，或者至少包含适用于此 Errors 实例的目标对象的兼容错误。
	void addAllErrors(Errors errors);

	/**
	 * Return if there were any errors.
	 */
	// 如果有任何错误返回
	boolean hasErrors();

	/**
	 * Return the total number of errors.
	 */
	// 返回错误总数
	int getErrorCount();

	/**
	 * Get all errors, both global and field ones.
	 * @return a list of {@link ObjectError} instances
	 */
	// 获取所有错误，包括全局错误和 Field 错误
	List<ObjectError> getAllErrors();

	/**
	 * Are there any global errors?
	 * @return {@code true} if there are any global errors
	 * @see #hasFieldErrors()
	 */
	// 是否有任何全局错误？
	boolean hasGlobalErrors();

	/**
	 * Return the number of global errors.
	 * @return the number of global errors
	 * @see #getFieldErrorCount()
	 */
	// 返回全局错误的数量。
	int getGlobalErrorCount();

	/**
	 * Get all global errors.
	 * @return a list of {@link ObjectError} instances
	 */
	// 获取所有全局错误
	List<ObjectError> getGlobalErrors();

	/**
	 * Get the <i>first</i> global error, if any.
	 * @return the global error, or {@code null}
	 */
	// 获取第一个全局错误（如果有
	@Nullable
	ObjectError getGlobalError();

	/**
	 * Are there any field errors?
	 * @return {@code true} if there are any errors associated with a field
	 * @see #hasGlobalErrors()
	 */
	// 是否有任何字段错误？
	boolean hasFieldErrors();

	/**
	 * Return the number of errors associated with a field.
	 * @return the number of errors associated with a field
	 * @see #getGlobalErrorCount()
	 */
	// 返回与字段关联的错误数
	int getFieldErrorCount();

	/**
	 * Get all errors associated with a field.
	 * @return a List of {@link FieldError} instances
	 */
	// 获取与字段相关的所有错误
	List<FieldError> getFieldErrors();

	/**
	 * Get the <i>first</i> error associated with a field, if any.
	 * @return the field-specific error, or {@code null}
	 */
	// 获取与字段关联的第一个错误（如果有）。
	@Nullable
	FieldError getFieldError();

	/**
	 * Are there any errors associated with the given field?
	 * @param field the field name
	 * @return {@code true} if there were any errors associated with the given field
	 */
	// 是否存在与给定字段相关的任何错误？
	boolean hasFieldErrors(String field);

	/**
	 * Return the number of errors associated with the given field.
	 * @param field the field name
	 * @return the number of errors associated with the given field
	 */
	// 返回与给定字段关联的错误数。
	int getFieldErrorCount(String field);

	/**
	 * Get all errors associated with the given field.
	 * <p>Implementations should support not only full field names like
	 * "name" but also pattern matches like "na*" or "address.*".
	 * @param field the field name
	 * @return a List of {@link FieldError} instances
	 */
	// 获取与给定字段相关的所有错误。实现不仅应该支持像“name”这样的完整字段名称，还应该支持像“na”或“address.”这样的模式匹配。
	List<FieldError> getFieldErrors(String field);

	/**
	 * Get the first error associated with the given field, if any.
	 * @param field the field name
	 * @return the field-specific error, or {@code null}
	 */
	// 获取与给定字段关联的第一个错误（如果有）。
	@Nullable
	FieldError getFieldError(String field);

	/**
	 * Return the current value of the given field, either the current
	 * bean property value or a rejected update from the last binding.
	 * <p>Allows for convenient access to user-specified field values,
	 * even if there were type mismatches.
	 * @param field the field name
	 * @return the current value of the given field
	 */
	// 返回给定字段的当前值，可以是当前的 bean 属性值，也可以是上次绑定被拒绝的更新。
	// 允许方便地访问用户指定的字段值，即使存在类型不匹配的情况。
	@Nullable
	Object getFieldValue(String field);

	/**
	 * Return the type of a given field.
	 * <p>Implementations should be able to determine the type even
	 * when the field value is {@code null}, for example from some
	 * associated descriptor.
	 * @param field the field name
	 * @return the type of the field, or {@code null} if not determinable
	 */
	// 返回给定字段的类型。
	// 即使字段值为空，实现也应该能够确定类型，例如从一些关联的描述符。
	@Nullable
	Class<?> getFieldType(String field);

}
