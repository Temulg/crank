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
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

class Unit {
	Unit(
		Class<?> cls_, Constructor<?> ctor, DependencyTracker deps
	) throws ReflectiveOperationException {
		cls = cls_;
		Lookup l = MethodHandles.lookup();
		constructor = l.unreflectConstructor(ctor);

		start = findLifecycleMethod(l, "start");
		stop = findLifecycleMethod(l, "stop");

		constructorArgs  = ctor.getParameterTypes();
		for (Class<?> p: constructorArgs)
			deps.addDependency(this, p);
	}

	private MethodHandle findLifecycleMethod(Lookup l, String name) {
		try {
			return l.findVirtual(
				cls, name, MethodType.methodType(void.class)
			);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	final Class<?> cls;
	final MethodHandle constructor;
	final MethodHandle start;
	final MethodHandle stop;
	final Class<?>[] constructorArgs;
}
