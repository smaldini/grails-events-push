<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
  <r:require modules="grailsEvents"/>
  <meta name='layout' content='main'/>
  <r:script>
    $(document).ready(function () {

      var grailsEvents = new grails.Events(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + "/events-push");

      function getKeyCode(ev) {
        if (window.event) return window.event.keyCode;
        return ev.keyCode;
      }

      function getElementById() {
        return document.getElementById(arguments[0]);
      }

      function subscribe() {
        grailsEvents.on(getElementById('topic').value, function (data) {
          $("ul").append("<div>" + data.message + "</div>")
        });
        grailsEvents.on("afterInsert", function (data) {
          $("ul").append("<div>" + data.id + "</div>");
        });
      }

      function getElementByIdValue() {
        detectedTransport = null;
        return document.getElementById(arguments[0]).value;
      }

      function unsubscribe() {
        grailsEvents.close();
      }

      function connect() {
        getElementById('phrase').value = '';
        getElementById('sendMessage').className = '';
        getElementById('phrase').focus();
        subscribe();
        getElementById('connect').value = "Switch transport";
      }

      getElementById('connect').onclick = function (event) {
        if (getElementById('topic').value == '') {
          alert("Please enter a PubSub topic to subscribe");
          return;
        }
        connect();
      }

      getElementById('topic').onkeyup = function (event) {
        getElementById('sendMessage').className = 'hidden';
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {
          connect();
          return false;
        }
      }

      getElementById('phrase').setAttribute('autocomplete', 'OFF');
      getElementById('phrase').onkeyup = function (event) {
        var keyc = getKeyCode(event);
        if (keyc == 13 || keyc == 10) {

          grailsEvents.send(getElementById('topic').value, {message:getElementByIdValue('phrase')});

          getElementById('phrase').value = '';
          return false;
        }
        return true;
      };

      getElementById('send_message').onclick = function (event) {
        if (getElementById('topic').value == '') {
          alert("Please enter a message to publish");
          return;
        }


        grailsEvents.send(getElementById('topic').value, {message:getElementByIdValue('phrase')});

        getElementById('phrase').value = '';
        return false;
      };


      getElementById('topic').focus();
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
<ul></ul>
</body>
</html>