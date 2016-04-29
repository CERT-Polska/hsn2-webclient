/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
 *
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.wrappers;

import java.util.Map;

public class CookieWrapper {

    private String name;
    private String value;
    private Map<String, String> attributes;

    public CookieWrapper(String name, String value, Map<String, String> attributes) {
        this.name = name;
        this.value = value;
        this.attributes = attributes;
    }

    public final String getName() {
        return name;
    }

    public final String getValue() {
        return value;
    }

    public final Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public final String toString() {
		return "CookieWrapper{name=" + name + ",value=" + value + "}";
    }
}
