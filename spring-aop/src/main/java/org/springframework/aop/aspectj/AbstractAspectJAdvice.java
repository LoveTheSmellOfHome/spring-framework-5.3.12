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

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutParameter;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for AOP Alliance {@link org.aopalliance.aop.Advice} classes
 * wrapping an AspectJ aspect or an AspectJ-annotated advice method.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
// 包装 AspectJ 切面或 AspectJ 注解的建议方法的 AOP 联盟 Advice 类的基类
// 这个 API 包含了许多信息，一部分包含我们正常运行的信息，比如方法名，返回类型，以及异常类型。
// 这个 API 对 JAVA Class 的解析面面俱到，AspectJ 5 种不同语义的 Advice 的父类
//
// 对应 spring-aop.xsd 中 basicAdviceType
@SuppressWarnings("serial")
public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation, Serializable {

	/**
	 * Key used in ReflectiveMethodInvocation userAttributes map for the current joinpoint.
	 */
	// 当前连接点的 ReflectiveMethodInvocation userAttributes 映射中使用的键
	protected static final String JOIN_POINT_KEY = JoinPoint.class.getName();


	/**
	 * Lazily instantiate joinpoint for the current invocation.
	 * Requires MethodInvocation to be bound with ExposeInvocationInterceptor.
	 * <p>Do not use if access is available to the current ReflectiveMethodInvocation
	 * (in an around advice).
	 * @return current AspectJ joinpoint, or through an exception if we're not in a
	 * Spring AOP invocation.
	 */
	// 延迟实例化当前调用的连接点。 要求 MethodInvocation 与 ExposeInvocationInterceptor 绑定。
	// 如果当前 ReflectiveMethodInvocation 可以访问（在环绕通知中），则不要使用。
	// 返回值：
	//			当前的 AspectJ 连接点，或者如果我们不在 Spring AOP 调用中，则通过异常。
	public static JoinPoint currentJoinPoint() {
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
		if (jp == null) {
			// 封装成 Spring 自己的基于方法级别的 MethodInvocationProceedingJoinPoint
			jp = new MethodInvocationProceedingJoinPoint(pmi);
			pmi.setUserAttribute(JOIN_POINT_KEY, jp);
		}
		return jp;
	}


	// containgClass,当前方法所在的引导类
	private final Class<?> declaringClass;

	// 当前方法
	private final String methodName;

	// 方法参数数组
	private final Class<?>[] parameterTypes;

	// AspectJ 执行的建议方法
	protected transient Method aspectJAdviceMethod;

	// 判断点
	private final AspectJExpressionPointcut pointcut;

	// AspectJ 实例工厂
	private final AspectInstanceFactory aspectInstanceFactory;

	/**
	 * The name of the aspect (ref bean) in which this advice was defined
	 * (used when determining advice precedence so that we can determine
	 * whether two pieces of advice come from the same aspect).
	 */
	// 定义此通知的切面（参考 bean）的名称（在确定通知优先级时使用，以便我们可以确定两条通知是否来自同一切面）
	private String aspectName = "";

	/**
	 * The order of declaration of this advice within the aspect.
	 */
	// 该通知在切面内的声明顺序
	private int declarationOrder;

	/**
	 * This will be non-null if the creator of this advice object knows the argument names
	 * and sets them explicitly.
	 */
	// 如果此通知对象的创建者知道参数名称并明确设置它们，则这将是非空的
	@Nullable
	private String[] argumentNames;

	/** Non-null if after throwing advice binds the thrown value. */
	// 如果在抛出通知后绑定抛出的值，则为非 null
	@Nullable
	private String throwingName;

	/** Non-null if after returning advice binds the return value. */
	// 如果在返回通知后绑定返回值，则为非 null
	@Nullable
	private String returningName;

	private Class<?> discoveredReturningType = Object.class;

	// 异常类型
	private Class<?> discoveredThrowingType = Object.class;

	/**
	 * Index for thisJoinPoint argument (currently only
	 * supported at index 0 if present at all).
	 */
	// thisJoinPoint 参数的索引（当前仅在索引 0 处支持，如果存在的话）
	private int joinPointArgumentIndex = -1;

	/**
	 * Index for thisJoinPointStaticPart argument (currently only
	 * supported at index 0 if present at all).
	 */
	// thisJoinPointStaticPart 参数的索引（当前仅在索引 0 处支持，如果存在的话）
	private int joinPointStaticPartArgumentIndex = -1;

	@Nullable
	private Map<String, Integer> argumentBindings;

	private boolean argumentsIntrospected = false;

	@Nullable
	private Type discoveredReturningGenericType;
	// Note: Unlike return type, no such generic information is needed for the throwing type,
	// since Java doesn't allow exception types to be parameterized.
	// 注意：与返回类型不同，抛出类型不需要此类通用信息，因为 Java 不允许对异常类型进行参数化。


	/**
	 * Create a new AbstractAspectJAdvice for the given advice method.
	 * @param aspectJAdviceMethod the AspectJ-style advice method
	 * @param pointcut the AspectJ expression pointcut
	 * @param aspectInstanceFactory the factory for aspect instances
	 */
	// 为给定的通知方法创建一个新的 AbstractAspectJAdvice。
	// 形参：
	//			aspectJAdviceMethod – AspectJ 风格的建议方法
	//			pointcut — AspectJ 表达式切入点
	//			aspectInstanceFactory – 方面实例的工厂
	public AbstractAspectJAdvice(
			Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {

		Assert.notNull(aspectJAdviceMethod, "Advice method must not be null");
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
		this.methodName = aspectJAdviceMethod.getName();
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
		this.aspectJAdviceMethod = aspectJAdviceMethod;
		this.pointcut = pointcut;
		this.aspectInstanceFactory = aspectInstanceFactory;
	}


	/**
	 * Return the AspectJ-style advice method.
	 */
	// 返回 AspectJ 风格的建议方法。
	public final Method getAspectJAdviceMethod() {
		return this.aspectJAdviceMethod;
	}

	/**
	 * Return the AspectJ expression pointcut.
	 */
	// 返回 AspectJ 表达式切入点
	public final AspectJExpressionPointcut getPointcut() {
		calculateArgumentBindings();
		return this.pointcut;
	}

	/**
	 * Build a 'safe' pointcut that excludes the AspectJ advice method itself.
	 * @return a composable pointcut that builds on the original AspectJ expression pointcut
	 * @see #getPointcut()
	 */
	// 构建一个不包括 AspectJ 建议方法本身的“安全”切入点
	// 返回值:
	//			基于原始 AspectJ 表达式切入点的可组合切入点
	public final Pointcut buildSafePointcut() {
		Pointcut pc = getPointcut();
		MethodMatcher safeMethodMatcher = MethodMatchers.intersection(
				new AdviceExcludingMethodMatcher(this.aspectJAdviceMethod), pc.getMethodMatcher());
		return new ComposablePointcut(pc.getClassFilter(), safeMethodMatcher);
	}

	/**
	 * Return the factory for aspect instances.
	 */
	// 返回切面实例的工厂
	public final AspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	/**
	 * Return the ClassLoader for aspect instances.
	 */
	// 返回切面实例的 ClassLoader
	@Nullable
	public final ClassLoader getAspectClassLoader() {
		return this.aspectInstanceFactory.getAspectClassLoader();
	}

	@Override
	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}


	/**
	 * Set the name of the aspect (bean) in which the advice was declared.
	 */
	// 设置声明通知的切面（bean）的名称
	public void setAspectName(String name) {
		this.aspectName = name;
	}

	@Override
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * Set the declaration order of this advice within the aspect.
	 */
	// 在切面内设置此通知的声明顺序
	public void setDeclarationOrder(int order) {
		this.declarationOrder = order;
	}

	@Override
	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	/**
	 * Set by creator of this advice object if the argument names are known.
	 * <p>This could be for example because they have been explicitly specified in XML,
	 * or in an advice annotation.
	 * @param argNames comma delimited list of arg names
	 */
	// 如果参数名称已知，则由此建议对象的创建者设置。
	// 例如，这可能是因为它们已在 XML 或通知注释中明确指定
	// 形参:
	//			argNames – 逗号分隔的 arg 名称列表
	public void setArgumentNames(String argNames) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(argNames);
		setArgumentNamesFromStringArray(tokens);
	}

	public void setArgumentNamesFromStringArray(String... args) {
		this.argumentNames = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			this.argumentNames[i] = StringUtils.trimWhitespace(args[i]);
			if (!isVariableName(this.argumentNames[i])) {
				throw new IllegalArgumentException(
						"'argumentNames' property of AbstractAspectJAdvice contains an argument name '" +
						this.argumentNames[i] + "' that is not a valid Java identifier");
			}
		}
		if (this.argumentNames != null) {
			if (this.aspectJAdviceMethod.getParameterCount() == this.argumentNames.length + 1) {
				// May need to add implicit join point arg name...
				// 可能需要添加隐式连接点参数名称...
				Class<?> firstArgType = this.aspectJAdviceMethod.getParameterTypes()[0];
				if (firstArgType == JoinPoint.class ||
						firstArgType == ProceedingJoinPoint.class ||
						firstArgType == JoinPoint.StaticPart.class) {
					String[] oldNames = this.argumentNames;
					this.argumentNames = new String[oldNames.length + 1];
					this.argumentNames[0] = "THIS_JOIN_POINT";
					System.arraycopy(oldNames, 0, this.argumentNames, 1, oldNames.length);
				}
			}
		}
	}

	public void setReturningName(String name) {
		throw new UnsupportedOperationException("Only afterReturning advice can be used to bind a return value");
	}

	/**
	 * We need to hold the returning name at this level for argument binding calculations,
	 * this method allows the afterReturning advice subclass to set the name.
	 */
	// 我们需要在此级别保存返回名称以进行参数绑定计算，此方法允许 afterReturning 通知子类设置名称
	protected void setReturningNameNoCheck(String name) {
		// name could be a variable or a type...
		// 名称可以是变量或类型...
		if (isVariableName(name)) {
			this.returningName = name;
		}
		else {
			// assume a type
			// 假设一个类型
			try {
				this.discoveredReturningType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Returning name '" + name  +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredReturningType() {
		return this.discoveredReturningType;
	}

	@Nullable
	protected Type getDiscoveredReturningGenericType() {
		return this.discoveredReturningGenericType;
	}

	public void setThrowingName(String name) {
		throw new UnsupportedOperationException("Only afterThrowing advice can be used to bind a thrown exception");
	}

	/**
	 * We need to hold the throwing name at this level for argument binding calculations,
	 * this method allows the afterThrowing advice subclass to set the name.
	 */
	// 我们需要在此级别保存抛出名称以进行参数绑定计算，此方法允许 afterThrowing 通知子类设置名称
	protected void setThrowingNameNoCheck(String name) {
		// name could be a variable or a type...
		if (isVariableName(name)) {
			this.throwingName = name;
		}
		else {
			// assume a type
			try {
				this.discoveredThrowingType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Throwing name '" + name  +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredThrowingType() {
		// 返回异常类型
		return this.discoveredThrowingType;
	}

	private static boolean isVariableName(String name) {
		return AspectJProxyUtils.isVariableName(name);
	}


	/**
	 * Do as much work as we can as part of the set-up so that argument binding
	 * on subsequent advice invocations can be as fast as possible.
	 * <p>If the first argument is of type JoinPoint or ProceedingJoinPoint then we
	 * pass a JoinPoint in that position (ProceedingJoinPoint for around advice).
	 * <p>If the first argument is of type {@code JoinPoint.StaticPart}
	 * then we pass a {@code JoinPoint.StaticPart} in that position.
	 * <p>Remaining arguments have to be bound by pointcut evaluation at
	 * a given join point. We will get back a map from argument name to
	 * value. We need to calculate which advice parameter needs to be bound
	 * to which argument name. There are multiple strategies for determining
	 * this binding, which are arranged in a ChainOfResponsibility.
	 */
	// 作为设置的一部分，尽可能多地做工作，以便在后续通知调用上绑定参数尽可能快。
	//
	// 如果第一个参数是 JoinPoint 或 ProceedingJoinPoint 类型，那么我们在该位置
	// 传递一个 JoinPoint（ProceedingJoinPoint 用于环绕建议）。
	//
	// 如果第一个参数是JoinPoint.StaticPart类型，那么我们在该位置传递一个JoinPoint.StaticPart 。
	//
	// 剩余的参数必须受给定连接点处的切入点评估约束。 我们将得到一个从参数名称到值的映射。
	// 我们需要计算哪个通知参数需要绑定到哪个参数名称。 确定这种绑定有多种策略，它们排列在
	// 一个 ChainOfResponsibility 中。
	public final synchronized void calculateArgumentBindings() {
		// The simple case... nothing to bind.
		if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
			return;
		}

		int numUnboundArgs = this.parameterTypes.length;
		Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
		if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0]) ||
				maybeBindJoinPointStaticPart(parameterTypes[0])) {
			numUnboundArgs--;
		}

		if (numUnboundArgs > 0) {
			// need to bind arguments by name as returned from the pointcut match
			// 需要按从切入点匹配返回的名称绑定参数
			bindArgumentsByName(numUnboundArgs);
		}

		this.argumentsIntrospected = true;
	}

	private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
		if (JoinPoint.class == candidateParameterType) {
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
		if (ProceedingJoinPoint.class == candidateParameterType) {
			if (!supportsProceedingJoinPoint()) {
				throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");
			}
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean supportsProceedingJoinPoint() {
		return false;
	}

	private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType) {
		if (JoinPoint.StaticPart.class == candidateParameterType) {
			this.joinPointStaticPartArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private void bindArgumentsByName(int numArgumentsExpectingToBind) {
		if (this.argumentNames == null) {
			this.argumentNames = createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);
		}
		if (this.argumentNames != null) {
			// We have been able to determine the arg names.
			// 我们已经能够确定 arg 名称
			bindExplicitArguments(numArgumentsExpectingToBind);
		}
		else {
			throw new IllegalStateException("Advice method [" + this.aspectJAdviceMethod.getName() + "] " +
					"requires " + numArgumentsExpectingToBind + " arguments to be bound by name, but " +
					"the argument names were not specified and could not be discovered.");
		}
	}

	/**
	 * Create a ParameterNameDiscoverer to be used for argument binding.
	 * <p>The default implementation creates a {@link DefaultParameterNameDiscoverer}
	 * and adds a specifically configured {@link AspectJAdviceParameterNameDiscoverer}.
	 */
	// 创建一个 ParameterNameDiscoverer 用于参数绑定。
	// 默认实现创建一个 DefaultParameterNameDiscoverer 并添加一个专门配置的
	// AspectJAdviceParameterNameDiscoverer 。
	protected ParameterNameDiscoverer createParameterNameDiscoverer() {
		// We need to discover them, or if that fails, guess,
		// and if we can't guess with 100% accuracy, fail.
		// 我们需要发现它们，或者如果失败，猜测，如果我们不能 100% 准确猜测，失败
		DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
		AspectJAdviceParameterNameDiscoverer adviceParameterNameDiscoverer =
				new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
		adviceParameterNameDiscoverer.setReturningName(this.returningName);
		adviceParameterNameDiscoverer.setThrowingName(this.throwingName);
		// Last in chain, so if we're called and we fail, that's bad...
		// 最后在链中，所以如果我们被调用并且我们失败了，那很糟糕......
		adviceParameterNameDiscoverer.setRaiseExceptions(true);
		discoverer.addDiscoverer(adviceParameterNameDiscoverer);
		return discoverer;
	}

	private void bindExplicitArguments(int numArgumentsLeftToBind) {
		Assert.state(this.argumentNames != null, "No argument names available");
		this.argumentBindings = new HashMap<>();

		int numExpectedArgumentNames = this.aspectJAdviceMethod.getParameterCount();
		if (this.argumentNames.length != numExpectedArgumentNames) {
			throw new IllegalStateException("Expecting to find " + numExpectedArgumentNames +
					" arguments to bind by name in advice, but actually found " +
					this.argumentNames.length + " arguments.");
		}

		// So we match in number...
		// 所以我们在数量上匹配...
		int argumentIndexOffset = this.parameterTypes.length - numArgumentsLeftToBind;
		for (int i = argumentIndexOffset; i < this.argumentNames.length; i++) {
			this.argumentBindings.put(this.argumentNames[i], i);
		}

		// Check that returning and throwing were in the argument names list if
		// specified, and find the discovered argument types.
		// 如果指定，检查返回和抛出是否在参数名称列表中，并找到发现的参数类型。
		if (this.returningName != null) {
			if (!this.argumentBindings.containsKey(this.returningName)) {
				throw new IllegalStateException("Returning argument name '" + this.returningName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.returningName);
				this.discoveredReturningType = this.aspectJAdviceMethod.getParameterTypes()[index];
				this.discoveredReturningGenericType = this.aspectJAdviceMethod.getGenericParameterTypes()[index];
			}
		}
		if (this.throwingName != null) {
			if (!this.argumentBindings.containsKey(this.throwingName)) {
				throw new IllegalStateException("Throwing argument name '" + this.throwingName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.throwingName);
				this.discoveredThrowingType = this.aspectJAdviceMethod.getParameterTypes()[index];
			}
		}

		// configure the pointcut expression accordingly.
		// 相应地配置切入点表达式。
		configurePointcutParameters(this.argumentNames, argumentIndexOffset);
	}

	/**
	 * All parameters from argumentIndexOffset onwards are candidates for
	 * pointcut parameters - but returning and throwing vars are handled differently
	 * and must be removed from the list if present.
	 */
	// 从 argumentIndexOffset 开始的所有参数都是切入点参数的候选者 - 但返回和抛出 var 的处理方式不同，
	// 如果存在则必须从列表中删除
	private void configurePointcutParameters(String[] argumentNames, int argumentIndexOffset) {
		int numParametersToRemove = argumentIndexOffset;
		if (this.returningName != null) {
			numParametersToRemove++;
		}
		if (this.throwingName != null) {
			numParametersToRemove++;
		}
		String[] pointcutParameterNames = new String[argumentNames.length - numParametersToRemove];
		Class<?>[] pointcutParameterTypes = new Class<?>[pointcutParameterNames.length];
		Class<?>[] methodParameterTypes = this.aspectJAdviceMethod.getParameterTypes();

		int index = 0;
		for (int i = 0; i < argumentNames.length; i++) {
			if (i < argumentIndexOffset) {
				continue;
			}
			if (argumentNames[i].equals(this.returningName) ||
				argumentNames[i].equals(this.throwingName)) {
				continue;
			}
			pointcutParameterNames[index] = argumentNames[i];
			pointcutParameterTypes[index] = methodParameterTypes[i];
			index++;
		}

		this.pointcut.setParameterNames(pointcutParameterNames);
		this.pointcut.setParameterTypes(pointcutParameterTypes);
	}

	/**
	 * Take the arguments at the method execution join point and output a set of arguments
	 * to the advice method.
	 * @param jp the current JoinPoint
	 * @param jpMatch the join point match that matched this execution join point
	 * @param returnValue the return value from the method execution (may be null)
	 * @param ex the exception thrown by the method execution (may be null)
	 * @return the empty array if there are no arguments
	 */
	// 在方法执行连接点获取参数并将一组参数输出到通知方法。
	// 参形：
	//			jp – 当前的 JoinPoint，即方法
	//			jpMatch - 匹配此执行连接点的连接点匹配
	//			returnValue – 方法执行的返回值（可能为 null）
	//			ex – 方法执行抛出的异常（可能为 null）
	// 返回值：
	//			如果没有参数，则为空数组
	protected Object[] argBinding(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable ex) {

		calculateArgumentBindings();

		// AMC start
		Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
		int numBound = 0;

		if (this.joinPointArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointArgumentIndex] = jp;
			numBound++;
		}
		else if (this.joinPointStaticPartArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
			numBound++;
		}

		if (!CollectionUtils.isEmpty(this.argumentBindings)) {
			// binding from pointcut match
			// 从切入点匹配绑定
			if (jpMatch != null) {
				PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
				for (PointcutParameter parameter : parameterBindings) {
					String name = parameter.getName();
					Integer index = this.argumentBindings.get(name);
					adviceInvocationArgs[index] = parameter.getBinding();
					numBound++;
				}
			}
			// binding from returning clause
			// 从返回子句绑定
			if (this.returningName != null) {
				Integer index = this.argumentBindings.get(this.returningName);
				adviceInvocationArgs[index] = returnValue;
				numBound++;
			}
			// binding from thrown exception
			// 从抛出的异常绑定
			if (this.throwingName != null) {
				Integer index = this.argumentBindings.get(this.throwingName);
				adviceInvocationArgs[index] = ex;
				numBound++;
			}
		}

		if (numBound != this.parameterTypes.length) {
			throw new IllegalStateException("Required to bind " + this.parameterTypes.length +
					" arguments, but only bound " + numBound + " (JoinPointMatch " +
					(jpMatch == null ? "was NOT" : "WAS") + " bound in invocation)");
		}

		return adviceInvocationArgs;
	}


	/**
	 * Invoke the advice method.
	 * @param jpMatch the JoinPointMatch that matched this execution join point
	 * @param returnValue the return value from the method execution (may be null)
	 * @param ex the exception thrown by the method execution (may be null)
	 * @return the invocation result
	 * @throws Throwable in case of invocation failure
	 */
	// 调用通知方法。
	// 参形：
	//			jpMatch – 匹配此执行连接点的 JoinPointMatch
	//			returnValue – 方法执行的返回值（可能为 null）
	//			ex – 方法执行抛出的异常（可能为 null）
	// 返回值：
	//			调用结果
	// 抛出：
	//			Throwable - 在调用失败的情况下
	protected Object invokeAdviceMethod(
			@Nullable JoinPointMatch jpMatch, @Nullable Object returnValue, @Nullable Throwable ex)
			throws Throwable {

		// 使用指定参数调用 Advice 方法，找到 它所匹配的 Joinpoint 的一个方法，然后进行调用
		return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
	}

	// As above, but in this case we are given the join point.
	// 如上所述，但在这种情况下，我们得到了连接点。
	protected Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable t) throws Throwable {

		return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
	}

	// 使用给定参数调用 Advice 方法
	protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
		Object[] actualArgs = args;
		if (this.aspectJAdviceMethod.getParameterCount() == 0) {
			actualArgs = null;
		}
		try {
			// 找到 aspectJAdviceMethod，并强吻
			ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
			// this.aspectJAdviceMethod.invoke（被代理对象，实际参数）
			return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("Mismatch on arguments to advice method [" +
					this.aspectJAdviceMethod + "]; pointcut expression [" +
					this.pointcut.getPointcutExpression() + "]", ex);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * Overridden in around advice to return proceeding join point.
	 */
	// 覆盖周围的建议以返回进行中的连接点
	protected JoinPoint getJoinPoint() {
		return currentJoinPoint();
	}

	/**
	 * Get the current join point match at the join point we are being dispatched on.
	 */
	// 在我们被调度的连接点获取当前连接点匹配
	@Nullable
	protected JoinPointMatch getJoinPointMatch() {
		// 拿到当前拦截的方法
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		return getJoinPointMatch((ProxyMethodInvocation) mi);
	}

	// Note: We can't use JoinPointMatch.getClass().getName() as the key, since
	// Spring AOP does all the matching at a join point, and then all the invocations.
	// Under this scenario, if we just use JoinPointMatch as the key, then
	// 'last man wins' which is not what we want at all.
	// Using the expression is guaranteed to be safe, since 2 identical expressions
	// are guaranteed to bind in exactly the same way.
	// 注意：我们不能使用 JoinPointMatch.getClass().getName() 作为键，因为 Spring AOP 在连接点完成所有匹配，
	// 然后是所有调用。在这种情况下，如果我们只使用 JoinPointMatch 作为键，那么“最后一个人获胜”，这根本不是我们想要的。
	// 使用表达式保证是安全的，因为 2 个相同的表达式保证以完全相同的方式绑定。
	@Nullable
	protected JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi) {
		// 拿到表达式
		String expression = this.pointcut.getExpression();
		return (expression != null ? (JoinPointMatch) pmi.getUserAttribute(expression) : null);
	}


	@Override
	public String toString() {
		return getClass().getName() + ": advice method [" + this.aspectJAdviceMethod + "]; " +
				"aspect name '" + this.aspectName + "'";
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		try {
			// 从当前引导类种获取指定方法
			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
		}
	}


	/**
	 * MethodMatcher that excludes the specified advice method.
	 * @see AbstractAspectJAdvice#buildSafePointcut()
	 */
	// 排除指定通知方法的 MethodMatcher
	private static class AdviceExcludingMethodMatcher extends StaticMethodMatcher {

		private final Method adviceMethod;

		public AdviceExcludingMethodMatcher(Method adviceMethod) {
			this.adviceMethod = adviceMethod;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return !this.adviceMethod.equals(method);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AdviceExcludingMethodMatcher)) {
				return false;
			}
			AdviceExcludingMethodMatcher otherMm = (AdviceExcludingMethodMatcher) other;
			return this.adviceMethod.equals(otherMm.adviceMethod);
		}

		@Override
		public int hashCode() {
			return this.adviceMethod.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.adviceMethod;
		}
	}

}
