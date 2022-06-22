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

package org.springframework.aop.aspectj.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

/**
 * Metadata for an AspectJ aspect class, with an additional Spring AOP pointcut
 * for the per clause.
 *
 * <p>Uses AspectJ 5 AJType reflection API, enabling us to work with different
 * AspectJ instantiation models such as "singleton", "pertarget" and "perthis".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcut
 */
// AspectJ 切面类的元数据，带有用于 per 子句的附加 Spring AOP 切入点。
//
// 使用 AspectJ 5 AJType 反射 API，使我们能够使用不同的 AspectJ 实例化模型，
// 例如“singleton”、“pertarget”和“perthis”。
//
// 和 Java 反射密切相关的元数据
// 可序列化的意思是：可以存储在不同的介质中。比如数据库，磁盘，网络空间
@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

	/**
	 * The name of this aspect as defined to Spring (the bean name) -
	 * allows us to determine if two pieces of advice come from the
	 * same aspect and hence their relative precedence.
	 */
	// 这个切面的名称定义为 Spring（bean 名称） - 允许我们确定两条建议是否来自同一切面，从而确定它们的相对优先级。
	private final String aspectName;

	/**
	 * The aspect class, stored separately for re-resolution of the
	 * corresponding AjType on deserialization.
	 */
	// 切面类，单独存储，用于在反序列化时重新解析相应的 AjType。
	private final Class<?> aspectClass;

	/**
	 * AspectJ reflection information (AspectJ 5 / Java 5 specific).
	 * Re-resolved on deserialization since it isn't serializable itself.
	 */
	// AspectJ 反射信息（AspectJ 5 / Java 5 特定）。 重新解决反序列化问题，因为它本身不可序列化
	private transient AjType<?> ajType;

	/**
	 * Spring AOP pointcut corresponding to the per clause of the
	 * aspect. Will be the Pointcut.TRUE canonical instance in the
	 * case of a singleton, otherwise an AspectJExpressionPointcut.
	 */
	// Spring AOP 切入点对应切面的 per 子句。 在单例的情况下将是 Pointcut.TRUE 规范实例，
	// 否则是 AspectJExpressionPointcut。
	private final Pointcut perClausePointcut;


	/**
	 * Create a new AspectMetadata instance for the given aspect class.
	 * @param aspectClass the aspect class
	 * @param aspectName the name of the aspect
	 */
	// 为给定的切面类创建一个新的 AspectMetadata 实例。
	// 参形：
	//			aspectClass – 切面类
	//			aspectName – 切面的名称
	public AspectMetadata(Class<?> aspectClass, String aspectName) {
		this.aspectName = aspectName;

		Class<?> currClass = aspectClass;
		AjType<?> ajType = null;
		// 递归，从子类迭代到 Object 父类
		while (currClass != Object.class) {
			// WeakReference:类似于线程安全的 WeakHashMap，存储在这个数据结构的数据不需要长期存储，
			// 在 JVM 内存不足，垃圾收集器在某个时间点确定一个对象是弱可达的。那时，它将原子地清除对该
			// 对象的所有弱引用以及对通过强引用和软引用链可以访问该对象的任何其他弱可达对象的所有弱引用。
			// 同时它将声明所有以前的弱可达对象都是可终结的。在同一时间或稍后的某个时间，它会将那些在
			// 引用队列中注册的新清除的弱引用排入队列
			AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
			if (ajTypeToCheck.isAspect()) {
				ajType = ajTypeToCheck;
				break;
			}
			currClass = currClass.getSuperclass();
		}
		if (ajType == null) {
			throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
		}
		if (ajType.getDeclarePrecedence().length > 0) {
			throw new IllegalArgumentException("DeclarePrecedence not presently supported in Spring AOP");
		}
		this.aspectClass = ajType.getJavaClass();
		this.ajType = ajType;

		switch (this.ajType.getPerClause().getKind()) {
			case SINGLETON:
				this.perClausePointcut = Pointcut.TRUE;
				return;
			case PERTARGET:
			case PERTHIS:
				AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
				ajexp.setLocation(aspectClass.getName());
				ajexp.setExpression(findPerClause(aspectClass));
				ajexp.setPointcutDeclarationScope(aspectClass);
				this.perClausePointcut = ajexp;
				return;
			case PERTYPEWITHIN:
				// Works with a type pattern
				this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
				return;
			default:
				throw new AopConfigException(
						"PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
		}
	}

	/**
	 * Extract contents from String of form {@code pertarget(contents)}.
	 */
	// 从 pertarget(contents) 形式的字符串中提取内容
	private String findPerClause(Class<?> aspectClass) {
		String str = aspectClass.getAnnotation(Aspect.class).value();
		int beginIndex = str.indexOf('(') + 1;
		int endIndex = str.length() - 1;
		return str.substring(beginIndex, endIndex);
	}


	/**
	 * Return AspectJ reflection information.
	 */
	// 返回 AspectJ 反射信息
	public AjType<?> getAjType() {
		return this.ajType;
	}

	/**
	 * Return the aspect class.
	 */
	// 返回切面类
	public Class<?> getAspectClass() {
		return this.aspectClass;
	}

	/**
	 * Return the aspect name.
	 */
	// 返回切面名称
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * Return a Spring pointcut expression for a singleton aspect.
	 * (e.g. {@code Pointcut.TRUE} if it's a singleton).
	 */
	// 返回单例切面的 Spring 切入点表达式。 （例如 Pointcut.TRUE 如果它是单例）
	public Pointcut getPerClausePointcut() {
		return this.perClausePointcut;
	}

	/**
	 * Return whether the aspect is defined as "perthis" or "pertarget".
	 */
	// 返回切面是定义为“perthis”还是“pertarget”
	public boolean isPerThisOrPerTarget() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
	}

	/**
	 * Return whether the aspect is defined as "pertypewithin".
	 */
	// 返回切面是否定义为“pertypewithin”。
	public boolean isPerTypeWithin() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTYPEWITHIN);
	}

	/**
	 * Return whether the aspect needs to be lazily instantiated.
	 */
	// 返回是否需要延迟实例化切面
	public boolean isLazilyInstantiated() {
		return (isPerThisOrPerTarget() || isPerTypeWithin());
	}


	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.ajType = AjTypeSystem.getAjType(this.aspectClass);
	}

}
