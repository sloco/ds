var LoginFocus = (function() {
	return {
		setFocus: function() {
			var email = document.querySelector("#email"),
					password = document.querySelector("#password");

			if (email.value) {
				password.focus();
			} else {
				email.focus();
			}
		}
	}
})();

LoginFocus.setFocus()