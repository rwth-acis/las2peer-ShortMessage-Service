// private properties
var LAS2PEERHOST = "http://localhost:9080/";
var LAS2PEERSERVICENAME = "i5.las2peer.services.shortMessageService.ShortMessageService";
var LAS2peerClient;

/**
* gets called when the window loads 
*/
window.onload = function() 
{
	LAS2peerClient = new LasAjaxClient(LAS2PEERSERVICENAME, function(statusCode, message) 
	{
		switch(statusCode) 
		{
			// this function is called whenever the login/connection/invocation status of the LAS2peer client changes or if an error occurs
			case Enums.Feedback.LoginSuccess:
				console.log("Login successful!");
				loggedInSuccessfully();
				break;
			case Enums.Feedback.LogoutSuccess:
				console.log("Logout successful!");
				break;
			case Enums.Feedback.LoginError:
			case Enums.Feedback.LogoutError:
				handleError("Login error: " + statusCode + ", " + message);
				break;
			case Enums.Feedback.InvocationWorking:
			case Enums.Feedback.InvocationSuccess:
			case Enums.Feedback.Warning:
			case Enums.Feedback.PingSuccess:
				break;
			default:
				handleError("Unhandled Error: " + statusCode + ", " + message);
			break;
		}
	});
};

/**
* Handles submission of the Login Form.
*/
var login_form_submit = function()
{
	var username = (document.getElementById("sms_username")).value,
		password = (document.getElementById("sms_password")).value;
	login(username, password);
};

/**
* Tries to login to LAS2peer and shows the Methods selection on success.
*/
function login(username, password)
{
	if(username != "" && password!="")
	{
		if(LAS2peerClient.getStatus() != "loggedIn")
		{
			LAS2peerClient.login(username, password, LAS2PEERHOST, "ShortMessageServiceFrontend"); 
			//the login function takes the LAS2Peer user credentials and the las2Peer host and the appCode
			// and connects to the Server
		}
		else
		{
			console.log("You're already logged in");
		}
	}
	else
	{
		handleError("No username or password specified!");
	}

}

/**
* called after entering correct credentials and successfully signing in and calls the getMethod function to
* populate the Combo box with available methods
*/
function loggedInSuccessfully()
{
	// clear error message
	handleError("");
	document.getElementById("sms_loginform").style.visibility="hidden";
	document.getElementById("sms_logout").style.visibility="visible";
	document.getElementById("sms_messageform").style.visibility="visible";
	document.getElementById("sms_getnew").style.visibility="visible";
	document.getElementById("sms_messageList").style.visibility="visible";
	document.getElementById("sms_messageList").innerHTML = '';
}

/**
* called upon clicking the logout button and calls the LasAjaxClient logout function
*/
function logout()
{
	// clear error message
	handleError("");
	LAS2peerClient.logout();
	document.getElementById("sms_loginform").style.visibility="visible";
	document.getElementById("sms_logout").style.visibility="hidden";
	document.getElementById("sms_messageform").style.visibility="hidden";
	document.getElementById("sms_getnew").style.visibility="hidden";
	document.getElementById("sms_messageList").style.visibility="hidden";
	document.getElementById("sms_messageList").innerHTML = '';
}

/**
* shows the result that is received from the Server
*/
function showResult(result)
{
	var text = document.createElement('p');
	if(result.type == "none")
	{
		text.innerHTML = "(void) Method";
	}
	else
	{
		text.innerHTML = result.value;
	}
	var div = document.getElementById("sms_messageList");
	div.insertBefore(text, div.firstChild);
}

/**
* shows an error message
*/
function handleError(message) {
	if (message != "")
	{
		console.log(message);
	}
	var div = document.getElementById("sms_errorText");
	div.innerHTML = message;
}

/**
* Retrieves new messages from service and displays them as a string list.
*/
function getNewMessages() {
	LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getNewMessagesAsString", [], function(status, result) 
	{
			if(status == 200|| status == 204) 
			{
				console.log("Message Invoked successfully");
				showResult(result);
			} 
			else 
			{
				handleError("Message: " + result);
			}
	}); 
}

/**
* Invokes the service method to send a short message over the network with values from the input fields.
*/
function message_form_submit() {
	var recipient = (document.getElementById("sms_recipientInput")).value,
		message = (document.getElementById("sms_messageInput")).value;
	if(recipient != "" && message != "")
	{
		// gather arguments
		invocationArguments = [{
			"type" : "String",
			"value" : recipient
		},{
			"type" : "String",
			"value" : message
		}];
		// clear input fields
//		document.getElementById("sms_recipientInput").value = "";
		document.getElementById("sms_messageInput").value = "";
		LAS2peerClient.invoke(LAS2PEERSERVICENAME, "sendMessage", invocationArguments, function(status, result)
		{
			if(status == 200|| status == 204) 
			{
				console.log("Message Invoked successfully");
				showResult(result);
			}
			else
			{
				handleError("Message: " + result);
			}
		});
	}
	else
	{
		handleError("Error! No recipient or message specified");
	}
}
