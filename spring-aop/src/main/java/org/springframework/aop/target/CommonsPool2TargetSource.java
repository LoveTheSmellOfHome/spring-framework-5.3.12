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

package org.springframework.aop.target;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.aop.TargetSource} implementation that holds
 * objects in a configurable Apache Commons2 Pool.
 *
 * <p>By default, an instance of {@code GenericObjectPool} is created.
 * Subclasses may change the type of {@code ObjectPool} used by
 * overriding the {@code createObjectPool()} method.
 *
 * <p>Provides many configuration properties mirroring those of the Commons Pool
 * {@code GenericObjectPool} class; these properties are passed to the
 * {@code GenericObjectPool} during construction. If creating a subclass of this
 * class to change the {@code ObjectPool} implementation type, pass in the values
 * of configuration properties that are relevant to your chosen implementation.
 *
 * <p>The {@code testOnBorrow}, {@code testOnReturn} and {@code testWhileIdle}
 * properties are explicitly not mirrored because the implementation of
 * {@code PoolableObjectFactory} used by this class does not implement
 * meaningful validation. All exposed Commons Pool properties use the
 * corresponding Commons Pool defaults.
 *
 * <p>Compatible with Apache Commons Pool 2.4, as of Spring 4.2.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 4.2
 * @see GenericObjectPool
 * @see #createObjectPool()
 * @see #setMaxSize
 * @see #setMaxIdle
 * @see #setMinIdle
 * @see #setMaxWait
 * @see #setTimeBetweenEvictionRunsMillis
 * @see #setMinEvictableIdleTimeMillis
 */
