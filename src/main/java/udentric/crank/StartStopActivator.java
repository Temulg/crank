/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This file is made available under the GNU General Public License
 * version 3 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package udentric.crank;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StartStopActivator implements ObjectActivator {
	public StartStopActivator() {
		lookup = MethodHandles.lookup();
	}

	@Override
	public void start(Object obj) throws Exception {
		MethodHandle h;

		try {
			h = lookup.findVirtual(
				obj.getClass(), "start", NO_ARGS_METHOD_TYPE
			);
		} catch (ReflectiveOperationException e) {
			LOGGER.debug(() -> msgFactory.apply(
				"no accessible start method"
			).with("object", obj));
			return;
		}

		try {
			h.invoke(obj);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			LOGGER.error(() -> msgFactory.apply(
				"exception starting object"
			).with("object", obj), e);
			throw (Exception)e;
		}

		return;
	}

	@Override
	public void stop(Object obj) {
		MethodHandle h = null;

		try {
			h = lookup.findVirtual(
				obj.getClass(), "stop",
				MethodType.methodType(void.class)
			);
		} catch (ReflectiveOperationException e) {
			LOGGER.debug(() -> msgFactory.apply(
				"no accessible stop method"
			).with("object", obj));
		}

		try {
			if (h != null)
				h.invoke(obj);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			LOGGER.error(() -> msgFactory.apply(
				"exception stopping object"
			).with("object", obj), e);
		}
	}

	@Override
	public void release(Object obj) {
		if (obj instanceof AutoCloseable) {
			try {
				((AutoCloseable)obj).close();
			} catch (Exception e) {
				LOGGER.warn(() -> msgFactory.apply(
					"exception releasing object"
				).with("object", obj), e);
			}
		}
	}

	public void setLogMessageFactory(
		Function<String, LogMessage> msgFactory_
	) {
		msgFactory = msgFactory_;
	}

	private static final Logger LOGGER = LogManager.getLogger();
	private static final MethodType NO_ARGS_METHOD_TYPE
	= MethodType.methodType(void.class);

	private final MethodHandles.Lookup lookup;
	private Function<String, LogMessage> msgFactory = LogMessage::new;
}
