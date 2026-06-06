$(function(){

// User Register validation

	var $userRegister=$("#userRegister");

	$userRegister.validate({
		
		rules:{
			name:{
				required:true,
				lettersonly:true
			}
			,
			email: {
				required: true,
				space: true,
				email: true
			},
			mobileNumber: {
				required: true,
				space: true,
				numericOnly: true,
				minlength: 10,
				maxlength: 12

			},
			password: {
				required: true,
				space: true

			},
			confirmpassword: {
				required: true,
				space: true,
				equalTo: '#pass'

			},
			address: {
				required: true,
				all: true

			},

			city: {
				required: true,
				space: true

			},
			state: {
				required: true,


			},
			pincode: {
				required: true,
				space: true,
				numericOnly: true

			}, img: {
				required: true,
			}
			
		},
		messages:{
			name:{
				required:'name required',
				lettersonly:'invalid name'
			},
			email: {
				required: 'email name must be required',
				space: 'space not allowed',
				email: 'Invalid email'
			},
			mobileNumber: {
				required: 'mob no must be required',
				space: 'space not allowed',
				numericOnly: 'invalid mob no',
				minlength: 'min 10 digit',
				maxlength: 'max 12 digit'
			},

			password: {
				required: 'password must be required',
				space: 'space not allowed'

			},
			confirmpassword: {
				required: 'confirm password must be required',
				space: 'space not allowed',
				equalTo: 'password mismatch'

			},
			address: {
				required: 'address must be required',
				all: 'invalid'

			},

			city: {
				required: 'city must be required',
				space: 'space not allowed'

			},
			state: {
				required: 'state must be required',
				space: 'space not allowed'

			},
			pincode: {
				required: 'pincode must be required',
				space: 'space not allowed',
				numericOnly: 'invalid pincode'

			},
			img: {
				required: 'image required',
			}
		}
	})
	
	
// Orders Validation

var $orders=$("#orders");

$orders.validate({
		rules:{
			firstName:{
				required:true,
				lettersonly:true
			},
			lastName:{
				required:true,
				lettersonly:true
			}
			,
			email: {
				required: true,
				space: true,
				email: true
			},
			mobileNo: {
				required: true,
				space: true,
				numericOnly: true,
				minlength: 10,
				maxlength: 12

			},
			address: {
				required: true,
				all: true

			},

			city: {
				required: true,
				all: true
			},
			state: {
				required: true,
				all: true
			},
			pincode: {
				required: true,
				space: true,
				numericOnly: true
			},
			paymentType:{
			required: true
			}
		},
		messages:{
			firstName:{
				required:'first required',
				lettersonly:'invalid name'
			},
			lastName:{
				required:'last name required',
				lettersonly:'invalid name'
			},
			email: {
				required: 'email name must be required',
				space: 'space not allowed',
				email: 'Invalid email'
			},
			mobileNo: {
				required: 'mob no must be required',
				space: 'space not allowed',
				numericOnly: 'invalid mob no',
				minlength: 'min 10 digit',
				maxlength: 'max 12 digit'
			}
		   ,
			address: {
				required: 'address must be required',
				all: 'invalid'
			},

			city: {
				required: 'city must be required',
				all: 'invalid'
			},
			state: {
				required: 'state must be required',
				all: 'invalid'
			},
			pincode: {
				required: 'pincode must be required',
				space: 'space not allowed',
				numericOnly: 'invalid pincode'
			},
			paymentType:{
			required: 'select payment type'
			}
		}	
})

// Reset Password Validation

var $resetPassword=$("#resetPassword");

$resetPassword.validate({
		
		rules:{
			password: {
				required: true,
				space: true

			},
			confirmPassword: {
				required: true,
				space: true,
				equalTo: '#pass'

			}
		},
		messages:{
		   password: {
				required: 'password must be required',
				space: 'space not allowed'

			},
			confirmPassword: {
				required: 'confirm password must be required',
				space: 'space not allowed',
				equalTo: 'password mismatch'

			}
		}	
})

if ($orders.length) {
	$orders.on("submit", function(e) {
		var form = this;
		var paymentType = (form.paymentType.value || "").toUpperCase();
		var paymentId = $("#razorpayPaymentId").val();

		if (paymentType !== "ONLINE" || paymentId) {
			return true;
		}

		e.preventDefault();
		if (!$orders.valid()) {
			return false;
		}

	if (form.dataset.onlinePaymentEnabled !== "true") {
		renderPaymentMessage("Online payment is not configured yet. Please choose Cash On Delivery.", false);
		return false;
	}

	togglePlaceOrderButton(true);
	renderPaymentMessage("Preparing secure payment gateway...", true);

	ensureRazorpayLoaded(form.dataset.checkoutScriptUrl).then(function() {
		renderPaymentMessage("Creating secure payment order...", true);
		return fetch(form.dataset.createPaymentUrl, {
			method: "POST",
			body: new FormData(form)
		});
	}).then(async function(response) {
		var payload = await response.json().catch(function() {
			return {};
		});

		if (!response.ok) {
			throw new Error(payload.message || "Unable to start online payment.");
		}

		return payload;
	}).then(function(paymentOrder) {
		openRazorpayCheckout(form, paymentOrder);
	}).catch(function(error) {
		togglePlaceOrderButton(false);
		renderPaymentMessage(error.message, false);
		});

		return false;
	});
}
})



