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
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StartStopActivator implements ObjectActivator {
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException propagate(
		Throwable t
	) throws T {
		throw (T)t;
	}

	@Override
	public void start(Object obj) {
		findMethod(obj, "start", NO_ARGS_METHOD_TYPE).ifPresent(h -> {
			try {
				h.invoke(obj);
			} catch (Error e) {
				throw e;
			} catch (Throwable e) {
				LOGGER.error(() -> msgFactory.apply(
					"exception starting object"
				).with("object", obj), e);
				propagate(e);
			}
		});
	}

	@Override
	public void stop(Object obj) {
		findMethod(obj, "stop", NO_ARGS_METHOD_TYPE).ifPresent(h -> {
			try {
				h.invoke(obj);
			} catch (Error e) {
				throw e;
			} catch (Throwable e) {
				LOGGER.error(() -> msgFactory.apply(
					"exception stopping object"
				).with("object", obj), e);
			}
		});
	}

	private Optional<MethodHandle> findMethod(
		Object obj, String method, MethodType sig
	) {
		try {
			return Optional.of(lookup.findVirtual(
				obj.getClass(), method, sig
			));
		} catch (ReflectiveOperationException e) {
			LOGGER.debug(() -> msgFactory.apply(
				"method not accessible"
			).with("object", obj).with("method", method));
			return Optional.empty();
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

	private final MethodHandles.Lookup lookup = MethodHandles.lookup();
	private Function<String, LogMessage> msgFactory = LogMessage::new;
}
