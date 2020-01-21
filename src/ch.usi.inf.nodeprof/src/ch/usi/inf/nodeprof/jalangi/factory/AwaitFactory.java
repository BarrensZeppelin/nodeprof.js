/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.CFBranchEventHandler;

public class AwaitFactory extends AbstractFactory {

    public AwaitFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post) {
        super("await", jalangiAnalysis, pre, post, 2, 4);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new CFBranchEventHandler(context) {
            @Child DirectCallNode preCall = createDirectCallNode(pre);
            @Child DirectCallNode postCall = createDirectCallNode(post);

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (!this.isAwaitNode()) {
                    return;
                }
                if (inputs == null || inputs.length == 0) {
                    return;
                }
                if (pre != null) {
                    if (inputs[0] == inputs[1] && JSPromise.isJSPromise(inputs[0])) {
                        // await some promise
                        setPreArguments(0, getSourceIID());
                        setPreArguments(1, assertGetInput(0, inputs, "awaited val"));
                        directCall(preCall, true, getSourceIID());
                    } else if (inputs[0] != inputs[1] && JSPromise.isJSPromise(inputs[1])) {
                        // await some value, and inputs[1] is the internal promise
                        setPreArguments(0, getSourceIID());
                        setPreArguments(1, assertGetInput(0, inputs, "awaited val"));
                        directCall(preCall, true, getSourceIID());
                    }
                }
                if (post != null) {
                    if (inputs[0] != inputs[1] && !JSPromise.isJSPromise((inputs[1]))) {
                        // await some promise
                        setPostArguments(0, this.getSourceIID());
                        // TODO, inputs[0] should never be null
                        setPostArguments(1, inputs[0] == null ? Undefined.instance : inputs[0]);
                        setPostArguments(2, assertGetInput(1, inputs, "awaited ret"));
                        setPostArguments(3, inputs[0] != null && JSPromise.isJSPromise(inputs[0]) && JSPromise.isRejected((DynamicObject) inputs[0]));

                        directCall(postCall, false, getSourceIID());
                    } else if (inputs[0] == inputs[1] && !JSPromise.isJSPromise(inputs[0])) {
                        // await some value
                        setPostArguments(0, this.getSourceIID());
                        setPostArguments(1, assertGetInput(0, inputs, "awaited val"));
                        setPostArguments(2, assertGetInput(0, inputs, "awaited ret"));
                        setPostArguments(3, false);

                        directCall(postCall, false, getSourceIID());
                    }
                }
            }

            @Override
            public void executeExceptionalCtrlFlow(VirtualFrame frame, Throwable exception, Object[] inputs) {
            }
        };
    }
}
