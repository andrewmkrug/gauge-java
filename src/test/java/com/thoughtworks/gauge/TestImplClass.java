// Copyright 2015 ThoughtWorks, Inc.

// This file is part of Gauge-Java.

// Gauge-Java is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Gauge-Java is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Gauge-Java.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge;

class TestImplClass {
    @Step("hello world")
    public void helloWorld() {

    }

    @Step("hello world <param0>")
    public int helloWorld(int i) {
        return 0;
    }

    @Step("a step with <param0> and <table>")
    public Table helloWorld(String a, Table table) {
        return null;
    }

    @Step({"first step name with name <a>", "second step name with <b>"})
    public Table aliasMethod(String a) {
        return null;
    }
}
