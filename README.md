grails-events-push
==================

Events-push is a client-side events bus based on the superb portable push library [Atmosphere](https://github.com/Atmosphere/atmosphere)  and [Grails platform-core](https://github.com/grailsrocks/grails-platform-core) plugin for events
propagation/listening. It simply allows your client to listen to server-side events and push data. It uses WebSockets by default
and falls back to use Comet if required (server not compliant, browser too old...).
Events-push is a white-list broadcaster (client-side events scope is 'browser'). You will need to define which events can be
 propagated to the server by modifying the Events DSL to use 'browser' scope. To register listeners from client, you will need to
 define them as well. Ie:

MyEvents.groovy >

```groovy
events = {    
    'savedTodo' namespace: 'browser', browser:true // allows browser push on this topic
}
```


MyService.groovy >
```groovy
//will receive client events from 'saveTodo' topic
@Listener(namespace='browser') saveTodo(Map data){
  //...
  event([namespace: 'browser', topic: 'savedTodo', data: data]) // will trigger registered browsers on 'savedTodo' topic
}
```

someView.gsp >
```gsp
<r:require module="grailsEvents"/>
<r:script>
 var grailsEvents = new grails.Events("http://localhost:8080/app/");
 grailsEvents.send('saveTodo', data); //will send data to server topic 'saveTodo'
 grailsEvents.on('savedTodo', function(data){...}); //will listen for server events on 'savedTodo' topic
</r:script>
```

You can find a full sample application using [Events-si](https://github.com/smaldini/grails-events-si), RabbitMQ, BackboneJS, coffeescript and CloudFoundry in

Wildcard Topics
---------------

In addition to single event names such as 'savedTodo', Events Push supports wildcard events that allow for restricting which browsers get the corresponding events without needing to create multiple event definitions:

Config.groovy >
```groovy
events = {
  'chat-*' namespace: 'browser', browser:true
}
```

view.gsp >
```gsp
<r:require module="grailsEvents"/>
<r:script>
  var grailsEvents = new grails.Events("http://localhost:8080/app/");
  var chatRoomId = '42';
  grailsEvents.on('chat-' + chatRoomId, function(data){...}); //will listen for server events for only this chatroom
</r:script>
```

MyService.groovy >
```groovy
void sendChatMessage(String chatMessage, String chatRoomId) {
  event([namespace: 'browser', topic: "chat-${chatRoomId}", data: [message: chatMessage]) // send the message to only browsers registered for this chatroom
}
```

Embedded Tomcat Configuration
-----------------------------

To configure the Grails embedded Tomcat container used for development and testing to support non-blocking IO for websockets, add the following line to your BuildConfig.groovy and set the servlet version to 3.0:

BuildConfig.groovy >
```groovy
grails.servlet.version = "3.0"
grails.tomcat.nio = true
```

Customizing Atmosphere Configuration
------------------------------------

The Atmosphere library has many configuration options that can be customized by adding values to your Config.groovy. For a full list of available Atmosphere configuration options, see [Atompshere config API](http://atmosphere.github.com/atmosphere/apidocs/org/atmosphere/cpr/ApplicationConfig.html)

Config.groovy customization example >
```groovy
events.push.servlet.initParams = [
    "org.atmosphere.useNative": "true",
    "org.atmosphere.cpr.CometSupport.maxInactiveActivity": "100"
]
```

Example Project
---------------

You can find a full sample using [Events-si](https://github.com/smaldini/grails-events-si), RabbitMQ, BackboneJS, coffeescript and CloudFoundry in
[Grails Todo repository](https://github.com/smaldini/grailsTodos).
