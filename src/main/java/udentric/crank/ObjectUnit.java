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

import java.util.List;

class ObjectUnit implements Unit {
	ObjectUnit(Object value_) {
		value = value_;
	}

	@Override
	public void collectOfferings(OfferingSet offSet) {
		Unit.collectOfferings(offSet, value.getClass(), this);
	}

	@Override
	public void collectRequirements(List<Requirement> rl) {
	}

	@Override
	public int selectVariant(OfferingSet offSet) {
		return 0;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object obtainValue(int pos) {
		return value;
	}

	private final Object value;
}