jQuery.validator.addMethod('lettersonly', function(value, element) {
		return /^[^-\s][a-zA-Z_\s-]+$/.test(value);
	});
	
		jQuery.validator.addMethod('space', function(value, element) {
		return /^[^-\s]+$/.test(value);
	});

	jQuery.validator.addMethod('all', function(value, element) {
		return /^[^-\s][a-zA-Z0-9_,.\s-]+$/.test(value);
	});


	jQuery.validator.addMethod('numericOnly', function(value, element) {
		return /^[0-9]+$/.test(value);
	});

function openRazorpayCheckout(form, paymentOrder) {
	var razorpay = new Razorpay({
		key: paymentOrder.key,
		amount: paymentOrder.amount,
		currency: paymentOrder.currency,
		name: paymentOrder.name,
		description: paymentOrder.description,
		image: paymentOrder.image,
		order_id: paymentOrder.orderId,
		prefill: {
			name: paymentOrder.prefillName,
			email: paymentOrder.prefillEmail,
			contact: paymentOrder.prefillContact
		},
		handler: function(response) {
			$("#razorpayOrderId").val(response.razorpay_order_id);
			$("#razorpayPaymentId").val(response.razorpay_payment_id);
			$("#razorpaySignature").val(response.razorpay_signature);
			renderPaymentMessage("Payment verified. Finalizing your order...", true);
			form.submit();
		},
		modal: {
			ondismiss: function() {
				togglePlaceOrderButton(false);
				renderPaymentMessage("Payment window closed. Your order has not been placed yet.", false);
			}
		},
		theme: {
			color: "#2b7256"
		}
	});

	razorpay.on("payment.failed", function(response) {
		togglePlaceOrderButton(false);
		var details = response && response.error ? response.error : {};
		var reason = details.description || details.reason || "Test payment failed before verification.";
		renderPaymentMessage(reason, false);
	});

	razorpay.open();
}

function ensureRazorpayLoaded(scriptUrl) {
	if (typeof window.Razorpay !== "undefined") {
		return Promise.resolve();
	}

	var existingScript = document.getElementById("razorpay-checkout-script");
	if (existingScript) {
		return waitForRazorpay(existingScript);
	}

	var script = document.createElement("script");
	script.id = "razorpay-checkout-script";
	script.src = scriptUrl || "https://checkout.razorpay.com/v1/checkout.js";
	document.body.appendChild(script);
	return waitForRazorpay(script);
}

function waitForRazorpay(scriptElement) {
	return new Promise(function(resolve, reject) {
		if (typeof window.Razorpay !== "undefined") {
			resolve();
			return;
		}

		var timeoutId = window.setTimeout(function() {
			reject(new Error("Razorpay checkout could not be loaded. Please allow checkout.razorpay.com and try again."));
		}, 10000);

		scriptElement.addEventListener("load", function handleLoad() {
			window.clearTimeout(timeoutId);
			if (typeof window.Razorpay === "undefined") {
				reject(new Error("Razorpay checkout loaded incorrectly. Please refresh and try again."));
				return;
			}
			resolve();
		}, { once: true });

		scriptElement.addEventListener("error", function handleError() {
			window.clearTimeout(timeoutId);
			reject(new Error("Razorpay checkout could not be loaded. Please check your internet connection and try again."));
		}, { once: true });
	});
}

function renderPaymentMessage(message, isInfo) {
	var $box = $("#payment-message");
	if (!$box.length) {
		return;
	}

	$box.removeClass("d-none alert-danger alert-info")
		.addClass("alert " + (isInfo ? "alert-info" : "alert-danger"))
		.text(message);
}

function togglePlaceOrderButton(isBusy) {
	var $button = $("#placeOrderBtn");
	if (!$button.length) {
		return;
	}

	if (!$button.data("defaultText")) {
		$button.data("defaultText", $button.text());
	}

	$button.prop("disabled", isBusy);
	$button.text(isBusy ? "Processing..." : $button.data("defaultText"));
}
