/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.observability.tracing.listener;

import java.util.concurrent.TimeUnit;

import org.springframework.observability.event.instant.InstantRecording;
import org.springframework.observability.event.interval.IntervalRecording;
import org.springframework.observability.event.listener.RecordingListener;
import org.springframework.observability.tracing.Span;
import org.springframework.observability.tracing.SpanAndScope;
import org.springframework.observability.tracing.Tracer;

/**
 * {@link RecordingListener} that uses the Tracing API to record events.
 */
public class TracingRecordingListener implements RecordingListener<TracingRecordingListener.TracingContext> {

	private final Tracer tracer;

	/**
	 * @param tracer The tracer to use to record events.
	 */
	public TracingRecordingListener(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void onStart(IntervalRecording<TracingContext> intervalRecording) {
		Span span = this.tracer.nextSpan().name(intervalRecording.getName())
				.start(getStartTimeInMicros(intervalRecording));
		intervalRecording.getContext().setSpanAndScope(span, this.tracer.withSpan(span));
	}

	@Override
	public void onStop(IntervalRecording<TracingContext> intervalRecording) {
		SpanAndScope spanAndScope = intervalRecording.getContext().getSpanAndScope();
		Span span = spanAndScope.getSpan();
		span.name(intervalRecording.getName());
		intervalRecording.getTags().forEach(tag -> span.tag(tag.getKey(), tag.getValue()));
		spanAndScope.getScope().close();
		span.end(getStopTimeInMicros(intervalRecording));
	}

	@Override
	public void onError(IntervalRecording<TracingContext> intervalRecording) {
		SpanAndScope spanAndScope = intervalRecording.getContext().getSpanAndScope();
		Span span = spanAndScope.getSpan();
		span.error(intervalRecording.getError());
	}

	@Override
	public void record(InstantRecording instantRecording) {
		// TODO: Context is not shared between instant and interval
		Span span = this.tracer.currentSpan();
		if (span != null) {
			String name = instantRecording.getEvent().getName();
			if (instantRecording.eventNanos() != null) {
				span.event(instantRecording.eventNanos(), name);
			}
			else {
				span.event(name);
			}
		}
	}

	@Override
	public TracingContext createContext() {
		return new TracingContext();
	}

	private long getStartTimeInMicros(IntervalRecording<TracingContext> recording) {
		return TimeUnit.NANOSECONDS.toMicros(recording.getStartWallTime());
	}

	private long getStopTimeInMicros(IntervalRecording<TracingContext> recording) {
		return TimeUnit.NANOSECONDS.toMicros(recording.getStartWallTime() + recording.getDuration().toNanos());
	}

	static class TracingContext {

		private SpanAndScope spanAndScope;

		SpanAndScope getSpanAndScope() {
			return this.spanAndScope;
		}

		void setSpanAndScope(Span span, Tracer.SpanInScope spanInScope) {
			this.spanAndScope = new SpanAndScope(span, spanInScope);
		}

	}

}
