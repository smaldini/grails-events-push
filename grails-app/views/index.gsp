<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
  <r:require modules="grailsEvents"/>
  <meta name='layout' content='main'/>
  <r:script>
    $(document).ready(function () {

      /*
       Register a grailsEvents handler for this window, constructor can take a root URL,
       a path to event-bus servlet and options. There are sensible defaults for each argument
       */
      var grailsEvents = new grails.Events("${createLink(uri:'')}", {logLevel:"debug", transport:"sse"});
      grailsEvents.on("afterInsert", function (data) {
          $("#messages").append("<div>" + $.stringifyJSON(data) + "</div>");
        });

      /*
       Add a listener for the topic from first input + a listener on afterInsert topic.
       */
      function subscribe() {
        /*
         Adding a listener requires a topic to listen and a function handler to react, data should be a JSON object
         */
        grailsEvents.on($('#topic').val(), function (data) {
          $("#messages").append("<div>" + data.message + "</div>")
        });
      }

      function getKeyCode(ev) {
        if (window.event) return window.event.keyCode;
        return ev.keyCode;
      }

      function connect() {
        $('#phrase').val('');
        $('#sendMessage').attr('class', '');
        $('#phrase').focus();
        subscribe();
        $('#connect').val("Switch transport");
      }

      $('#connect').click(function (event) {
        if ($('#topic').val() == '') {
          alert("Please enter a PubSub topic to subscribe");
          return;
        }
        connect();
      });

      $('#topic').keyup(function (event) {
        $('#sendMessage').attr('class', 'hidden');
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {
          connect();
          return false;
        }
      });

      $('#phrase').attr('autocomplete', 'OFF');
      $('#phrase').keyup(function (event) {
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {
          /*
           Sending an event to server (will be also to local in a future release).
           Requires the event topic to be used, and a JSON object.
           */

          grailsEvents.send($('#topic').val(), {message:$('#phrase').val()});
          $('#phrase').val('');
          return false;
        }
        return true;
      });

      $('#send_message').click(function (event) {
        if ($('#topic').val() == '') {
          alert("Please enter a message to publish");
          return;
        }
        /*
         Same sending method than above but using click button.
         */
        grailsEvents.send($('#topic').val(), {message:$('#phrase').val()});
        $('#phrase').val('');
        return false;
      });


      $('#topic').focus();
    });
  </r:script>
</head>

<body>
<h2>Select topic to subscribe</h2>

<div id='pubsub'>
  <input id='topic' type='text' value="sampleBro"/>
</div>

<input id='connect' class='button' type='submit' name='connect' value='Connect'/>
<br/>

<h2 id="s_h" class='hidden'>Publish Topic</h2>

<div id='sendMessage' class='hidden'>
  <input id='phrase' type='text'/>
  <input id='send_message' class='button' type='submit' name='Publish' value='Publish Message'/>
</div>
<br/>

<h2>Real Time PubSub Update</h2>
<ul id='messages'></ul>
</body>
</html>