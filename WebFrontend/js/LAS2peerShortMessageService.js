		//Private Porperties
		var LAS2PEERHOST = "http://localhost:9080/";
		var LAS2PEERSERVICENAME = "i5.las2peer.services.shortMessageService.ShortMessageService";
		var LAS2peerClient; 
		
		/**
		* Gets called when the window loads 
		*/
		window.onload = function() 
		{
			LAS2peerClient = new LasAjaxClient(LAS2PEERSERVICENAME, function(statusCode, message) 
					{
						switch(statusCode) 
							{
						//this function is called whenever the login/ connection/invocation status of the LAS2Peer client changes or if an error occurs
								case Enums.Feedback.LoginSuccess:
									console.log("Login successful!");
									loggedInSuccessfully();
									break;
								case Enums.Feedback.LogoutSuccess:
									console.log("Logout successful!");
									break;
								case Enums.Feedback.LoginError:
								case Enums.Feedback.LogoutError:
									console.log("Login error: " + statusCode + ", " + message);
									break;
								case Enums.Feedback.InvocationWorking:
								case Enums.Feedback.InvocationSuccess:
								case Enums.Feedback.Warning:
								case Enums.Feedback.PingSuccess:
									break;
								default:
									console.log("Unhandled Error: " + statusCode + ", " + message);
								break;
							}
					});
		};
		
		
		/**
		* Handles submission of the Login Form. Tries to login to LAS2peer and shows the Methods selection on success.
		*/
		var login_form_submit = function()
		{
			var username = (document.getElementById("cs_username")).value,
				password = (document.getElementById("cs_password")).value;
			if(username != "" && password!="")
			{
				if(LAS2peerClient.getStatus() != "loggedIn")
				{
					LAS2peerClient.login(username, password, LAS2PEERHOST, "ShortMessageServiceFrontend"); 
					//the login function takes the LAS2Peer user credentials and the las2Peer host and the appCode
					// and connects to the Server
				}
			}
		};
		
		/**
		* called after entering correct credentials and successfully signing in and calls the getMethod function to
		* populate the Combo box with available methods
		*/
		function loggedInSuccessfully()
		{
			console.log("Login success!");
			document.getElementById("cs_loginform").style.visibility="hidden";
			document.getElementById("cs_logout").style.visibility="visible";
			document.getElementById("selectMethod").style.visibility="visible";
			LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getMethods", "", function(status, result) 
			{
				if(status == 200 || status == 204) 
				{
					populateComboBox(result.value);
				} 
				else 
				{
					console.log("Error! Message: " + result);
				}
			});
			AddTextBoxes([]);
		}
		
		/**
		* method to fill in all methods in the Combo box
		*/
		function populateComboBox(methodNamesArray)
		{
			var select = document.getElementById("selectMethod");
			select.innerHTML="";
			for(var i = 0; i < methodNamesArray.length; i++) 
			{
			    var opt = methodNamesArray[i];
			    var el = document.createElement("option");
			    el.textContent = opt;
			    el.value = opt;
			    select.appendChild(el);
			}
		}
		
		/**
		* called upon clicking the logout button and calls the LasAjaxClient logout function
		*/
		function logout()
		{
			LAS2peerClient.logout();
			document.getElementById("cs_loginform").style.visibility="visible";
			document.getElementById("cs_logout").style.visibility="hidden";
			document.getElementById("selectMethod").style.visibility="hidden";
			var div = document.getElementById("textBoxesDiv");
			div.innerHTML = '';
		}
		
		/**
		* handles the value changes in the Combo box 
		*/
		function ComboBoxChange()
		{
		    var e = document.getElementById("selectMethod");//get the combobox
		    var selMethod = e.options[e.selectedIndex].value;//get selected value
			invocationArguments = [ {
				"type" : "int",
				"value" : e.selectedIndex
			}];
		    LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getParameterTypesOfMethod", invocationArguments, function(status, result) 
		    {
				if(status == 200 || status == 204) 
				{	
					AddTextBoxes(result.value);
				} 
				else 
				{
					console.log("Error! Message: " + result);
				}
			});                 
		}
		
		/**
		* after pressing the invoke button the arguments are passed in a JSON string to the LasAjaxClient invoke method which calls
		* the given method on the Server and returns the result
		*/
		function InvokeButtonClicked()
		{
			var textBoxes=document.getElementsByClassName("argumentTextBoxes");
			var invocationArguments = [];
			for(var i=0;i<textBoxes.length;i++)
				{
					var elem =document.getElementById("argumentTextBoxes"+i);
					var val =elem.value;
					var parameterI={};
					parameterI.type = elem.placeholder;
					parameterI.value = val;
					invocationArguments.push(parameterI);
				}
			
			var e = document.getElementById("selectMethod");//get the combobox
			var selMethod = e.options[e.selectedIndex].value;//get selected value
			LAS2peerClient.invoke(LAS2PEERSERVICENAME, selMethod, invocationArguments, function(status, result) 
			{
					if(status == 200|| status == 204) 
					{
						console.log("Message Invoked successfully");
						ShowResult(result);
					} 
					else 
					{
						console.log("Error! Message: " + result);
						var resultCorrectFormat={};
						resultCorrectFormat.value = result;
						ShowResult(resultCorrectFormat);
					}
				}); 
		}
		
		/**
		* Shows the result that is received from the Server
		*/
		function ShowResult(result)
		{
			var text = document.createElement('p');
			if(result.type=="none")
			{
				text.innerHTML = "(void) Method";
			}
			else
			{
				text.innerHTML = result.value;
			}

			var div = document.getElementById("textBoxesDiv");
			div.appendChild(text);
		}
		
		/**
		* AddTextBoxes handles the amount of Text boxes to be added according to which method was 
		* chosen from the Combo box
		*/
		function AddTextBoxes(parameters)
		{
			var div = document.getElementById("textBoxesDiv");
			div.innerHTML = '';
			for(var i = 0; i < parameters.length; i++) 
			{
				var input = document.createElement('input'); 
				input.type = "text"; 
				input.id="argumentTextBoxes"+i;
				input.className="argumentTextBoxes";
				input.placeholder=parameters[i];
				div.appendChild(input);
			}
			
			var button = document.createElement('input');
			button.type = "button"; 
			button.value="invoke Method";
			button.onclick=InvokeButtonClicked;
			div.appendChild(button);
		}
		