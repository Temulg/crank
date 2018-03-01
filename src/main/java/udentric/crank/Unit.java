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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class Unit {
	Unit(Object obj) {
		cls = obj.getClass();
		ctors = ImmutableList.<Constructor<?>>builder().build();
		ctorArgs = ImmutableTable.<
			Integer, Integer, ClassRequirement
		>builder().build();
		value = obj;
	}

	Unit(Class<?> cls_) {
		if (cls_.isArray() || null == cls_.getSuperclass())
			throw new IllegalArgumentException(String.format(
				"Class %s is not an object class", cls_
			));

		cls = cls_;
		value = null;

		ImmutableList.Builder<
			Constructor<?>
		> lb = ImmutableList.builder();
		ImmutableTable.Builder<
			Integer, Integer, ClassRequirement
		> tb = ImmutableTable.builder();

		int row = 0;
		for (Constructor<?> ctor: sortConstructors()) {
			lb.add(ctor);
			int col = 0;
			for (Parameter p: ctor.getParameters()) {
				tb.put(
					row, col,
					new ClassRequirement(p.getType())
				);
				col++;
			}
			row++;
		}

		ctors = lb.build();
		ctorArgs = tb.build();
	}

	void collectOfferings(OfferingSet s) {
		s.addClassOffering(new ClassOffering(this, cls));

		Class<?> sc = cls.getSuperclass();

		int badness = 1;

		while (sc != Object.class) {
			s.addClassOffering(
				new AbstractClassOffering(this, sc, badness)
			);
			badness++;
			sc = sc.getSuperclass();
		}

		collectInterfaces(s, cls.getInterfaces(), 1);
	}

	private void collectInterfaces(
		OfferingSet s, Class<?>[] ifaces, int badness
	) {
		for (Class<?> iface: ifaces) {
			s.addClassOffering(new AbstractClassOffering(
				this, iface, badness
			));
			collectInterfaces(
				s, iface.getInterfaces(), badness + 1
			);
		}
	}

	void collectRequirements(List<Requirement> rl) {
		ctorArgs.cellSet().forEach(c -> rl.add(c.getValue()));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(
			"Class", cls
		).add(
			"hasValue", value != null
		).toString();
	}

	private ArrayList<Constructor<?>> sortConstructors() {
		ArrayList<Constructor<?>> rv = new ArrayList<>();

		for (Constructor<?> ctor: cls.getConstructors()) {
			if (ctor.getParameterCount() == 0)
				continue;

			rv.add(ctor);
		}

		Collections.sort(rv, Collections.reverseOrder(
			Comparator.comparingInt(
				Constructor::getParameterCount
			)
		));
		return rv;
	}

	int selectConstructor(OfferingSet offSet) {
		int count = ctors.size();

		nextCtor: for (int pos = 0; pos < count; pos++) {
			for (ClassRequirement cr: ctorArgs.row(pos).values()) {
				if (!offSet.satisfy(cr))
					continue nextCtor;
			}
			return pos;
		}

		return -1;
	}

	Object value() {
		return value;
	}

	Object makeValue(int variant) throws Throwable {
		if (value != null)
			return value;

		MethodHandle h = MethodHandles.lookup().unreflectConstructor(
			ctors.get(variant)
		);

		Object[] pReqs = ctorArgs.row(
			variant
		).values().toArray();
		Object[] args = new Object[pReqs.length];
		for (int pos = 0; pos < pReqs.length; pos++) {
			ClassRequirement cr = (ClassRequirement)pReqs[pos];
			Unit ru = cr.getReferred();

			if (ru == null || ru.value() == null)
				throw new IllegalStateException(
					"uresolved dependency in unit " + this
				);

			args[pos] = ru.value();
		}

		value = h.invokeWithArguments(args);
		return value;
	}

	final Class<?> cls;
	private final ImmutableList<Constructor<?>> ctors;
	private final ImmutableTable<
		Integer, Integer, ClassRequirement
	> ctorArgs;
	private Object value;
}
