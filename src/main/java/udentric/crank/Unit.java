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

import com.google.common.collect.SetMultimap;
import java.lang.reflect.Constructor;
import java.util.*;

class Unit {
	Unit(Object obj) {
		this(obj.getClass());
		value = obj;
	}

	Unit(Class<?> cls) {
		primaryType = cls;

		if (cls.isArray())
			return;

		Class<?> other = cls.getSuperclass();
		if (other != null) {
			int badness = 1;

			while (other != Object.class) {
				providedTypes.put(other, badness);
				badness++;
				other = other.getSuperclass();
			}
			listAndSortConstructors();
		}

		appendInterfaces(cls.getInterfaces(), 1);
	}

	private void appendInterfaces(Class<?>[] clss, int badness) {
		for (Class<?> cls: clss) {
			providedTypes.put(cls, badness);
			appendInterfaces(cls.getInterfaces(), badness + 1);
		}
	}

	void addToClassMap(SetMultimap<Class<?>, Unit> classMap) {
		classMap.put(primaryType, this);
		providedTypes.forEach((k, v) -> classMap.put(k, this));
	}

	private void listAndSortConstructors() {
		for (Constructor<?> ctor: primaryType.getConstructors()) {
			if (ctor.getParameterCount() == 0)
				continue;

			for (Class<?> cls: ctor.getParameterTypes()) {
				neededTypes.add(cls);
			}

			ctors.add(ctor);
		}

		Collections.sort(ctors, Collections.reverseOrder(
			Comparator.comparingInt(
				Constructor::getParameterCount
			)
		));
	}

	HashSet<Class<?>> neededTypes() {
		return neededTypes;
	}

	private final Class<?> primaryType;
	private final HashMap<
		Class<?>, Integer
	> providedTypes = new HashMap<>();
	private final HashSet<Class<?>> neededTypes = new HashSet<>();
	private final ArrayList<Constructor<?>> ctors = new ArrayList<>();
	private Object value;
}
