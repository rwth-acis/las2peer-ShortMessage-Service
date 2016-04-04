(function() {
  var api, fetchEntries, initEvents, iwcCallback, iwcManager, login, smsLib, requestSender, sendMessage, autorefresh;

  api = i5.las2peer.jsAPI;

  smsLib = i5.las2peer.sms;

  /*
    Check explicitly if gadget is known, i.e. script is executed in widget environment.
    Allows compatibility as a Web page.
   */

  if (typeof gadgets !== "undefined" && gadgets !== null) {
    iwcCallback = function(intent) {};
    iwcManager = new api.IWCManager(iwcCallback);
  }

  login = new api.Login(api.LoginTypes.HTTP_BASIC);

  login.setUserAndPassword(clientDefaults.userName, clientDefaults.userPassword);


  /*
    Init RequestSender object with uri and login data.
   */

  requestSender = new api.RequestSender(clientDefaults.address, login);

  $(document).ready(function() {
    initMessageSelector();
  });

  initMessageSelector = function() {
    initEvents();
    fetchEntries();
  };

  initEvents = function() {
    $("#refreshMessages").click(function() {
      if (typeof autorefresh == "undefined" || autorefresh == null) {
        // enable auto message refresh
        autorefresh = window.setInterval(function() { fetchEntries(); }, 250);
        $("#refreshMessages").css({"color":"#0f0"});
      } else {
        // disable auto message refresh
        window.clearInterval(autorefresh);
        autorefresh = null;
        $("#refreshMessages").css({"color":"#f00"});
      }
    });
    $("#text").focus(function() {
      $("#hint").html("");
    });
    $("#text").on('input', function() {
      $("#hint").html("");
    });
    $("#send").click(function() {
      sendMessage();
    });
    $("#text").focus();
  };


  /*
    Request service for messages.
   */

  fetchEntries = function() {
//    $("#content").val("loading...");
    requestSender.sendRequest("get", "getShortMessagesAsString", "", function(data) {
        $("#content").val(data);
        console = document.getElementById("content");
        console.scrollTop = console.scrollHeight;
    }, function(error) {
        // disable auto message refresh
        if (typeof autorefresh != "undefined" && autorefresh != null) {
          window.clearInterval(autorefresh);
          autorefresh = null;
          $("#refreshMessages").css({"color":"#f00"});
        }
        $("#content").val(error);
        alert(error);
    });
  };

  sendMessage = function() {
    agent = document.getElementById("agent");
    text = document.getElementById("text");
    requestSender.sendRequest("get", "sendShortMessage/"+agent.value+"/"+encodeURIComponent(text.value), "", function(data) {
        $("#hint").html(data);
    }, function(error) {
        $("#hint").html(error);
        alert(error);
    });
    text.value = '';
    text.focus();
  };

}).call(this);
