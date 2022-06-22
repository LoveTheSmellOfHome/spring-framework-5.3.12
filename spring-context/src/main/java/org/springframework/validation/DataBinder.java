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

package org.springframework.validation;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormatterPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Binder that allows for setting property values onto a target object,
 * including support for validation and binding result analysis.
 * The binding process can be customized through specifying allowed fields,
 * required fields, custom editors, etc.
 *
 * <p>Note that there are potential security implications in failing to set an array
 * of allowed fields. In the case of HTTP form POST data for example, malicious clients
 * can attempt to subvert an application by supplying values for fields or properties
 * that do not exist on the form. In some cases this could lead to illegal data being
 * set on command objects <i>or their nested objects</i>. For this reason, it is
 * <b>highly recommended to specify the {@link #setAllowedFields allowedFields} property</b>
 * on the DataBinder.
 *
 * <p>The binding results can be examined via the {@link BindingResult} interface,
 * extending the {@link Errors} interface: see the {@link #getBindingResult()} method.
 * Missing fields and property access exceptions will be converted to {@link FieldError FieldErrors},
 * collected in the Errors instance, using the following error codes:
 *
 * <ul>
 * <li>Missing field error: "required"
 * <li>Type mismatch error: "typeMismatch"
 * <li>Method invocation error: "methodInvocation"
 * </ul>
 *
 * <p>By default, binding errors get resolved through the {@link BindingErrorProcessor}
 * strategy, processing for missing fields and property access exceptions: see the
 * {@link #setBindingErrorProcessor} method. You can override the default strategy
 * if needed, for example to generate different error codes.
 *
 * <p>Custom validation errors can be added afterwards. You will typically want to resolve
 * such error codes into proper user-visible error messages; this can be achieved through
 * resolving each error via a {@link org.springframework.context.MessageSource}, which is
 * able to resolve an {@link ObjectError}/{@link FieldError} through its
 * {@link org.springframework.context.MessageSource#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)}
 * method. The list of message codes can be customized through the {@link MessageCodesResolver}
 * strategy: see the {@link #setMessageCodesResolver} method. {@link DefaultMessageCodesResolver}'s
 * javadoc states details on the default resolution rules.
 *
 * <p>This generic data binder can be used in any kind of environment.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #registerCustomEditor
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 * @see org.springframework.context.MessageSource
 */
// 允许将属性值设置到目标对象上的绑定器，包括支持验证和绑定结果分析。可以通过指定允许字段、必填字段、自定义编辑器等来自定义绑定过程
//
// <p>请注意，未能设置一组允许的字段会带来潜在的安全隐患。例如，在 HTTP 表单 POST 数据的情况下，恶意客户端可以尝试通过为表单上不存在
// 的字段或属性提供值来破坏应用程序。在某些情况下，这可能会导致在命令对象<i>或其嵌套对象<i>上设置非法数据。
// 为此，<b>强烈建议在 DataBinder 上指定 {@link setAllowedFields allowedFields} 属性<b>。
//
// <p>可以通过 {@link BindingResult} 接口检查绑定结果，扩展 {@link Errors} 接口：参见 {@link getBindingResult()} 方法。
// 缺少字段和属性访问异常将转换为 {@link FieldError FieldErrors}，收集在 Errors 实例中，使用以下错误代码：
//
// <p>默认情况下，绑定错误通过 {@link BindingErrorProcessor} 策略解决，处理丢失的字段和属性访问异常：请参阅
// {@link setBindingErrorProcessor} 方法。如果需要，您可以覆盖默认策略，例如生成不同的错误代码
//
// <p>自定义验证错误可以在之后添加。您通常希望将此类错误代码解析为正确的用户可见错误消息；这可以通过通过
// {@link org.springframework.context.MessageSource} 解决每个错误来实现，它能够通过其
// {@link org.springframework.context.MessageSourcegetMessage} 解决 {@link ObjectError}/{@link FieldError}
// (org.springframework.context.MessageSourceResolvable, java.util.Locale)} 方法。可以通过
// {@link MessageCodesResolver} 策略自定义消息代码列表：参见 {@link setMessageCodesResolver} 方法。
// {@link DefaultMessageCodesResolver} 的 javadoc 详细说明了默认解析规则。
//
// <p>这种通用数据绑定器可用于任何类型的环境
// Spring 标准数据绑定组件，将 PropertyValues 绑定到某个对象上去，
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

	/** Default object name used for binding: "target". */
	// 用于绑定的默认对象名称："target"
	public static final String DEFAULT_OBJECT_NAME = "target";

	/** Default limit for array and collection growing: 256. */
	// 数组和集合增长的默认限制：256
	public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;


	/**
	 * We'll create a lot of DataBinder instances: Let's use a static logger.
	 */
	// 我们将创建很多 DataBinder 实例：让我们使用静态日志
	protected static final Log logger = LogFactory.getLog(DataBinder.class);

	@Nullable
	private final Object target;

	private final String objectName;

	@Nullable
	private AbstractPropertyBindingResult bindingResult;

	private boolean directFieldAccess = false;

	@Nullable
	private SimpleTypeConverter typeConverter;

	// 是否忽略未知属性，默认为 true,如果存在未知属性也不报错，直接忽略 {@link DataBinderDemo} source.put("age", "18");
	private boolean ignoreUnknownFields = true;

	// 是否忽略掉非法字段，默认为 false
	private boolean ignoreInvalidFields = false;

	// 是否自动增加嵌套路径，比如 user 中有个 Company 属性，我们在设置值时候可以直接将嵌套对象的属性名设值
	// source.put("company.name", "gupao");
	private boolean autoGrowNestedPaths = true;

	private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

	// 绑定字段白名单
	@Nullable
	private String[] allowedFields;

	// 绑定字段黑名单
	@Nullable
	private String[] disallowedFields;

	// 必须绑定字段
	@Nullable
	private String[] requiredFields;

	@Nullable
	private ConversionService conversionService;

	@Nullable
	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

	private final List<Validator> validators = new ArrayList<>();


	/**
	 * Create a new DataBinder instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	// 使用默认对象名称创建一个新的 DataBinder 实例
	// @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
	public DataBinder(@Nullable Object target) {
		this(target, DEFAULT_OBJECT_NAME);
	}

	/**
	 * Create a new DataBinder instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	// 创建一个新的 DataBinder 实例
	// @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
	// @param objectName 目标对象的名称
	public DataBinder(@Nullable Object target, String objectName) {
		this.target = ObjectUtils.unwrapOptional(target);
		this.objectName = objectName;
	}


	/**
	 * Return the wrapped target object.
	 */
	@Nullable
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Return the name of the bound object.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Set whether this binder should attempt to "auto-grow" a nested path that contains a null value.
	 * <p>If "true", a null path location will be populated with a default object value and traversed
	 * instead of resulting in an exception. This flag also enables auto-growth of collection elements
	 * when accessing an out-of-bounds index.
	 * <p>Default is "true" on a standard DataBinder. Note that since Spring 4.1 this feature is supported
	 * for bean property access (DataBinder's default mode) and field access.
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowNestedPaths
	 */
	// 设置此绑定器是否应尝试“自动增长”包含空值的嵌套路径。
	// <p>如果为“true”，则将使用默认对象值填充空路径位置并进行遍历，而不是导致异常。当访问越界索引时，此标志还启用集合元素的自动增长。
	// <p>标准 DataBinder 上的默认值为“true”。请注意，从 Spring 4.1 开始，此功能支持 bean 属性访问（DataBinder 的默认模式）和字段访问。
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 */
	// 返回是否已激活嵌套路径的“自动增长”
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * Specify the limit for array and collection auto-growing.
	 * <p>Default is 256, preventing OutOfMemoryErrors in case of large indexes.
	 * Raise this limit if your auto-growing needs are unusually high.
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowCollectionLimit
	 */
	// 指定数组和集合自动增长的限制。
	// <p>默认值为 256，防止在大索引的情况下出现 OutOfMemoryErrors。如果您的自动增长需求异常高，请提高此限制。
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * Return the current limit for array and collection auto-growing.
	 */
	// 返回数组和集合自动增长的当前限制
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * Initialize standard JavaBean property access for this DataBinder.
	 * <p>This is the default; an explicit call just leads to eager initialization.
	 * @see #initDirectFieldAccess()
	 * @see #createBeanPropertyBindingResult()
	 */
	// 初始化此 DataBinder 的标准 JavaBean 属性访问。
	// <p>这是默认设置；显式调用只会导致急切的初始化。
	public void initBeanPropertyAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
		this.directFieldAccess = false;
	}

	/**
	 * Create the {@link AbstractPropertyBindingResult} instance using standard
	 * JavaBean property access.
	 * @since 4.2.1
	 */
	// 使用标准 JavaBean 属性访问创建 {@link AbstractPropertyBindingResult} 实例。
	protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
		// 创建一个 BeanPropertyBindingResult
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		// 返回 BeanPropertyBindingResult
		return result;
	}

	/**
	 * Initialize direct field access for this DataBinder,
	 * as alternative to the default bean property access.
	 * @see #initBeanPropertyAccess()
	 * @see #createDirectFieldBindingResult()
	 */
	// 初始化此 DataBinder 的直接字段访问，作为默认 bean 属性访问的替代方法。
	public void initDirectFieldAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
		this.directFieldAccess = true;
	}

	/**
	 * Create the {@link AbstractPropertyBindingResult} instance using direct
	 * field access.
	 * @since 4.2.1
	 */
	// 使用直接字段访问创建 {@link AbstractPropertyBindingResult} 实例。
	protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
		DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * Return the internal BindingResult held by this DataBinder,
	 * as an AbstractPropertyBindingResult.
	 */
	// 返回此 DataBinder 持有的内部 BindingResult，作为 AbstractPropertyBindingResult。
	protected AbstractPropertyBindingResult getInternalBindingResult() {
		if (this.bindingResult == null) {
			this.bindingResult = (this.directFieldAccess ?
					createDirectFieldBindingResult(): createBeanPropertyBindingResult());
		}
		// bind 方法生成 BeanPropertyBindingResult
		return this.bindingResult;
	}

	/**
	 * Return the underlying PropertyAccessor of this binder's BindingResult.
	 */
	// 返回此绑定器的 BindingResult 的底层 PropertyAccessor
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		// 前边返回 BeanPropertyBindingResult 对象
		return getInternalBindingResult().getPropertyAccessor();
	}

	/**
	 * Return this binder's underlying SimpleTypeConverter.
	 */
	// 返回此绑定器的底层 SimpleTypeConverter。
	protected SimpleTypeConverter getSimpleTypeConverter() {
		if (this.typeConverter == null) {
			this.typeConverter = new SimpleTypeConverter();
			if (this.conversionService != null) {
				this.typeConverter.setConversionService(this.conversionService);
			}
		}
		return this.typeConverter;
	}

	/**
	 * Return the underlying TypeConverter of this binder's BindingResult.
	 */
	// 返回此绑定器的 BindingResult 的基础 TypeConverter。
	protected PropertyEditorRegistry getPropertyEditorRegistry() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * Return the underlying TypeConverter of this binder's BindingResult.
	 */
	// 返回此绑定器的 BindingResult 的基础 TypeConverter。
	protected TypeConverter getTypeConverter() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * Return the BindingResult instance created by this DataBinder.
	 * This allows for convenient access to the binding results after
	 * a bind operation.
	 * @return the BindingResult instance, to be treated as BindingResult
	 * or as Errors instance (Errors is a super-interface of BindingResult)
	 * @see Errors
	 * @see #bind
	 */
	// 返回由此 DataBinder 创建的 BindingResult 实例。这允许在绑定操作后方便地访问绑定结果
	public BindingResult getBindingResult() {
		return getInternalBindingResult();
	}


	/**
	 * Set whether to ignore unknown fields, that is, whether to ignore bind
	 * parameters that do not have corresponding fields in the target object.
	 * <p>Default is "true". Turn this off to enforce that all bind parameters
	 * must have a matching field in the target object.
	 * <p>Note that this setting only applies to <i>binding</i> operations
	 * on this DataBinder, not to <i>retrieving</i> values via its
	 * {@link #getBindingResult() BindingResult}.
	 * @see #bind
	 */
	// 设置是否忽略未知字段，即是否忽略目标对象中没有对应字段的绑定参数。
	// <p>默认为"true"。关闭此选项以强制所有绑定参数必须在目标对象中具有匹配字段。
	// <p>请注意，此设置仅适用于此 DataBinder 上的 <i>binding<i> 操作，不适用于 <i>通过其检索<i> 值
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * Return whether to ignore unknown fields when binding.
	 */
	public boolean isIgnoreUnknownFields() {
		return this.ignoreUnknownFields;
	}

	/**
	 * Set whether to ignore invalid fields, that is, whether to ignore bind
	 * parameters that have corresponding fields in the target object which are
	 * not accessible (for example because of null values in the nested path).
	 * <p>Default is "false". Turn this on to ignore bind parameters for
	 * nested objects in non-existing parts of the target object graph.
	 * <p>Note that this setting only applies to <i>binding</i> operations
	 * on this DataBinder, not to <i>retrieving</i> values via its
	 * {@link #getBindingResult() BindingResult}.
	 * @see #bind
	 */
	// 设置是否忽略无效字段，即是否忽略目标对象中对应字段不可访问的绑定参数（例如由于嵌套路径中的空值）。
	// <p>默认为"false"。打开此选项可忽略目标对象图不存在部分中嵌套对象的绑定参数。
	// <p>请注意，此设置仅适用于此 DataBinder 上的 <i>binding<i> 操作，不适用于 <i>通过其检索<i> 值
	public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
		this.ignoreInvalidFields = ignoreInvalidFields;
	}

	/**
	 * Return whether to ignore invalid fields when binding.
	 */
	// 返回绑定时是否忽略无效字段
	public boolean isIgnoreInvalidFields() {
		return this.ignoreInvalidFields;
	}

	/**
	 * Register fields that should be allowed for binding. Default is all
	 * fields. Restrict this for example to avoid unwanted modifications
	 * by malicious users when binding HTTP request parameters.
	 * <p>Supports "xxx*", "*xxx" and "*xxx*" patterns. More sophisticated matching
	 * can be implemented by overriding the {@code isAllowed} method.
	 * <p>Alternatively, specify a list of <i>disallowed</i> fields.
	 * @param allowedFields array of field names
	 * @see #setDisallowedFields
	 * @see #isAllowed(String)
	 */
	// 注册应该允许绑定的字段。默认为所有字段。例如，限制此项以避免在绑定 HTTP 请求参数时被恶意用户进行不必要的修改
	// <p>支持“xxx*”、“*xxx”和“*xxx*”模式。通过覆盖 {@code isAllowed} 方法可以实现更复杂的匹配。
	public void setAllowedFields(@Nullable String... allowedFields) {
		this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
	}

	/**
	 * Return the fields that should be allowed for binding.
	 * @return array of field names
	 */
	// 返回应该允许绑定的字段
	@Nullable
	public String[] getAllowedFields() {
		return this.allowedFields;
	}

	/**
	 * Register fields that should <i>not</i> be allowed for binding. Default is none.
	 * Mark fields as disallowed for example to avoid unwanted modifications
	 * by malicious users when binding HTTP request parameters.
	 * <p>Supports "xxx*", "*xxx" and "*xxx*" patterns. More sophisticated matching
	 * can be implemented by overriding the {@code isAllowed} method.
	 * <p>Alternatively, specify a list of <i>allowed</i> fields.
	 * @param disallowedFields array of field names
	 * @see #setAllowedFields
	 * @see #isAllowed(String)
	 */
	// 注册应该<i>不允许<i>绑定的字段。默认为无。例如，将字段标记为不允许的，以避免在绑定 HTTP 请求参数时恶意用户进行不必要的修改。
	// <p>支持“xxx*”、“*xxx”和“*xxx*”模式。通过覆盖 {@code isAllowed} 方法可以实现更复杂的匹配。
	// <p>或者，指定一个 <i>允许的<i> 字段列表
	public void setDisallowedFields(@Nullable String... disallowedFields) {
		this.disallowedFields = PropertyAccessorUtils.canonicalPropertyNames(disallowedFields);
	}

	/**
	 * Return the fields that should <i>not</i> be allowed for binding.
	 * @return array of field names
	 */
	// 返回应该 <i>not<i> 允许绑定的字段
	@Nullable
	public String[] getDisallowedFields() {
		return this.disallowedFields;
	}

	/**
	 * Register fields that are required for each binding process.
	 * <p>If one of the specified fields is not contained in the list of
	 * incoming property values, a corresponding "missing field" error
	 * will be created, with error code "required" (by the default
	 * binding error processor).
	 * @param requiredFields array of field names
	 * @see #setBindingErrorProcessor
	 * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
	 */
	// 注册每个绑定过程所需的字段。
	// <p>如果传入属性值列表中不包含指定字段之一，则会创建相应的“缺少字段”错误，错误代码为“required”（由默认绑定错误处理器）。
	public void setRequiredFields(@Nullable String... requiredFields) {
		this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
		if (logger.isDebugEnabled()) {
			logger.debug("DataBinder requires binding of required fields [" +
					StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
		}
	}

	/**
	 * Return the fields that are required for each binding process.
	 * @return array of field names
	 */
	// 返回每个绑定过程所需的字段
	@Nullable
	public String[] getRequiredFields() {
		return this.requiredFields;
	}

	/**
	 * Set the strategy to use for resolving errors into message codes.
	 * Applies the given strategy to the underlying errors holder.
	 * <p>Default is a DefaultMessageCodesResolver.
	 * @see BeanPropertyBindingResult#setMessageCodesResolver
	 * @see DefaultMessageCodesResolver
	 */
	// 设置用于将错误解析为消息代码的策略。将给定的策略应用于基础错误持有者。
	// <p>默认是一个 DefaultMessageCodesResolver。
	public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
		this.messageCodesResolver = messageCodesResolver;
		if (this.bindingResult != null && messageCodesResolver != null) {
			this.bindingResult.setMessageCodesResolver(messageCodesResolver);
		}
	}

	/**
	 * Set the strategy to use for processing binding errors, that is,
	 * required field errors and {@code PropertyAccessException}s.
	 * <p>Default is a DefaultBindingErrorProcessor.
	 * @see DefaultBindingErrorProcessor
	 */
	// 设置用于处理绑定错误的策略，即必填字段错误和 {@code PropertyAccessException}。
	// <p>默认是一个 DefaultBindingErrorProcessor。
	public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * Return the strategy for processing binding errors.
	 */
	// 返回处理绑定错误的策略
	public BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * Set the Validator to apply after each binding step.
	 * @see #addValidators(Validator...)
	 * @see #replaceValidators(Validator...)
	 */
	// 将验证器设置为在每个绑定步骤后应用
	public void setValidator(@Nullable Validator validator) {
		assertValidators(validator);
		this.validators.clear();
		if (validator != null) {
			this.validators.add(validator);
		}
	}

	private void assertValidators(Validator... validators) {
		Object target = getTarget();
		for (Validator validator : validators) {
			if (validator != null && (target != null && !validator.supports(target.getClass()))) {
				throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
			}
		}
	}

	/**
	 * Add Validators to apply after each binding step.
	 * @see #setValidator(Validator)
	 * @see #replaceValidators(Validator...)
	 */
	// 添加验证器以在每个绑定步骤之后应用。
	public void addValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * Replace the Validators to apply after each binding step.
	 * @see #setValidator(Validator)
	 * @see #addValidators(Validator...)
	 */
	// 在每个绑定步骤之后替换要应用的验证器。
	public void replaceValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.clear();
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * Return the primary Validator to apply after each binding step, if any.
	 */
	// 在每个绑定步骤之后返回要应用的主要验证器（如果有）
	@Nullable
	public Validator getValidator() {
		return (!this.validators.isEmpty() ? this.validators.get(0) : null);
	}

	/**
	 * Return the Validators to apply after data binding.
	 */
	// 返回数据绑定后要应用的验证器
	public List<Validator> getValidators() {
		return Collections.unmodifiableList(this.validators);
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyEditorRegistry/TypeConverter interface
	// PropertyEditorRegistry/TypeConverter 接口的实现
	//---------------------------------------------------------------------

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	// 指定用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代。
	public void setConversionService(@Nullable ConversionService conversionService) {
		Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
		this.conversionService = conversionService;
		if (this.bindingResult != null && conversionService != null) {
			this.bindingResult.initConversion(conversionService);
		}
	}

	/**
	 * Return the associated ConversionService, if any.
	 */
	// 返回关联的 ConversionService（如果有）
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Add a custom formatter, applying it to all fields matching the
	 * {@link Formatter}-declared type.
	 * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
	 * @param formatter the formatter to add, generically declared for a specific type
	 * @since 4.2
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	// 添加自定义格式化程序，将其应用于与 {@link Formatter} 声明类型匹配的所有字段。
	// <p>在封面下注册相应的 {@link PropertyEditor} 适配器。
	public void addCustomFormatter(Formatter<?> formatter) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
	}

	/**
	 * Add a custom formatter for the field type specified in {@link Formatter} class,
	 * applying it to the specified fields only, if any, or otherwise to all fields.
	 * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
	 * @param formatter the formatter to add, generically declared for a specific type
	 * @param fields the fields to apply the formatter to, or none if to be applied to all
	 * @since 4.2
	 * @see #registerCustomEditor(Class, String, PropertyEditor)
	 */
	// 为 {@link Formatter} 类中指定的字段类型添加自定义格式化程序，仅将其应用于指定的字段（如果有），否则应用于所有字段。
	// <p>在封面下注册相应的 {@link PropertyEditor} 适配器
	public void addCustomFormatter(Formatter<?> formatter, String... fields) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		Class<?> fieldType = adapter.getFieldType();
		if (ObjectUtils.isEmpty(fields)) {
			getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
		}
		else {
			for (String field : fields) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
			}
		}
	}

	/**
	 * Add a custom formatter, applying it to the specified field types only, if any,
	 * or otherwise to all fields matching the {@link Formatter}-declared type.
	 * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
	 * @param formatter the formatter to add (does not need to generically declare a
	 * field type if field types are explicitly specified as parameters)
	 * @param fieldTypes the field types to apply the formatter to, or none if to be
	 * derived from the given {@link Formatter} implementation class
	 * @since 4.2
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	// 添加自定义格式化程序，仅将其应用于指定的字段类型（如果有），或以其他方式应用于与 {@link Formatter} 声明的类型匹配的所有字段。
	// <p>在封面下注册相应的 {@link PropertyEditor} 适配器。
	public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		if (ObjectUtils.isEmpty(fieldTypes)) {
			getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
		}
		else {
			for (Class<?> fieldType : fieldTypes) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
			}
		}
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
	}

	@Override
	public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
	}

	@Override
	@Nullable
	public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
		return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
		return getTypeConverter().convertIfNecessary(value, requiredType);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, field);
	}

	@Nullable
	@Override
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
	}


	/**
	 * Bind the given property values to this binder's target.
	 * <p>This call can create field errors, representing basic binding
	 * errors like a required field (code "required"), or type mismatch
	 * between value and bean property (code "typeMismatch").
	 * <p>Note that the given PropertyValues should be a throwaway instance:
	 * For efficiency, it will be modified to just contain allowed fields if it
	 * implements the MutablePropertyValues interface; else, an internal mutable
	 * copy will be created for this purpose. Pass in a copy of the PropertyValues
	 * if you want your original instance to stay unmodified in any case.
	 * @param pvs property values to bind
	 * @see #doBind(org.springframework.beans.MutablePropertyValues)
	 */
	// 将给定的属性值绑定到此绑定器的目标。
	// <p>这个调用会产生字段错误，代表基本的绑定错误，比如必填字段（代码“required”），或者值和 bean 属性之间的类型
	// 不匹配（代码“typeMismatch”）。
	// <p>注意，给定的 PropertyValues 应该是一个一次性实例：为了效率，如果它实现了 MutablePropertyValues 接口，它将被修改为
	// 只包含允许的字段；否则，将为此创建一个内部可变副本。如果您希望原始实例在任何情况下都保持不变，请传入 PropertyValues 的副本。
	public void bind(PropertyValues pvs) {
		MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ?
				(MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
		// 绑定属性值
		doBind(mpvs);
	}

	/**
	 * Actual implementation of the binding process, working with the
	 * passed-in MutablePropertyValues instance.
	 * @param mpvs the property values to bind,
	 * as MutablePropertyValues instance
	 * @see #checkAllowedFields
	 * @see #checkRequiredFields
	 * @see #applyPropertyValues
	 */
	// 绑定过程的实际实现，使用传入的 MutablePropertyValues 实例
	// @param mpvs 要绑定的属性值，作为 MutablePropertyValues 实例
	protected void doBind(MutablePropertyValues mpvs) {
		checkAllowedFields(mpvs);
		checkRequiredFields(mpvs);
		// {@link AbstractAutowireCapableBeanFactory}#populateBean##
		// applyPropertyValues(beanName, mbd, bw, pvs); 二者实现过程一致
		applyPropertyValues(mpvs);
	}

	/**
	 * Check the given property values against the allowed fields,
	 * removing values for fields that are not allowed.
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getAllowedFields
	 * @see #isAllowed(String)
	 */
	// 根据允许的字段检查给定的属性值，删除不允许的字段的值。
	// @param mpvs 要绑定的属性值（可以修改）
	protected void checkAllowedFields(MutablePropertyValues mpvs) {
		PropertyValue[] pvs = mpvs.getPropertyValues();
		for (PropertyValue pv : pvs) {
			String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
			if (!isAllowed(field)) {
				mpvs.removePropertyValue(pv);
				getBindingResult().recordSuppressedField(field);
				if (logger.isDebugEnabled()) {
					logger.debug("Field [" + field + "] has been removed from PropertyValues " +
							"and will not be bound, because it has not been found in the list of allowed fields");
				}
			}
		}
	}

	/**
	 * Return if the given field is allowed for binding.
	 * Invoked for each passed-in property value.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
	 * as well as direct equality, in the specified lists of allowed fields and
	 * disallowed fields. A field matching a disallowed pattern will not be accepted
	 * even if it also happens to match a pattern in the allowed list.
	 * <p>Can be overridden in subclasses.
	 * @param field the field to check
	 * @return if the field is allowed
	 * @see #setAllowedFields
	 * @see #setDisallowedFields
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	// 如果给定的字段允许绑定，则返回。为每个传入的属性值调用。
	// <p>默认实现会检查指定的允许字段和不允许字段列表中的“xxx*”、“*xxx”和“*xxx*”匹配以及直接相等。
	// 匹配不允许模式的字段将不被接受，即使它也恰好与允许列表中的模式匹配。
	// <p>可以在子类中覆盖。
	// @param field 要检查的字段
	// @return 如果该字段被允许
	protected boolean isAllowed(String field) {
		String[] allowed = getAllowedFields();
		String[] disallowed = getDisallowedFields();
		return ((ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) &&
				(ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field)));
	}

	/**
	 * Check the given property values against the required fields,
	 * generating missing field errors where appropriate.
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getRequiredFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processMissingFieldError
	 */
	// 根据所需字段检查给定的属性值，在适当的情况下生成缺失字段错误。
	// @param mpvs 要绑定的属性值（可以修改）
	protected void checkRequiredFields(MutablePropertyValues mpvs) {
		String[] requiredFields = getRequiredFields();
		if (!ObjectUtils.isEmpty(requiredFields)) {
			Map<String, PropertyValue> propertyValues = new HashMap<>();
			PropertyValue[] pvs = mpvs.getPropertyValues();
			for (PropertyValue pv : pvs) {
				String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
				propertyValues.put(canonicalName, pv);
			}
			for (String field : requiredFields) {
				PropertyValue pv = propertyValues.get(field);
				boolean empty = (pv == null || pv.getValue() == null);
				if (!empty) {
					if (pv.getValue() instanceof String) {
						empty = !StringUtils.hasText((String) pv.getValue());
					}
					else if (pv.getValue() instanceof String[]) {
						String[] values = (String[]) pv.getValue();
						empty = (values.length == 0 || !StringUtils.hasText(values[0]));
					}
				}
				if (empty) {
					// Use bind error processor to create FieldError.
					// 使用绑定错误处理器创建 FieldError
					getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
					// Remove property from property values to bind:
					// It has already caused a field error with a rejected value.
					if (pv != null) {
						mpvs.removePropertyValue(pv);
						propertyValues.remove(field);
					}
				}
			}
		}
	}

	/**
	 * Apply given property values to the target object.
	 * <p>Default implementation applies all of the supplied property
	 * values as bean property values. By default, unknown fields will
	 * be ignored.
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getTarget
	 * @see #getPropertyAccessor
	 * @see #isIgnoreUnknownFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processPropertyAccessException
	 */
	// 将给定的属性值应用于目标对象。
	// <p>默认实现将所有提供的属性值应用为 bean 属性值。默认情况下，未知字段将被忽略
	protected void applyPropertyValues(MutablePropertyValues mpvs) {
		try {
			// Bind request parameters onto target object.
			// 将请求参数绑定到目标对象上。
			getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
		}
		catch (PropertyBatchUpdateException ex) {
			// Use bind error processor to create FieldErrors.
			// 使用绑定错误处理器来创建 FieldErrors
			for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
				getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
			}
		}
	}


	/**
	 * Invoke the specified Validators, if any.
	 * @see #setValidator(Validator)
	 * @see #getBindingResult()
	 */
	// 调用指定的验证器（如果有）。
	public void validate() {
		Object target = getTarget();
		Assert.state(target != null, "No target to validate");
		BindingResult bindingResult = getBindingResult();
		// Call each validator with the same binding result
		// 使用相同的绑定结果调用每个验证器
		for (Validator validator : getValidators()) {
			validator.validate(target, bindingResult);
		}
	}

	/**
	 * Invoke the specified Validators, if any, with the given validation hints.
	 * <p>Note: Validation hints may get ignored by the actual target Validator.
	 * @param validationHints one or more hint objects to be passed to a {@link SmartValidator}
	 * @since 3.1
	 * @see #setValidator(Validator)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	// 使用给定的验证提示调用指定的验证器（如果有）。 <p>注意：实际目标验证器可能会忽略验证提示
	// @param validationHints 一个或多个要传递给 {@link SmartValidator} 的提示对象
	public void validate(Object... validationHints) {
		Object target = getTarget();
		Assert.state(target != null, "No target to validate");
		BindingResult bindingResult = getBindingResult();
		// Call each validator with the same binding result
		// 使用相同的绑定结果调用每个验证器
		for (Validator validator : getValidators()) {
			if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
				((SmartValidator) validator).validate(target, bindingResult, validationHints);
			}
			else if (validator != null) {
				validator.validate(target, bindingResult);
			}
		}
	}

	/**
	 * Close this DataBinder, which may result in throwing
	 * a BindException if it encountered any errors.
	 * @return the model Map, containing target object and Errors instance
	 * @throws BindException if there were any errors in the bind operation
	 * @see BindingResult#getModel()
	 */
	// 关闭此 DataBinder，如果遇到任何错误，可能会导致抛出 BindException。
	// @return 模型 Map，包含目标对象和 Errors 实例
	public Map<?, ?> close() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
		return getBindingResult().getModel();
	}

}
