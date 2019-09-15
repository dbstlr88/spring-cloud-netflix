/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.reactive;

import java.net.URI;
import java.util.Map;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

import static com.netflix.appinfo.InstanceInfo.PortType.SECURE;

/**
 * A {@link ReactiveDiscoveryClient} implementation for Eureka.
 *
 * @author Tim Ysewyn
 */
public class EurekaReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private final EurekaClient eurekaClient;

	private final EurekaClientConfig clientConfig;

	public EurekaReactiveDiscoveryClient(EurekaClient eurekaClient,
			EurekaClientConfig clientConfig) {
		this.eurekaClient = eurekaClient;
		this.clientConfig = clientConfig;
	}

	@Override
	public String description() {
		return "Spring Cloud Eureka Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return Flux
				.defer(() -> Flux.fromIterable(
						eurekaClient.getInstancesByVipAddress(serviceId, false)))
				.map(EurekaServiceInstance::new);
	}

	@Override
	public Flux<String> getServices() {
		return Flux.defer(() -> Mono.justOrEmpty(eurekaClient.getApplications()))
				.flatMapIterable(Applications::getRegisteredApplications)
				.filter(application -> !application.getInstances().isEmpty())
				.map(Application::getName).map(String::toLowerCase);
	}

	@Override
	public int getOrder() {
		return clientConfig instanceof Ordered ? ((Ordered) clientConfig).getOrder()
				: ReactiveDiscoveryClient.DEFAULT_ORDER;
	}

	/**
	 * An Eureka-specific {@link ServiceInstance} implementation.
	 */
	public static class EurekaServiceInstance implements ServiceInstance {

		private InstanceInfo instance;

		public EurekaServiceInstance(InstanceInfo instance) {
			Assert.notNull(instance, "Service instance required");
			this.instance = instance;
		}

		public InstanceInfo getInstanceInfo() {
			return instance;
		}

		@Override
		public String getInstanceId() {
			return this.instance.getId();
		}

		@Override
		public String getServiceId() {
			return this.instance.getAppName();
		}

		@Override
		public String getHost() {
			return this.instance.getHostName();
		}

		@Override
		public int getPort() {
			if (isSecure()) {
				return this.instance.getSecurePort();
			}
			return this.instance.getPort();
		}

		@Override
		public boolean isSecure() {
			// assume if secure is enabled, that is the default
			return this.instance.isPortEnabled(SECURE);
		}

		@Override
		public URI getUri() {
			return DefaultServiceInstance.getUri(this);
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.instance.getMetadata();
		}

	}

}
