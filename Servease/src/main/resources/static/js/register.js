const form = document.getElementById("registerForm");

const nameInput = document.getElementById("name");
const emailInput = document.getElementById("email");
const phoneInput = document.getElementById("phone");
const passwordInput = document.getElementById("password");
const confirmPasswordInput =
    document.getElementById("confirmPassword");
const roleInput = document.getElementById("role");

const message = document.getElementById("message");
const registerBtn = document.getElementById("registerBtn");


// Only allow numbers in phone field
phoneInput.addEventListener("input", function () {

    this.value = this.value.replace(/\D/g, "");

});


// Submit registration
form.addEventListener("submit", async function (event) {

    event.preventDefault();


    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const phone = phoneInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;
    const role = roleInput.value;


    // Name validation
    if (name.length < 2) {

        showMessage(
            "Please enter your full name.",
            "error"
        );

        return;
    }


    // Phone validation
    if (phone.length !== 10) {

        showMessage(
            "Please enter a valid 10-digit phone number.",
            "error"
        );

        return;
    }


    // Password validation
    if (password.length < 6) {

        showMessage(
            "Password must be at least 6 characters long.",
            "error"
        );

        return;
    }


    // Confirm password
    if (password !== confirmPassword) {

        showMessage(
            "Passwords do not match.",
            "error"
        );

        return;
    }


    // Role validation
    if (!role) {

        showMessage(
            "Please select an account type.",
            "error"
        );

        return;
    }


    registerBtn.disabled = true;
    registerBtn.textContent = "Creating Account...";


    try {

        const response = await fetch(
            "/api/auth/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({

                    name: name,

                    email: email,

                    phone: phone,

                    password: password,

                    role: role

                })
            }
        );


        const data = await response.json();


        if (response.ok && data.success) {

            showMessage(
                "Account created successfully. Redirecting to login...",
                "success"
            );


            // Redirect to login
            setTimeout(() => {

                window.location.href = "login.html";

            }, 1200);


        } else {

            showMessage(
                data.message ||
                "Registration failed. Please try again.",
                "error"
            );

        }

    } catch (error) {

        console.error(
            "Registration error:",
            error
        );


        showMessage(
            "Something went wrong. Please try again later.",
            "error"
        );

    } finally {

        registerBtn.disabled = false;

        registerBtn.textContent = "Create Account";

    }

});


function showMessage(text, type) {

    message.textContent = text;

    message.className =
        "message " + type;

}