// org.springframework.aop.TargetSource实现将对象保存在可配置的 Apache Commons2 池中。
//
// 默认情况下，会创建一个 GenericObjectPool 实例。子类可以通过覆盖 createObjectPool()方法来更改使用的 ObjectPool的类型。
//
// 提供许多镜像 Commons Pool GenericObjectPool类的配置属性；这些属性在构造期间传递给 GenericObjectPool 。
// 如果创建此类的子类以更改ObjectPool实现类型，请传入与您选择的实现相关的配置属性的值。
//
// testOnBorrow、testOnReturn 和 testWhileIdle 属性没有显式镜像，因为此类使用的 PoolableObjectFactory
// 的实现没有实现有意义的验证。所有公开的 Commons Pool 属性都使用相应的 Commons Pool 默认值。
//
// 从 Spring 4.2 开始，与 Apache Commons Pool 2.4 兼容。
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class CommonsPool2TargetSource extends AbstractPoolingTargetSource implements PooledObjectFactory<Object> {

	private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;

	private long timeBetweenEvictionRunsMillis = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

	/**
	 * The Apache Commons {@code ObjectPool} used to pool target objects.
	 */
	// Apache Commons ObjectPool 用于汇集目标对象
	@Nullable
	private ObjectPool pool;


	/**
	 * Create a CommonsPoolTargetSource with default settings.
	 * Default maximum size of the pool is 8.
	 * @see #setMaxSize
	 * @see GenericObjectPoolConfig#setMaxTotal
	 */
	// 使用默认设置创建 CommonsPoolTargetSource。池的默认最大大小为 8。
	public CommonsPool2TargetSource() {
		setMaxSize(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL);
	}


	/**
	 * Set the maximum number of idle objects in the pool.
	 * Default is 8.
	 * @see GenericObjectPool#setMaxIdle
	 */
	// 设置池中空闲对象的最大数量。默认值为 8
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * Return the maximum number of idle objects in the pool.
	 */
	// 返回池中空闲对象的最大数量。
	public int getMaxIdle() {
		return this.maxIdle;
	}

	/**
	 * Set the minimum number of idle objects in the pool.
	 * Default is 0.
	 * @see GenericObjectPool#setMinIdle
	 */
	// 设置池中空闲对象的最小数量。默认值为 0。
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * Return the minimum number of idle objects in the pool.
	 */
	// 返回池中空闲对象的最小数量
	public int getMinIdle() {
		return this.minIdle;
	}

	/**
	 * Set the maximum waiting time for fetching an object from the pool.
	 * Default is -1, waiting forever.
	 * @see GenericObjectPool#setMaxWaitMillis
	 */
	// 设置从池中获取对象的最大等待时间。默认为-1，永远等待。
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * Return the maximum waiting time for fetching an object from the pool.
	 */
	// 返回从池中获取对象的最大等待时间。
	public long getMaxWait() {
		return this.maxWait;
	}

	/**
	 * Set the time between eviction runs that check idle objects whether
	 * they have been idle for too long or have become invalid.
	 * Default is -1, not performing any eviction.
	 * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
	 */
	// 设置检查空闲对象是否空闲时间过长或变得无效的逐出运行之间的时间。默认值为 -1，不执行任何驱逐。
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * Return the time between eviction runs that check idle objects.
	 */
	// 返回检查空闲对象的驱逐运行之间的时间。
	public long getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	/**
	 * Set the minimum time that an idle object can sit in the pool before
	 * it becomes subject to eviction. Default is 1800000 (30 minutes).
	 * <p>Note that eviction runs need to be performed to take this
	 * setting into effect.
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @see GenericObjectPool#setMinEvictableIdleTimeMillis
	 */
	// 设置空闲对象在被驱逐之前可以位于池中的最短时间。默认值为 1800000（30 分钟）。
	// 请注意，需要执行驱逐运行才能使此设置生效。
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Return the minimum time that an idle object can sit in the pool.
	 */
	// 返回空闲对象可以在池中的最短时间。
	public long getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	/**
	 * Set whether the call should bock when the pool is exhausted.
	 */
	// 设置当池耗尽时调用是否阻塞。
	public void setBlockWhenExhausted(boolean blockWhenExhausted) {
		this.blockWhenExhausted = blockWhenExhausted;
	}

	/**
	 * Specify if the call should block when the pool is exhausted.
	 */
	// 指定当池耗尽时调用是否应该阻塞。
	public boolean isBlockWhenExhausted() {
		return this.blockWhenExhausted;
	}


	/**
	 * Creates and holds an ObjectPool instance.
	 * @see #createObjectPool()
	 */
	// 创建并保存一个 ObjectPool 实例
	@Override
	protected final void createPool() {
		logger.debug("Creating Commons object pool");
		this.pool = createObjectPool();
	}

	/**
	 * Subclasses can override this if they want to return a specific Commons pool.
	 * They should apply any configuration properties to the pool here.
	 * <p>Default is a GenericObjectPool instance with the given pool size.
	 * @return an empty Commons {@code ObjectPool}.
	 * @see GenericObjectPool
	 * @see #setMaxSize
	 */
	// 如果子类想要返回特定的 Commons 池，则可以覆盖它。他们应该在此处将任何配置属性应用于池。
	// 默认是具有给定池大小的 GenericObjectPool 实例。
	// 返回值：
	//			一个空的 Commons ObjectPool 。
	protected ObjectPool createObjectPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(getMaxSize());
		config.setMaxIdle(getMaxIdle());
		config.setMinIdle(getMinIdle());
		config.setMaxWaitMillis(getMaxWait());
		config.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		config.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		config.setBlockWhenExhausted(isBlockWhenExhausted());
		// 创建池
		return new GenericObjectPool(this, config);
	}


	/**
	 * Borrows an object from the {@code ObjectPool}.
	 */
	// 从 ObjectPool 借用一个对象。
	@Override
	public Object getTarget() throws Exception {
		Assert.state(this.pool != null, "No Commons ObjectPool available");
		return this.pool.borrowObject();
	}

	/**
	 * Returns the specified object to the underlying {@code ObjectPool}.
	 */
	// 将指定对象返回到底层 ObjectPool 。
	@Override
	public void releaseTarget(Object target) throws Exception {
		if (this.pool != null) {
			// 将对象返还池内，有借有还
			this.pool.returnObject(target);
		}
	}

	@Override
	public int getActiveCount() throws UnsupportedOperationException {
		return (this.pool != null ? this.pool.getNumActive() : 0);
	}

	@Override
	public int getIdleCount() throws UnsupportedOperationException {
		return (this.pool != null ? this.pool.getNumIdle() : 0);
	}


	/**
	 * Closes the underlying {@code ObjectPool} when destroying this object.
	 */
	// 销毁此对象时关闭基础 ObjectPool。
	@Override
	public void destroy() throws Exception {
		if (this.pool != null) {
			logger.debug("Closing Commons ObjectPool");
			this.pool.close();
		}
	}


	//----------------------------------------------------------------------------
	// Implementation of org.apache.commons.pool2.PooledObjectFactory interface
	// org.apache.commons.pool2.PooledObjectFactory 接口的实现
	//----------------------------------------------------------------------------

	@Override
	public PooledObject<Object> makeObject() throws Exception {
		return new DefaultPooledObject<>(newPrototypeInstance());
	}

	@Override
	public void destroyObject(PooledObject<Object> p) throws Exception {
		destroyPrototypeInstance(p.getObject());
	}

	@Override
	public boolean validateObject(PooledObject<Object> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<Object> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Object> p) throws Exception {
	}

}
