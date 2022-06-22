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

package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.weaver.patterns.NamePattern;
import org.aspectj.weaver.reflect.ReflectionWorld.ReflectionWorldException;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.*;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AbstractExpressionPointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring {@link org.springframework.aop.Pointcut} implementation
 * that uses the AspectJ weaver to evaluate a pointcut expression.
 *
 * <p>The pointcut expression value is an AspectJ expression. This can
 * reference other pointcuts and use composition and other operations.
 *
 * <p>Naturally, as this is to be processed by Spring AOP's proxy-based model,
 * only method execution pointcuts are supported.
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Dave Syer
 * @since 2.0
 */
// Spring org.springframework.aop.Pointcut 实现，它使用 AspectJ weaver 来评估切入点表达式。
// 切入点表达式值是一个 AspectJ 表达式。 这可以引用其他切入点并使用组合和其他操作。
// 自然，由于这是由 Spring AOP 的基于代理的模型处理的，因此仅支持方法执行切入点。
@SuppressWarnings("serial")
public class AspectJExpressionPointcut extends AbstractExpressionPointcut
		implements ClassFilter, IntroductionAwareMethodMatcher, BeanFactoryAware {

	private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

	// 支持表达式的类型(原语)，Spring 支持的 10 种原语比 AspectJ 提供的 24 种原语要少的多。
	static {
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.ARGS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.REFERENCE);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.THIS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.TARGET);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.WITHIN);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ANNOTATION);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_WITHIN);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ARGS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_TARGET);
	}


	private static final Log logger = LogFactory.getLog(AspectJExpressionPointcut.class);

	@Nullable
	private Class<?> pointcutDeclarationScope;

	private String[] pointcutParameterNames = new String[0];

	private Class<?>[] pointcutParameterTypes = new Class<?>[0];

	// 将 Spring AOP 和 Spring IoC 容器进行整合
	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private transient ClassLoader pointcutClassLoader;

	// 表示 AspectJ 切入点表达式并提供方便的方法来确定切入点是否与根据 java.lang.reflect 接口指定的
	// 连接点(方法)匹配,包含了匹配上下文，提供了方法匹配和构造器匹配两种方式，
	// 整合 AspectJ
	@Nullable
	private transient PointcutExpression pointcutExpression;

	// 在这里将方法，和 方法是否匹配做了一个缓存，下次进来就不需要判断了。
	private transient Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);


	/**
	 * Create a new default AspectJExpressionPointcut.
	 */
	// 创建一个新的默认 AspectJExpressionPointcut。
	public AspectJExpressionPointcut() {
	}

	/**
	 * Create a new AspectJExpressionPointcut with the given settings.
	 * @param declarationScope the declaration scope for the pointcut
	 * @param paramNames the parameter names for the pointcut
	 * @param paramTypes the parameter types for the pointcut
	 */
	// 使用给定的设置创建一个新的 AspectJExpressionPointcut。
	// 形参：
	//			declarationScope - 切入点的声明范围
	// 			paramNames – 切入点的参数名称
	//			paramTypes – 切入点的参数类型
	public AspectJExpressionPointcut(Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
		this.pointcutDeclarationScope = declarationScope;
		if (paramNames.length != paramTypes.length) {
			throw new IllegalStateException(
					"Number of pointcut parameter names must match number of pointcut parameter types");
		}
		this.pointcutParameterNames = paramNames;
		this.pointcutParameterTypes = paramTypes;
	}


	/**
	 * Set the declaration scope for the pointcut.
	 */
	// 设置切入点的声明范围
	public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
		this.pointcutDeclarationScope = pointcutDeclarationScope;
	}

	/**
	 * Set the parameter names for the pointcut.
	 */
	// 设置切入点的参数名称
	public void setParameterNames(String... names) {
		this.pointcutParameterNames = names;
	}

	/**
	 * Set the parameter types for the pointcut.
	 */
	// 设置切入点的参数类型
	public void setParameterTypes(Class<?>... types) {
		this.pointcutParameterTypes = types;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public ClassFilter getClassFilter() {
		obtainPointcutExpression();
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		obtainPointcutExpression();
		return this;
	}


	/**
	 * Check whether this pointcut is ready to match,
	 * lazily building the underlying AspectJ pointcut expression.
	 */
	// 检查这个切入点是否准备好匹配，懒惰地构建底层的 AspectJ 切入点表达式
	private PointcutExpression obtainPointcutExpression() {
		// 获取表达式
		if (getExpression() == null) {
			throw new IllegalStateException("Must set property 'expression' before attempting to match");
		}
		if (this.pointcutExpression == null) {
			this.pointcutClassLoader = determinePointcutClassLoader();
			this.pointcutExpression = buildPointcutExpression(this.pointcutClassLoader);
		}
		return this.pointcutExpression;
	}

	/**
	 * Determine the ClassLoader to use for pointcut evaluation.
	 */
	// 确定用于切入点评估的 ClassLoader
	@Nullable
	private ClassLoader determinePointcutClassLoader() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
		}
		if (this.pointcutDeclarationScope != null) {
			return this.pointcutDeclarationScope.getClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Build the underlying AspectJ pointcut expression.
	 */
	// 构建底层的 AspectJ 切入点表达式
	private PointcutExpression buildPointcutExpression(@Nullable ClassLoader classLoader) {
		PointcutParser parser = initializePointcutParser(classLoader);
		PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
		for (int i = 0; i < pointcutParameters.length; i++) {
			pointcutParameters[i] = parser.createPointcutParameter(
					this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
		}
		// Spring 桥接了 AspectJ 的实现语法
		return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()),
				this.pointcutDeclarationScope, pointcutParameters);
	}

	private String resolveExpression() {
		String expression = getExpression();
		Assert.state(expression != null, "No expression set");
		return expression;
	}

	/**
	 * Initialize the underlying AspectJ pointcut parser.
	 */
	// 初始化底层 AspectJ 切入点解析器
	private PointcutParser initializePointcutParser(@Nullable ClassLoader classLoader) {
		PointcutParser parser = PointcutParser
				.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
						SUPPORTED_PRIMITIVES, classLoader);
		parser.registerPointcutDesignatorHandler(new BeanPointcutDesignatorHandler());
		return parser;
	}


	/**
	 * If a pointcut expression has been specified in XML, the user cannot
	 * write {@code and} as "&&" (though &amp;&amp; will work).
	 * We also allow {@code and} between two pointcut sub-expressions.
	 * <p>This method converts back to {@code &&} for the AspectJ pointcut parser.
	 */
	// 如果在 XML 中指定了切入点表达式，则用户不能将and写为“&&”（尽管 && 会起作用）。
	// 我们还允许and两个切入点子表达式之间。
	// 对于 AspectJ 切入点解析器，此方法会转换回&&
	private String replaceBooleanOperators(String pcExpr) {
		String result = StringUtils.replace(pcExpr, " and ", " && ");
		result = StringUtils.replace(result, " or ", " || ");
		result = StringUtils.replace(result, " not ", " ! ");
		return result;
	}


	/**
	 * Return the underlying AspectJ pointcut expression.
	 */
	// 返回底层的 AspectJ 切入点表达式。
	public PointcutExpression getPointcutExpression() {
		return obtainPointcutExpression();
	}

	@Override
	public boolean matches(Class<?> targetClass) {
		PointcutExpression pointcutExpression = obtainPointcutExpression();
		try {
			try {
				// 判断切入点表达式是否能够匹配目标类型
				return pointcutExpression.couldMatchJoinPointsInType(targetClass);
			}
			catch (ReflectionWorldException ex) {
				logger.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
				// Actually this is still a "maybe" - treat the pointcut as dynamic if we don't know enough yet
				// 实际上这仍然是一个“可能” - 如果我们还不够了解，则将切入点视为动态
				PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
				if (fallbackExpression != null) {
					return fallbackExpression.couldMatchJoinPointsInType(targetClass);
				}
			}
		}
		catch (Throwable ex) {
			logger.debug("PointcutExpression matching rejected target class", ex);
		}
		return false;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
		// 获取表达式
		obtainPointcutExpression();
		// 判断目标类型的方法是否匹配，
		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

		// Special handling for this, target, @this, @target, @annotation
		// in Spring - we can optimize since we know we have exactly this class,
		// and there will never be matching subclass at runtime.
		//
		// Spring 中对 this、target、@this、@target、@annotation 的特殊处理——我们可以优化，
		// 因为我们知道我们有这个类，并且在运行时永远不会有匹配的子类。
		if (shadowMatch.alwaysMatches()) { // 给定方法永远匹配
			return true;
		}
		else if (shadowMatch.neverMatches()) { // 给定方法永不匹配
			return false;
		}
		else {
			// the maybe case
			if (hasIntroductions) { // 如果包含 Introductions，返回 true
				return true;
			}
			// A match test returned maybe - if there are any subtype sensitive variables
			// involved in the test (this, target, at_this, at_target, at_annotation) then
			// we say this is not a match as in Spring there will never be a different
			// runtime subtype.
			//
			// 可能返回匹配测试 - 如果测试中涉及任何子类型敏感变量
			// （this、target、at_this、at_target、at_annotation），那么我们说这不是匹配，
			// 因为在 Spring 中永远不会有不同的运行时子类型
			RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
			return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
		}
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return matches(method, targetClass, false);
	}

	@Override
	public boolean isRuntime() {
		return obtainPointcutExpression().mayNeedDynamicTest();
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		obtainPointcutExpression();
		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

		// Bind Spring AOP proxy to AspectJ "this" and Spring AOP target to AspectJ target,
		// consistent with return of MethodInvocationProceedingJoinPoint
		//
		// 将 Spring AOP 代理绑定到 AspectJ “this”，将 Spring AOP target 绑定到 AspectJ target，
		// 与 MethodInvocationProceedingJoinPoint 的返回一致
		ProxyMethodInvocation pmi = null;
		Object targetObject = null;
		Object thisObject = null;
		try {
			MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
			targetObject = mi.getThis();
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			pmi = (ProxyMethodInvocation) mi;
			thisObject = pmi.getProxy();
		}
		catch (IllegalStateException ex) {
			// No current invocation...
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access current invocation - matching with limited context: " + ex);
			}
		}

		try {
			JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, args);

			/*
			 * Do a final check to see if any this(TYPE) kind of residue match. For
			 * this purpose, we use the original method's (proxy method's) shadow to
			 * ensure that 'this' is correctly checked against. Without this check,
			 * we get incorrect match on this(TYPE) where TYPE matches the target
			 * type but not 'this' (as would be the case of JDK dynamic proxies).
			 * <p>See SPR-2979 for the original bug.
			 */
			// 做最后的检查，看看是否有任何 this(TYPE) 类型的残留匹配。为此，我们使用原始方法（代理方法）的影子
			// 来确保正确检查“this”。如果没有这个检查，我们会在 this(TYPE) 上得到不正确的匹配，其中 TYPE 匹配
			// 目标类型但不匹配 'this'（就像 JDK 动态代理的情况一样）。 <p>有关原始错误，请参阅 SPR-2979。
			if (pmi != null && thisObject != null) {  // there is a current invocation
				RuntimeTestWalker originalMethodResidueTest = getRuntimeTestWalker(getShadowMatch(method, method));
				if (!originalMethodResidueTest.testThisInstanceOfResidue(thisObject.getClass())) {
					return false;
				}
				if (joinPointMatch.matches()) {
					bindParameters(pmi, joinPointMatch);
				}
			}

			return joinPointMatch.matches();
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate join point for arguments " + Arrays.asList(args) +
						" - falling back to non-match", ex);
			}
			return false;
		}
	}

	@Nullable
	protected String getCurrentProxiedBeanName() {
		return ProxyCreationContext.getCurrentProxiedBeanName();
	}


	/**
	 * Get a new pointcut expression based on a target class's loader rather than the default.
	 */
	// 根据目标类的加载器而不是默认值获取新的切入点表达式
	@Nullable
	private PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
		try {
			ClassLoader classLoader = targetClass.getClassLoader();
			if (classLoader != null && classLoader != this.pointcutClassLoader) {
				return buildPointcutExpression(classLoader);
			}
		}
		catch (Throwable ex) {
			logger.debug("Failed to create fallback PointcutExpression", ex);
		}
		return null;
	}

	private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
		if (shadowMatch instanceof DefensiveShadowMatch) {
			return new RuntimeTestWalker(((DefensiveShadowMatch) shadowMatch).primary);
		}
		return new RuntimeTestWalker(shadowMatch);
	}

	private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
		// Note: Can't use JoinPointMatch.getClass().getName() as the key, since
		// Spring AOP does all the matching at a join point, and then all the invocations
		// under this scenario, if we just use JoinPointMatch as the key, then
		// 'last man wins' which is not what we want at all.
		// Using the expression is guaranteed to be safe, since 2 identical expressions
		// are guaranteed to bind in exactly the same way.
		//
		// 注意：不能使用JoinPointMatch.getClass().getName()作为key，因为Spring AOP在一个join点做所有的匹配，
		// 然后这个场景下的所有调用，如果我们只使用JoinPointMatch作为key，那么“最后一个人获胜”，这根本不是我们想要的。
		// 使用表达式保证是安全的，因为 2 个相同的表达式保证以完全相同的方式绑定。
		invocation.setUserAttribute(resolveExpression(), jpm);
	}

	private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
		Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		if (targetMethod.getDeclaringClass().isInterface()) {
			// Try to build the most specific interface possible for inherited methods to be
			// considered for sub-interface matches as well, in particular for proxy classes.
			// Note: AspectJ is only going to take Method.getDeclaringClass() into account.
			//
			// 尝试为要考虑的子接口匹配的继承方法构建最具体的接口，特别是对于代理类。
			// 注意：AspectJ 只会考虑 Method.getDeclaringClass()。
			Set<Class<?>> ifcs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
			if (ifcs.size() > 1) {
				try {
					Class<?> compositeInterface = ClassUtils.createCompositeInterface(
							ClassUtils.toClassArray(ifcs), targetClass.getClassLoader());
					targetMethod = ClassUtils.getMostSpecificMethod(targetMethod, compositeInterface);
				}
				catch (IllegalArgumentException ex) {
					// Implemented interfaces probably expose conflicting method signatures...
					// Proceed with original target method.
					// 实现的接口可能会暴露冲突的方法签名...继续使用原始目标方法
				}
			}
		}
		return getShadowMatch(targetMethod, method);
	}

	private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
		// Avoid lock contention for known Methods through concurrent access...
		// 通过并发访问避免已知方法的锁争用...
		ShadowMatch shadowMatch = this.shadowMatchCache.get(targetMethod);
		if (shadowMatch == null) {
			synchronized (this.shadowMatchCache) {
				// Not found - now check again with full lock...
				// 未找到 - 现在再次使用全锁检查...
				PointcutExpression fallbackExpression = null;
				shadowMatch = this.shadowMatchCache.get(targetMethod);
				if (shadowMatch == null) {
					Method methodToMatch = targetMethod;
					try {
						try {
							shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
						}
						catch (ReflectionWorldException ex) {
							// Failed to introspect target method, probably because it has been loaded
							// in a special ClassLoader. Let's try the declaring ClassLoader instead...
							// 无法内省目标方法，可能是因为它已在特殊的 ClassLoader 中加载。让我们尝试使用声明的 ClassLoader 代替...
							try {
								fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
								if (fallbackExpression != null) {
									shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
								}
							}
							catch (ReflectionWorldException ex2) {
								fallbackExpression = null;
							}
						}
						if (targetMethod != originalMethod && (shadowMatch == null ||
								(shadowMatch.neverMatches() && Proxy.isProxyClass(targetMethod.getDeclaringClass())))) {
							// Fall back to the plain original method in case of no resolvable match or a
							// negative match on a proxy class (which doesn't carry any annotations on its
							// redeclared methods).
							// 如果代理类上没有可解析的匹配或否定匹配（在其重新声明的方法上不带有任何注释），则回退到原始方法
							methodToMatch = originalMethod;
							try {
								shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
							}
							catch (ReflectionWorldException ex) {
								// Could neither introspect the target class nor the proxy class ->
								// let's try the original method's declaring class before we give up...
								// 既不能内省目标类也不能内省代理类->让我们在放弃之前尝试原始方法的声明类......
								try {
									fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
									if (fallbackExpression != null) {
										shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
									}
								}
								catch (ReflectionWorldException ex2) {
									fallbackExpression = null;
								}
							}
						}
					}
					catch (Throwable ex) {
						// Possibly AspectJ 1.8.10 encountering an invalid signature
						// 可能是 AspectJ 1.8.10 遇到无效签名
						logger.debug("PointcutExpression matching rejected target method", ex);
						fallbackExpression = null;
					}
					if (shadowMatch == null) {
						shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
					}
					else if (shadowMatch.maybeMatches() && fallbackExpression != null) {
						shadowMatch = new DefensiveShadowMatch(shadowMatch,
								fallbackExpression.matchesMethodExecution(methodToMatch));
					}
					this.shadowMatchCache.put(targetMethod, shadowMatch);
				}
			}
		}
		return shadowMatch;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AspectJExpressionPointcut)) {
			return false;
		}
		AspectJExpressionPointcut otherPc = (AspectJExpressionPointcut) other;
		return ObjectUtils.nullSafeEquals(this.getExpression(), otherPc.getExpression()) &&
				ObjectUtils.nullSafeEquals(this.pointcutDeclarationScope, otherPc.pointcutDeclarationScope) &&
				ObjectUtils.nullSafeEquals(this.pointcutParameterNames, otherPc.pointcutParameterNames) &&
				ObjectUtils.nullSafeEquals(this.pointcutParameterTypes, otherPc.pointcutParameterTypes);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.getExpression());
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutDeclarationScope);
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterNames);
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterTypes);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AspectJExpressionPointcut: (");
		for (int i = 0; i < this.pointcutParameterTypes.length; i++) {
			sb.append(this.pointcutParameterTypes[i].getName());
			sb.append(' ');
			sb.append(this.pointcutParameterNames[i]);
			if ((i+1) < this.pointcutParameterTypes.length) {
				sb.append(", ");
			}
		}
		sb.append(") ");
		if (getExpression() != null) {
			sb.append(getExpression());
		}
		else {
			sb.append("<pointcut expression not set>");
		}
		return sb.toString();
	}

	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		// 依赖默认序列化，反序列化后初始化状态即可。
		ois.defaultReadObject();

		// Initialize transient fields.
		// pointcutExpression will be initialized lazily by checkReadyToMatch()
		// 初始化瞬态字段。 pointcutExpression 将被 checkReadyToMatch() 延迟初始化
		this.shadowMatchCache = new ConcurrentHashMap<>(32);
	}


	/**
	 * Handler for the Spring-specific {@code bean()} pointcut designator
	 * extension to AspectJ.
	 * <p>This handler must be added to each pointcut object that needs to
	 * handle the {@code bean()} PCD. Matching context is obtained
	 * automatically by examining a thread local variable and therefore a matching
	 * context need not be set on the pointcut.
	 */
	// 特定于 Spring 的bean()切入点指示符扩展到 AspectJ 的处理程序。
	//
	//必须将此处理程序添加到需要处理bean() PCD 的每个切入点对象。
	// 匹配上下文是通过检查线程局部变量自动获得的，因此不需要在切入点上设置匹配上下文
	private class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {

		private static final String BEAN_DESIGNATOR_NAME = "bean";

		@Override
		public String getDesignatorName() {
			return BEAN_DESIGNATOR_NAME;
		}

		@Override
		public ContextBasedMatcher parse(String expression) {
			return new BeanContextMatcher(expression);
		}
	}


	/**
	 * Matcher class for the BeanNamePointcutDesignatorHandler.
	 * <p>Dynamic match tests for this matcher always return true,
	 * since the matching decision is made at the proxy creation time.
	 * For static match tests, this matcher abstains to allow the overall
	 * pointcut to match even when negation is used with the bean() pointcut.
	 */
	// BeanNamePointcutDesignatorHandler 的匹配器类。
	// 这个匹配器的动态匹配测试总是返回 true，因为匹配决定是在代理创建时做出的。
	// 对于静态匹配测试，即使在 bean() 切入点使用否定时，该匹配器也会弃权以允许整个切入点匹配。
	private class BeanContextMatcher implements ContextBasedMatcher {

		private final NamePattern expressionPattern;

		public BeanContextMatcher(String expression) {
			this.expressionPattern = new NamePattern(expression);
		}

		@Override
		@SuppressWarnings("rawtypes")
		@Deprecated
		public boolean couldMatchJoinPointsInType(Class someClass) {
			return (contextMatch(someClass) == FuzzyBoolean.YES);
		}

		@Override
		@SuppressWarnings("rawtypes")
		@Deprecated
		public boolean couldMatchJoinPointsInType(Class someClass, MatchingContext context) {
			return (contextMatch(someClass) == FuzzyBoolean.YES);
		}

		@Override
		public boolean matchesDynamically(MatchingContext context) {
			return true;
		}

		@Override
		public FuzzyBoolean matchesStatically(MatchingContext context) {
			return contextMatch(null);
		}

		@Override
		public boolean mayNeedDynamicTest() {
			return false;
		}

		private FuzzyBoolean contextMatch(@Nullable Class<?> targetType) {
			String advisedBeanName = getCurrentProxiedBeanName();
			if (advisedBeanName == null) {  // no proxy creation in progress
				// abstain; can't return YES, since that will make pointcut with negation fail
				return FuzzyBoolean.MAYBE;
			}
			if (BeanFactoryUtils.isGeneratedBeanName(advisedBeanName)) {
				return FuzzyBoolean.NO;
			}
			if (targetType != null) {
				boolean isFactory = FactoryBean.class.isAssignableFrom(targetType);
				return FuzzyBoolean.fromBoolean(
						matchesBean(isFactory ? BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName : advisedBeanName));
			}
			else {
				return FuzzyBoolean.fromBoolean(matchesBean(advisedBeanName) ||
						matchesBean(BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName));
			}
		}

		private boolean matchesBean(String advisedBeanName) {
			return BeanFactoryAnnotationUtils.isQualifierMatch(
					this.expressionPattern::matches, advisedBeanName, beanFactory);
		}
	}


	private static class DefensiveShadowMatch implements ShadowMatch {

		private final ShadowMatch primary;

		private final ShadowMatch other;

		public DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) {
			this.primary = primary;
			this.other = other;
		}

		@Override
		public boolean alwaysMatches() {
			return this.primary.alwaysMatches();
		}

		@Override
		public boolean maybeMatches() {
			return this.primary.maybeMatches();
		}

		@Override
		public boolean neverMatches() {
			return this.primary.neverMatches();
		}

		@Override
		public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
			try {
				return this.primary.matchesJoinPoint(thisObject, targetObject, args);
			}
			catch (ReflectionWorldException ex) {
				return this.other.matchesJoinPoint(thisObject, targetObject, args);
			}
		}

		@Override
		public void setMatchingContext(MatchingContext aMatchContext) {
			this.primary.setMatchingContext(aMatchContext);
			this.other.setMatchingContext(aMatchContext);
		}
	}

}
