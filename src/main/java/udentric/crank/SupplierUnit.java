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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

class SupplierUnit implements Unit {
	static ArrayList<Unit> inspectSupplierMethods(
		Object sup
	) throws ReflectiveOperationException {
		HashMap<Class<?>, ArrayList<Method>> supMap = new HashMap<>();

		Method[] mm = sup.getClass().getMethods();
		for (Method m: mm) {
			CrankSupplier cs = m.getAnnotation(
				CrankSupplier.class
			);
			if (cs == null)
				continue;

			if (m.getParameterCount() < 1)
				continue;

			supMap.compute(m.getReturnType(), (k, v) -> {
				if (v == null)
					v = new ArrayList<>();

				v.add(m);
				return v;
			});
		}

		ArrayList<Unit> rv = new ArrayList<>();
		for (Map.Entry<
			Class<?>, ArrayList<Method>
		> entry: supMap.entrySet()) {
			rv.add(new SupplierUnit(
				sup, entry.getKey(), entry.getValue()
			));
		}

		return rv;
	}

	private SupplierUnit(
		Object sup_, Class<?> valueClass_, ArrayList<Method> mm
	) throws ReflectiveOperationException {
		sup = sup_;
		valueClass = valueClass_;

		Collections.sort(mm, Collections.reverseOrder(
			Comparator.comparingInt(
				Method::getParameterCount
			)
		));

		ImmutableList.Builder<
			MethodHandle
		> lb = ImmutableList.builder();
		ImmutableTable.Builder<
			Integer, Integer, ClassRequirement
		> tb = ImmutableTable.builder();

		MethodHandles.Lookup l = MethodHandles.lookup();
		int row = 0;
		for (Method m: mm) {
			MethodHandle h = l.unreflect(m);
			lb.add(h.bindTo(sup));

			int col = 0;
			for (Parameter p: m.getParameters()) {
				tb.put(
					row, col,
					new ClassRequirement(p.getType())
				);
				col++;
			}
			row++;
		}

		ctors = new MethodSet(lb.build(), tb.build());
	}

	@Override
	public void collectOfferings(OfferingSet offSet) {
		Unit.collectOfferings(offSet, valueClass, this);
	}

	@Override
	public void collectRequirements(List<Requirement> rl) {
		ctors.collectRequirements(rl);
	}

	@Override
	public int selectVariant(OfferingSet offSet) {
		return ctors.selectVariant(offSet);
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object obtainValue(int variant) throws Throwable {
		if (value != null)
			return value;

		value = ctors.make(variant);
		if (value == null)
			throw new IllegalStateException(
				"uresolved dependency in unit " + this
			);

		return value;
	}

	private final Object sup;
	private final MethodSet ctors;
	private final Class<?> valueClass;
	private Object value;
}
