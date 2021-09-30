/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Sample;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;

import org.springframework.observability.event.Recorder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Strategy that can be used to apply {@link Timer Timers} automatically instead of using
 * {@link Timed @Timed}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.2.0
 */
@FunctionalInterface
public interface AutoTimer {

	/**
	 * An {@link AutoTimer} implementation that is enabled but applies no additional
	 * customizations.
	 */
	AutoTimer ENABLED = (builder) -> {
	};

	/**
	 * An {@link AutoTimer} implementation that is disabled and will not record metrics.
	 */
	AutoTimer DISABLED = new AutoTimer() {

		@Override
		public boolean isEnabled() {
			return false;
		}

		@Override
		public void apply(Sample builder) {
			throw new IllegalStateException("AutoTimer is disabled");
		}

	};

	/**
	 * Return if the auto-timer is enabled and metrics should be recorded.
	 * @return if the auto-timer is enabled
	 */
	default boolean isEnabled() {
		return true;
	}

	/**
	 * Factory method to create a new {@link Builder Timer.Builder} with auto-timer
	 * settings {@link #apply(Timer.Builder) applied}.
	 * @param name the name of the timer
	 * @return a new builder instance with auto-settings applied
	 */
	default Sample builder(String name, Recorder<?> recorder) {
		return builder(() -> Sample.sample(name, recorder));
	}

	/**
	 * Factory method to create a new {@link Builder Timer.Builder} with auto-timer
	 * settings {@link #apply(Timer.Builder) applied}.
	 * @param supplier the builder supplier
	 * @return a new builder instance with auto-settings applied
	 */
	default Sample builder(Supplier<Sample> supplier) {
		Sample builder = supplier.get();
		apply(builder);
		return builder;
	}

	/**
	 * Called to apply any auto-timer settings to the given {@link Builder Timer.Builder}.
	 * @param builder the builder to apply settings to
	 */
	void apply(Sample builder);

	static void apply(AutoTimer autoTimer, Recorder<?> recorder, String metricName, Set<Timed> annotations,
			Consumer<Sample> action) {
		if (!CollectionUtils.isEmpty(annotations)) {
			Sample sample = Sample.sample(metricName, recorder);
			for (Timed annotation : annotations) {
				// TODO: We need to provide support for passing of histograms and percentiles to metrics
				sample.tags(annotation.extraTags());
				if (StringUtils.hasText(annotation.description())) {
					sample.getEvent().description(annotation.description());
				}
				action.accept(sample);
			}
		}
		else {
			if (autoTimer != null && autoTimer.isEnabled()) {
				action.accept(autoTimer.builder(metricName, recorder));
			}
		}
	}

}
