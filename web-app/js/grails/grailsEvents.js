/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2012, Stephane Maldini - adapted from vertx.io EventBus.js library to use atmosphere & events-push grails
 * plugin.
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 */
var grails = grails || {};
(function () {
    if (!grails.Events) {
        grails.Events = function (root, path, options) {

            var that = this;
            var socket = $.atmosphere;

            that.root = (root && (typeof root == "string")) ? root : (window.location.protocol + '//' + window.location.hostname + ':' + window.location.port);
            that.path = (path && (typeof path == "string")) ? path : "g-eventsbus";

            var hasOptions = (options && (typeof options == "object"));
            that.globalTopicName = hasOptions && options.globalTopicName && (typeof options.globalTopicName == "string") ? options.globalTopicName : "events-push-topic";
            that.transport = hasOptions && options.transport && (typeof options.transport == "string") ? options.transport : "websocket";


            var state = grails.Events.CONNECTING;
            that.onopen = null;
            that.onglobalmessage = null;
            that.onclose = null;
            var handlerMap = {};

            that.send = function (topic, message) {
                checkSpecified("topic", 'string', topic);
                checkSpecified("message", 'object', message);
                checkOpen();
                var envelope = {
                    topic:topic,
                    body:message
                };
                that.globalTopicSocket.push({data:$.stringifyJSON(envelope)});
            };

            that.on = function (topic, handler, init) {
                checkSpecified("topic", 'string', topic);
                checkSpecified("handler", 'function', handler);

                var request = {};

                if (init) {
                    socket.onOpen = function () {
                        state = grails.Events.OPEN;
                        if (that.onopen) {
                            that.onopen();
                        }
                    };
                    request.onClose = function () {
                        state = grails.Events.CLOSED;
                        if (that.onclose) {
                            that.onclose();
                        }
                    };
                } else {
                    checkOpen();
                }

                var handlers = handlerMap[topic];
                if (!handlers) {
                    handlers = [handler];
                    handlerMap[topic] = handlers;

                    request.url = that.root + '/' + that.path + '/' + topic;
                    request.transport = that.transport;
                    request.onMessage = function (response) {
                        if (response.status == 200) {
                            var data = {};
                            if (response.responseBody.length > 0) {
                                data = $.parseJSON(response.responseBody).body;
                                var handlers = handlerMap[topic];
                                if (handlers) {
                                    // We make a copy since the handler might get unregistered from within the
                                    // handler itself, which would screw up our iteration
                                    var copy = handlers.slice(0);
                                    for (var i = 0; i < copy.length; i++) {
                                        copy[i](data);
                                    }
                                }
                            }
                        }
                    };
                    return socket.subscribe(request);
                } else {
                    handlers[handlers.length] = handler;
                }
            };

            that.globalTopicSocket = that.on(that.globalTopicName, function (data) {
                if (that.onglobalmessage) {
                    that.onglobalmessage(data);
                }
            }, true);

            that.unregisterHandler = function (topic, handler) {
                checkSpecified("topic", 'string', topic);
                checkSpecified("handler", 'function', handler);
                checkOpen();
                var handlers = handlerMap[topic];
                if (handlers) {
                    var idx = handlers.indexOf(handler);
                    if (idx != -1) handlers.splice(idx, 1);
                    if (handlers.length == 0) {
                        // No more local handlers so we should unregister the connection
                        that.socket.unsubscribeUrl(that.root + '/' + that.path + '/' + topic);
                        delete handlerMap[topic];
                    }
                }
            };

            that.close = function () {
                checkOpen();
                state = grails.Events.CLOSING;
                socket.unsubscribe();
            };

            that.readyState = function () {
                return state;
            };

            function checkOpen() {
                if (state != grails.Events.OPEN) {
                    throw new Error('INVALID_STATE_ERR');
                }
            }

            function checkSpecified(paramName, paramType, param, optional) {
                if (!optional && !param) {
                    throw new Error("Parameter " + paramName + " must be specified");
                }
                if (param && typeof param != paramType) {
                    throw new Error("Parameter " + paramName + " must be of type " + paramType);
                }
            }

        }
        ;

        grails.Events.CONNECTING = 0;
        grails.Events.OPEN = 1;
        grails.Events.CLOSING = 2;
        grails.Events.CLOSED = 3;

    }
})();