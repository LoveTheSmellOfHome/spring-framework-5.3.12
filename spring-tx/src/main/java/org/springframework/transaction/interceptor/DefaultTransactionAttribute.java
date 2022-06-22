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

package org.springframework.transaction.interceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Spring's common transaction attribute implementation.
 * Rolls back on runtime, but not checked, exceptions by default.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 16.03.2003
 */
// Spring 的通用事务属性实现。默认情况下回滚运行时但未检查的异常
@SuppressWarnings("serial")
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

	@Nullable
	private String descriptor;

	@Nullable
	private String timeoutString;

	@Nullable
	private String qualifier;

	private Collection<String> labels = Collections.emptyList();


	/**
	 * Create a new DefaultTransactionAttribute, with default settings.
	 * Can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	// 使用默认设置创建一个新的 DefaultTransactionAttribute。可以通过 bean 属性设置器进行修改
	public DefaultTransactionAttribute() {
		super();
	}

	/**
	 * Copy constructor. Definition can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	// 复制构造函数。可以通过 bean 属性设置器修改定义
	public DefaultTransactionAttribute(TransactionAttribute other) {
		super(other);
	}

	/**
	 * Create a new DefaultTransactionAttribute with the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	// 使用给定的传播行为创建一个新的 DefaultTransactionAttribute。可以通过 bean 属性设置器进行修改。
	// @parampropagationBehavior TransactionDefinition 接口中的传播常量之一
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}


	/**
	 * Set a descriptor for this transaction attribute,
	 * e.g. indicating where the attribute is applying.
	 * @since 4.3.4
	 */
	// 为这个事务属性设置一个描述符，例如指示应用属性的位置。
	public void setDescriptor(@Nullable String descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * Return a descriptor for this transaction attribute,
	 * or {@code null} if none.
	 * @since 4.3.4
	 */
	// 返回此事务属性的描述符，如果没有，则返回 {@code null}。
	@Nullable
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Set the timeout to apply, if any,
	 * as a String value that resolves to a number of seconds.
	 * @since 5.3
	 * @see #setTimeout
	 * @see #resolveAttributeStrings
	 */
	// 将超时设置为应用（如果有）作为解析为秒数的字符串值。
	public void setTimeoutString(@Nullable String timeoutString) {
		this.timeoutString = timeoutString;
	}

	/**
	 * Return the timeout to apply, if any,
	 * as a String value that resolves to a number of seconds.
	 * @since 5.3
	 * @see #getTimeout
	 * @see #resolveAttributeStrings
	 */
	// 返回要应用的超时（如果有），作为解析为秒数的字符串值。
	@Nullable
	public String getTimeoutString() {
		return this.timeoutString;
	}

	/**
	 * Associate a qualifier value with this transaction attribute.
	 * <p>This may be used for choosing a corresponding transaction manager
	 * to process this specific transaction.
	 * @since 3.0
	 * @see #resolveAttributeStrings
	 */
	// 将限定符值与此事务属性相关联。 <p>这可用于选择相应的事务管理器来处理此特定事务。
	public void setQualifier(@Nullable String qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * Return a qualifier value associated with this transaction attribute.
	 * @since 3.0
	 */
	// 返回与此事务属性关联的限定符值
	@Override
	@Nullable
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * Associate one or more labels with this transaction attribute.
	 * <p>This may be used for applying specific transactional behavior
	 * or follow a purely descriptive nature.
	 * @since 5.3
	 * @see #resolveAttributeStrings
	 */
	// 将一个或多个标签与此交易属性相关联。
	// <p>这可用于应用特定的交易行为或遵循纯粹的描述性质。
	public void setLabels(Collection<String> labels) {
		this.labels = labels;
	}

	@Override
	public Collection<String> getLabels() {
		return this.labels;
	}

	/**
	 * The default behavior is as with EJB: rollback on unchecked exception
	 * ({@link RuntimeException}), assuming an unexpected outcome outside of any
	 * business rules. Additionally, we also attempt to rollback on {@link Error} which
	 * is clearly an unexpected outcome as well. By contrast, a checked exception is
	 * considered a business exception and therefore a regular expected outcome of the
	 * transactional business method, i.e. a kind of alternative return value which
	 * still allows for regular completion of resource operations.
	 * <p>This is largely consistent with TransactionTemplate's default behavior,
	 * except that TransactionTemplate also rolls back on undeclared checked exceptions
	 * (a corner case). For declarative transactions, we expect checked exceptions to be
	 * intentionally declared as business exceptions, leading to a commit by default.
	 * @see org.springframework.transaction.support.TransactionTemplate#execute
	 */
	// 默认行为与 EJB 一样：回滚未经检查的异常 ({@link RuntimeException})，假设任何业务规则之外的意外结果。
	// 此外，我们还尝试回滚 {@link Error}，这显然也是一个意想不到的结果。
	// 相比之下，检查异常被认为是业务异常，因此是事务性业务方法的常规预期结果，即一种仍然允许资源操作的常规完成的替代返回值。
	// <p>这与 TransactionTemplate 的默认行为基本一致，除了 TransactionTemplate 也会回滚未声明的已检查异常（极端情况）。
	// 对于声明性事务，我们希望将检查异常有意声明为业务异常，从而导致默认提交。
	@Override
	public boolean rollbackOn(Throwable ex) {
		return (ex instanceof RuntimeException || ex instanceof Error);
	}


	/**
	 * Resolve attribute values that are defined as resolvable Strings:
	 * {@link #setTimeoutString}, {@link #setQualifier}, {@link #setLabels}.
	 * This is typically used for resolving "${...}" placeholders.
	 * @param resolver the embedded value resolver to apply, if any
	 * @since 5.3
	 */
	public void resolveAttributeStrings(@Nullable StringValueResolver resolver) {
		String timeoutString = this.timeoutString;
		if (StringUtils.hasText(timeoutString)) {
			if (resolver != null) {
				timeoutString = resolver.resolveStringValue(timeoutString);
			}
			if (StringUtils.hasLength(timeoutString)) {
				try {
					setTimeout(Integer.parseInt(timeoutString));
				}
				catch (RuntimeException ex) {
					throw new IllegalArgumentException(
							"Invalid timeoutString value \"" + timeoutString + "\" - cannot parse into int");
				}
			}
		}

		if (resolver != null) {
			if (this.qualifier != null) {
				this.qualifier = resolver.resolveStringValue(this.qualifier);
			}
			Set<String> resolvedLabels = new LinkedHashSet<>(this.labels.size());
			for (String label : this.labels) {
				resolvedLabels.add(resolver.resolveStringValue(label));
			}
			this.labels = resolvedLabels;
		}
	}

	/**
	 * Return an identifying description for this transaction attribute.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected final StringBuilder getAttributeDescription() {
		StringBuilder result = getDefinitionDescription();
		if (StringUtils.hasText(this.qualifier)) {
			result.append("; '").append(this.qualifier).append('\'');
		}
		if (!this.labels.isEmpty()) {
			result.append("; ").append(this.labels);
		}
		return result;
	}

}
