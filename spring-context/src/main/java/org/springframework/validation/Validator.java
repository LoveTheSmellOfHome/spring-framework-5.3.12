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

package org.springframework.validation;

/**
 * A validator for application-specific objects.
 *
 * <p>This interface is totally divorced from any infrastructure
 * or context; that is to say it is not coupled to validating
 * only objects in the web tier, the data-access tier, or the
 * whatever-tier. As such it is amenable to being used in any layer
 * of an application, and supports the encapsulation of validation
 * logic as a first-class citizen in its own right.
 *
 * <p>Find below a simple but complete {@code Validator}
 * implementation, which validates that the various {@link String}
 * properties of a {@code UserLogin} instance are not empty
 * (that is they are not {@code null} and do not consist
 * wholly of whitespace), and that any password that is present is
 * at least {@code 'MINIMUM_PASSWORD_LENGTH'} characters in length.
 *
 * <pre class="code">public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>See also the Spring reference manual for a fuller discussion of
 * the {@code Validator} interface and its role in an enterprise
 * application.
 *
 * @author Rod Johnson
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 */
// 应用程序特定对象的校验器
//
// 这个接口完全脱离了任何基础设施或上下文；也就是说，它不只验证 Web 层、数据访问层或任何层中的对象。
// 因此，它适用于应用程序的任何层，并支持封装验证逻辑作为其自身的一等公民。
//
// 在下面找到一个简单但完整的 Validator 实现，它验证 UserLogin 实例的各种 String 属性不为空（即它们不为空且不完全由空格组成），
// 并且存在的任何密码至少为 ' MINIMUM_PASSWORD_LENGTH' 个字符的长度。
//
// 另请参阅 Spring 参考手册以更全面地讨论 Validator 接口及其在企业应用程序中的作用
// Spring 内部校验接口，通过编程的方式校验目标对象
public interface Validator {

	/**
	 * Can this {@link Validator} {@link #validate(Object, Errors) validate}
	 * instances of the supplied {@code clazz}?
	 * <p>This method is <i>typically</i> implemented like so:
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * (Where {@code Foo} is the class (or superclass) of the actual
	 * object instance that is to be {@link #validate(Object, Errors) validated}.)
	 * @param clazz the {@link Class} that this {@link Validator} is
	 * being asked if it can {@link #validate(Object, Errors) validate}
	 * @return {@code true} if this {@link Validator} can indeed
	 * {@link #validate(Object, Errors) validate} instances of the
	 * supplied {@code clazz}
	 */
	// 这个验证器可以验证提供的 clazz 的实例吗？
	// 这个方法通常是这样实现的： return Foo.class.isAssignableFrom(clazz); （其中 Foo 是要验证的实际对象实例的类（或超类）。）
	// 确认目标类能否被校验
	boolean supports(Class<?> clazz);

	/**
	 * Validate the supplied {@code target} object, which must be
	 * of a {@link Class} for which the {@link #supports(Class)} method
	 * typically has (or would) return {@code true}.
	 * <p>The supplied {@link Errors errors} instance can be used to report
	 * any resulting validation errors.
	 * @param target the object that is to be validated
	 * @param errors contextual state about the validation process
	 * @see ValidationUtils
	 */
	// 验证提供的目标对象，该对象必须属于一个类，其支持（类）方法通常具有（或将）返回 true。
	// 提供的错误实例可用于报告任何由此产生的验证错误
	// 校验目标对象，并将校验失败的内容输出至 Errors 对象
	void validate(Object target, Errors errors);

}
