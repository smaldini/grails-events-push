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
        grails.Events = function (root, options) {

            var that = this;
            var socket = $.atmosphere;

            that.root = (root && (typeof root == "string")) ? root : (window.location.protocol + '//' + window.location.hostname + ':' + window.location.port);
            var hasOptions = (options && (typeof options == "object"));

            that.globalTopicName = hasOptions && options.globalTopicName && (typeof options.globalTopicName == "string") ? options.globalTopicName : "eventsbus";
            that.path = hasOptions && options.path && (typeof options.path == "string") ? option.path : "g-eventsbus";

            var loadingRequest = {};
            var state = grails.Events.CONNECTING;
            that.onopen = null;
            that.onglobalmessage = null;
            that.onclose = null;
            var handlerMap = {};

            var localId = "";

            that.send = function (topic, message, raw) {
                checkSpecified("topic", 'string', topic);
                //checkSpecified("message", 'object', message);
                //checkOpen();
                var envelope = {
                    topic: topic,
                    body: message
                };
                that.globalTopicSocket.push(raw ? message : {data: jQuery.stringifyJSON(envelope)});
            };

            that.on = function (topic, handler, request) {
                checkSpecified("topic", 'string', topic);
                checkSpecified("handler", 'function', handler);

                var subscribe = jQuery.isEmptyObject(handlerMap) || !jQuery.isEmptyObject(request);
                var handlers = handlerMap[topic];
                if (handlers) {
                    handlers[handlers.length] = handler;
                } else {
                    handlerMap[topic] = [handler];
                }
                if (subscribe) {
                    var topics = "";
                    for (var _topic in handlerMap) {
                        topics += _topic + ',';
                    }
                    //request.shared = true;
                    var rq = {
                        trackMessageLength: true,
                        url: that.root + '/' + that.path + '/' + that.globalTopicName,
                        transport: "websocket",
                        contentType: "application/json",
                        fallbackTransport: "streaming",
                        reconnectInterval: 4000
                    };

                    if (!!window.ArrayBuffer){
                        rq.binaryType = "arraybuffer"
                    }

                    if (!!window.EventSource) {
                        rq.fallbackTransport = 'sse';
                    }

                    // Allow the user to extend/override the request
                    rq = jQuery.extend(true, rq, options);
                    rq = jQuery.extend(true, rq, request);

                    rq.onOpen = function (response) {
                        rq.onOpen = null;
                        for(var val in handlerMap){
                            console.log("defer connecting topic: " + val);
                            that.globalTopicSocket.push({data: jQuery.stringifyJSON({topic: val})});
                        }
                        loadingRequest = null;
                    };
                    loadingRequest = rq;
                    that.globalTopicSocket = socket.subscribe(rq);
                } else {
                    if(loadingRequest == null){
                        console.log("connecting topic: " + topic);
                        that.globalTopicSocket.push({data: jQuery.stringifyJSON({topic: topic})});
                    }
                }


                return handler;
            };

            that.unregisterHandlers = function (topic) {
                checkSpecified("topic", 'string', topic);
                delete handlerMap[topic];
                socket.unsubscribeUrl(that.root + '/' + that.path + '/' + that.globalTopicName);
                init();
            };

            that.unregisterHandler = function (topic, handler) {
                checkSpecified("topic", 'string', topic);
                checkSpecified("handler", 'function', handler);
                checkOpen();
                var handlers = handlerMap[topic];
                if (handlers) {
                    var idx = handlers.indexOf(handler);
                    if (idx != -1) handlers.splice(idx, 1);
                    if (handlers.length == 0) {
                        that.unregisterHandlers(topic);
                    }
                }
            };

            that.close = function () {
                state = grails.Events.CLOSING;
                socket.unsubscribe();
            };

            that.readyState = function () {
                return state;
            };

            function init() {
                var connecting = function () {
                    state = grails.Events.OPEN;
                    if (that.onopen) {
                        that.onopen();
                    }
                };

                socket.onOpen = connecting;
                socket.onReconnect = connecting;

                socket.onClose = function (e) {
                    state = grails.Events.CLOSED;
                    if (that.onclose) {
                        that.onclose();
                    }
                };

                socket.onMessage = function (response) {
                    if (response.status == 200) {
                        var data;
                        if (response.responseBody.length > 0) {
                            try {
                                data = jQuery.parseJSON(response.responseBody);
                            } catch (e) {
                                if (console != 'undefined') {
                                    console.log('discarded message: ' + response.responseBody);
                                }
                                return;
                            }
                            var handlers = handlerMap[data && data.topic ? data.topic : that.globalTopicName];
                            if (handlers) {
                                // We make a copy since the handler might get unregistered from within the
                                // handler itself, which would screw up our iteration
                                var copy = handlers.slice(0);
                                for (var i = 0; i < copy.length; i++) {
                                    copy[i](data.body, data, response);
                                }
                            }
                        }
                    }
                };

                that.on(that.globalTopicName, function (data, e) {
                    if (that.onglobalmessage) {
                        that.onglobalmessage(data);
                    }
                });
            }

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

            init();

        };

        grails.Events.CONNECTING = 0;
        grails.Events.OPEN = 1;
        grails.Events.CLOSING = 2;
        grails.Events.CLOSED = 3;

    }
})();