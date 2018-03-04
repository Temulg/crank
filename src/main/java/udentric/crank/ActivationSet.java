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

import java.util.ArrayList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActivationSet implements AutoCloseable {
	ActivationSet(
		ValidationSet vs, ObjectActivator activator_,
		Function<String, LogMessage> msgFactory_
	) throws Exception {
		activator = activator_;
		msgFactory = msgFactory_;
		for (ValidationSet.Entry entry: vs.entries) {
			if (!constructObject(entry)) {
				closeAll();
				throw ex;
			}
		}

		for (int pos = 0; pos < objs.size(); pos++) {
			if (!startObj(objs.get(pos))) {
				stopAll(pos - 1);
				closeAll();
				throw ex;
			}
		}
		LOGGER.debug(() -> msgFactory.apply(
			"activation sequence complete"
		));
	}

	public void stop() {
		stopAll(objs.size() - 1);
		closeAll();
	}

	@Override
	public void close() {
		stop();
	}

	private void closeAll() {
		for (int pos = objs.size() - 1; pos >= 0; pos--)
			activator.release(objs.get(pos));

		objs.clear();
	}

	private void stopAll(int pos) {
		for (; pos >= 0; pos--)
			activator.stop(objs.get(pos));
	}

	private boolean constructObject(ValidationSet.Entry entry) {
		try {
			Object obj = entry.unit.obtainValue(entry.variant);
			objs.add(obj);
			LOGGER.debug(() -> msgFactory.apply(
				"object added to activation set"
			).with("object", obj));
			return true;
		} catch (Throwable e) {
			if (e instanceof Error)
				throw (Error)e;

			LOGGER.error(() -> msgFactory.apply(
				"exception obtaining value from unit"
			).with(
				"unit", entry.unit
			).with(
				"variant", entry.variant
			), e);

			ex = (Exception)e;
			return false;
		}
	}

	private boolean startObj(Object obj) {
		try {
			activator.start(obj);
			return true;
		} catch (Exception e) {
			ex = e;
			return false;
		}
	}

	static private final Logger LOGGER = LogManager.getLogger();

	private final ArrayList<Object> objs = new ArrayList<>();
	private final ObjectActivator activator;
	private final Function<String, LogMessage> msgFactory;
	private Exception ex;
}
