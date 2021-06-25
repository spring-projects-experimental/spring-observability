/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.observability.tracing.brave.bridge;

import org.springframework.observability.tracing.SpanCustomizer;
import org.springframework.observability.tracing.docs.AssertingSpanCustomizer;

/**
 * Brave implementation of a {@link SpanCustomizer}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class BraveSpanCustomizer implements SpanCustomizer {

	private final brave.SpanCustomizer spanCustomizer;

	/**
	 * @param spanCustomizer Brave delegate
	 */
	public BraveSpanCustomizer(brave.SpanCustomizer spanCustomizer) {
		this.spanCustomizer = spanCustomizer;
	}

	static brave.SpanCustomizer toBrave(SpanCustomizer spanCustomizer) {
		return ((BraveSpanCustomizer) AssertingSpanCustomizer.unwrap(spanCustomizer)).spanCustomizer;
	}

	static SpanCustomizer fromBrave(brave.SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

	@Override
	public SpanCustomizer name(String name) {
		return new BraveSpanCustomizer(this.spanCustomizer.name(name));
	}

	@Override
	public SpanCustomizer tag(String key, String value) {
		return new BraveSpanCustomizer(this.spanCustomizer.tag(key, value));
	}

	@Override
	public SpanCustomizer event(String value) {
		return new BraveSpanCustomizer(this.spanCustomizer.annotate(value));
	}

}
