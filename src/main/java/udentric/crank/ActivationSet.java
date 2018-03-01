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
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ActivationSet implements AutoCloseable {
	ActivationSet(ValidationSet vs) throws Exception {
		for (ValidationSet.Entry entry: vs.entries) {
			if (!constructObject(entry)) {
				closeAll();
				throw ex;
			}
		}

		MethodHandles.Lookup l = MethodHandles.lookup();

		for (int pos = 0; pos < objs.size(); pos++) {
			if (!startObj(objs.get(pos), l)) {
				stopAll(pos - 1, l);
				closeAll();
				throw ex;
			}
		}
		Crank.LOGGER.debug("activation sequence complete");
	}

	public void stop() {
		stopAll(objs.size() - 1, MethodHandles.lookup());
		closeAll();
	}

	@Override
	public void close() {
		stop();
	}

	private void closeAll() {
		for (int pos = objs.size() - 1; pos >= 0; pos--)
			closeObject(objs.get(pos));

		objs.clear();
	}

	private void stopAll(int pos, MethodHandles.Lookup l) {
		for (; pos >= 0; pos--)
			stopObject(objs.get(pos), l);
	}

	private boolean constructObject(ValidationSet.Entry entry) {
		try {
			Object obj = entry.unit.makeValue(entry.variant);
			objs.add(obj);
			Crank.LOGGER.debug("activation set: added {}", obj);
			return true;
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			Crank.LOGGER.error(
				"exception constructing object of class {}",
				entry.unit.cls, e
			);
			ex = (Exception)e;
			return false;
		}
	}

	private void closeObject(Object obj) {
		if (obj instanceof AutoCloseable) {
			try {
				((AutoCloseable)obj).close();
			} catch (Exception e) {
				Crank.LOGGER.warn(
					"exception closing object {}", obj, e
				);
			}
		}
	}

	private boolean startObj(Object obj, MethodHandles.Lookup l) {
		MethodHandle h;

		try {
			h = l.findVirtual(
				obj.getClass(), "start",
				MethodType.methodType(void.class)
			);
		} catch (ReflectiveOperationException e) {
			Crank.LOGGER.debug(
				"object {} has no accessible start method",
				obj
			);
			return true;
		}

		try {
			h.invoke(obj);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			Crank.LOGGER.error(
				"exception starting object {}", obj, e
			);
			ex = (Exception)e;
			return false;
		}

		return true;
	}

	private void stopObject(Object obj, MethodHandles.Lookup l) {
		MethodHandle h = null;

		try {
			h = l.findVirtual(
				obj.getClass(), "stop",
				MethodType.methodType(void.class)
			);
		} catch (ReflectiveOperationException e) {
			Crank.LOGGER.debug(
				"object {} has no accessible stop method",
				obj
			);
		}

		try {
			if (h != null)
				h.invoke(obj);
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			Crank.LOGGER.warn(
				"exception stopping object {}", obj, e
			);
		}
	}

	private final ArrayList<Object> objs = new ArrayList<>();
	private Exception ex;
}
