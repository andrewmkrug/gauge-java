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

package com.thoughtworks.gauge.processor;

import com.thoughtworks.gauge.execution.MethodExecutor;
import com.thoughtworks.gauge.SpecificationInfo;
import gauge.messages.Messages;
import gauge.messages.Spec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MethodExecutionMessageProcessor {

    public Messages.Message execute(Method method, Messages.Message message, Object... args) {
        HashSet<Method> methods = new HashSet<Method>();
        methods.add(method);
        return execute(methods, message, args);
    }

    public Messages.Message execute(Set<Method> methods, Messages.Message message, Object... args) {
        MethodExecutor methodExecutor = new MethodExecutor();
        long totalExecutionTime = 0;
        for (Method method : methods) {
            Spec.ProtoExecutionResult result = methodExecutor.execute(method, args);
            totalExecutionTime += result.getExecutionTime();
            if (result.getFailed()) {
                return createMessageWithExecutionStatusResponse(message, result);
            }
        }

        Spec.ProtoExecutionResult passingExecution = Spec.ProtoExecutionResult.newBuilder().setFailed(false).setExecutionTime(totalExecutionTime).build();
        return createMessageWithExecutionStatusResponse(message, passingExecution);
    }

    public Messages.Message executeHooks(Set<Method> hooks, Messages.Message message, SpecificationInfo executionInfo) {
        MethodExecutor methodExecutor = new MethodExecutor();
        Spec.ProtoExecutionResult result;
        long totalHooksExecutionTime = 0;
        for (Method method : hooks) {
            if (methodHasArguments(method, executionInfo)) {
                result = methodExecutor.execute(method, executionInfo);
            } else {
                result = methodExecutor.execute(method);
            }
            totalHooksExecutionTime += result.getExecutionTime();
            if (result.getFailed()) {
                Spec.ProtoExecutionResult failureResult = Spec.ProtoExecutionResult.newBuilder(result).setExecutionTime(totalHooksExecutionTime).build();
                return createMessageWithExecutionStatusResponse(message, failureResult);
            }
        }

        Spec.ProtoExecutionResult passingExecution = Spec.ProtoExecutionResult.newBuilder().setFailed(false).setExecutionTime(totalHooksExecutionTime).build();
        return createMessageWithExecutionStatusResponse(message, passingExecution);
    }

    private boolean methodHasArguments(Method method, Object... arg) {
        if (method.getParameterTypes().length != arg.length) {
            return false;
        }
        List<Class> argsClassList = createClassList(arg);
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean isValid = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!(argsClassList.indexOf(parameterTypes[i]) == i)) {
                isValid = false;
            }
        }
        return isValid;
    }

    private List<Class> createClassList(Object[] objects) {
        ArrayList<Class> classes = new ArrayList<Class>();
        for (Object obj : objects) {
            classes.add(obj.getClass());
        }
        return classes;
    }


    public Messages.Message createMessageWithExecutionStatusResponse(Messages.Message receivedMessage, Spec.ProtoExecutionResult result) {
        return Messages.Message.newBuilder()
                .setMessageId(receivedMessage.getMessageId())
                .setMessageType(Messages.Message.MessageType.ExecutionStatusResponse)
                .setExecutionStatusResponse(Messages.ExecutionStatusResponse.newBuilder().setExecutionResult(result).build())
                .build();
    }
}
