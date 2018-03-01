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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ClassUnit implements Unit {
	ClassUnit(Class<?> cls_) throws ReflectiveOperationException {
		if (cls_.isArray() || null == cls_.getSuperclass())
			throw new IllegalArgumentException(String.format(
				"Class %s is not an object class", cls_
			));

		cls = cls_;
		value = null;

		ImmutableList.Builder<
			MethodHandle
		> lb = ImmutableList.builder();
		ImmutableTable.Builder<
			Integer, Integer, ClassRequirement
		> tb = ImmutableTable.builder();

		MethodHandles.Lookup l = MethodHandles.lookup();
		int row = 0;
		for (Constructor<?> ctor: sortConstructors()) {
			lb.add(l.unreflectConstructor(ctor));
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

		ctors = new MethodSet(lb.build(), tb.build());
	}

	@Override
	public void collectOfferings(OfferingSet offSet) {
		Unit.collectOfferings(offSet, cls, this);
	}

	@Override
	public void collectRequirements(List<Requirement> rl) {
		ctors.collectRequirements(rl);
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

	private final Class<?> cls;
	private final MethodSet ctors;
	private Object value;
}
