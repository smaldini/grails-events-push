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
            that.binary = hasOptions && options.binary && (typeof options.binary == "boolean") ? options.binary : false;

            var loadingRequest = {};
            var state = grails.Events.CONNECTING;
            that.onopen = null;
            that.onglobalmessage = null;
            that.onclose = null;
            var handlerMap = {};

            var localId = "";

            that.send = function (topic, message) {
                checkSpecified("topic", 'string', topic);
                //checkSpecified("message", 'object', message);
                //checkOpen();
                var envelope;
                if (that.binary && (message instanceof ArrayBuffer || message instanceof Blob)) {
                    envelope = appendBuffer(stringToUint8(topic + '|' + '2'), message);
                    that.globalTopicSocket.push(envelope);
                } else {
                    envelope = topic + '|' + (typeof (message) === 'object' ?
                        '1' + '|' + jQuery.stringifyJSON(message) :
                        '0' + '|' + message)
                    if (!that.binary) {
                        that.globalTopicSocket.push({data: envelope});
                    } else {
                        that.globalTopicSocket.push(stringToUint8(envelope).buffer);
                    }
                }
                return envelope;
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
                        url: that.root + '/' + that.path + '/' + that.globalTopicName,
                        transport: "websocket",
                        contentType: "application/json",
                        fallbackTransport: "streaming",
                        reconnectInterval: 4000
                    };

                    if (that.binary && !!window.ArrayBuffer) {
                        rq.webSocketBinaryType = "arraybuffer"
                        rq.headers = { "X-Atmosphere-Binary" : true };
                    }

                    if (!!window.EventSource) {
                        rq.fallbackTransport = 'sse';
                    }

                    // Allow the user to extend/override the request
                    rq = jQuery.extend(true, rq, options);
                    rq = jQuery.extend(true, rq, request);

                    rq.onTransportFailure = function (e, r) {
                        rq.onOpen = null;
                        r.headers = {
                            topics: Object.keys(handlerMap)
                        };
                        that.binary = false;
                    };

                    rq.onOpen = function (response) {
                        rq.onOpen = null;
                        for (var val in handlerMap) {
                            console.log("defer connecting topic: " + val);
                            _registerRequest(val);
                        }
                        loadingRequest = null;
                    };
                    loadingRequest = rq;
                    that.globalTopicSocket = socket.subscribe(rq);
                } else {
                    if (loadingRequest == null) {
                        console.log("connecting topic: " + topic);
                        _registerRequest(topic);
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

            function _registerRequest(topic) {
                if (!that.binary) {
                    that.globalTopicSocket.push(topic + '|' + "4");
                } else {
                    that.globalTopicSocket.push(stringToUint8(topic + '|' + "4").buffer);
                }
            }

            function init() {
                var connecting = function () {
                    state = grails.Events.OPEN;
                    if (that.onopen) {
                        that.onopen();
                    }
                };

                socket.onOpen = connecting;
                socket.onClose = function (e) {
                    state = grails.Events.CLOSED;
                    if (that.onclose) {
                        that.onclose();
                    }
                };

                var buffer;

                socket.onMessage = function (response) {
                    if (response.status == 200) {
                        var topic, type, data, envelope;
                        try {
                            if (response.responseBody instanceof ArrayBuffer) {
                                var dataUint = new Uint8Array(response.responseBody);
                                var endPacket = dataUint.length >= 3 &&
                                    "E".charCodeAt(0) == dataUint[dataUint.length - 3] &&
                                    "N".charCodeAt(0) == dataUint[dataUint.length - 2] &&
                                    "D".charCodeAt(0) == dataUint[dataUint.length - 1];

                                if (dataUint.length > 0 && !endPacket) {
                                    if (buffer == null) {
                                        buffer = response.responseBody;
                                    } else {
                                        var tmp = new Uint8Array(dataUint.length + buffer.byteLength);
                                        tmp.set(new Uint8Array(buffer), 0);
                                        tmp.set(dataUint, buffer.byteLength);
                                        buffer = tmp.buffer;
                                    }
                                }

                                if (!endPacket) {
                                    return;
                                }

                                if (!buffer || buffer.byteLength == 0) {
                                    buffer = null;
                                    return;
                                }

                                var bytes = new Uint8Array(buffer);
                                var tmp = "";
                                for (var i = 0; i < bytes.length; i++) {
                                    if (bytes[i] == "|".charCodeAt(0)) {
                                        if (tmp.length > 0) {
                                            if (!topic) {
                                                topic = tmp;
                                                tmp = "";
                                            } else if (!type) {
                                                type = tmp;
                                                data = bytes.buffer.slice(i + 1);
                                                break;
                                            }
                                        }
                                    } else {
                                        tmp += String.fromCharCode(bytes[i]);
                                    }
                                }
                                buffer = null;

                            } else if (response.responseBody.length > 0) {
                                var _data = response.responseBody.split('|')
                                topic = _data[0];
                                type = _data[1];
                                data = _data[2];
                            }
                        } catch (e) {
                            if (console != 'undefined') {
                                console.log(e.stack);
                                console.log('discarded message: ' + response.responseBody);
                                buffer = null;
                            }
                            return;
                        }
                        var handlers = handlerMap[topic ? topic : that.globalTopicName];
                        if (handlers) {
                            if (type == '5') {
                                that.unregisterHandlers(topic);
                                return;
                            }
                            // We make a copy since the handler might get unregistered from within the
                            // handler itself, which would screw up our iteration
                            var copy = handlers.slice(0);
                            if (type == '0' || type == '2') {
                                envelope = data;
                            } else if (type == '1') {
                                envelope = jQuery.parseJSON(
                                    data instanceof ArrayBuffer ?
                                        String.fromCharCode.apply(null, new Uint8Array(data)) :
                                        data);
                            }
                            for (var i = 0; i < copy.length; i++) {
                                copy[i](envelope, topic, response, type);
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

            function stringToUint8(str) {
                str = str + '|';
                var uint = new Uint8Array(str.length);
                for (var i = 0, j = str.length; i < j; ++i) {
                    uint[i] = str.charCodeAt(i);
                }
                return uint;
            }

            function appendBuffer(uint, buffer) {
                var tmp = new Uint8Array(uint.length + buffer.byteLength);
                tmp.set(uint, 0);
                tmp.set(new Uint8Array(buffer), uint.length);
                return tmp.buffer;
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